package com.sheridan.gcr.client;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.recoil.IRecoilCameraHandler;
import com.sheridan.gcr.client.recoil.RecoilCameraHandler;
import com.sheridan.gcr.client.recoil.RecoilHandler;
import com.sheridan.gcr.client.render.HardCodeAnimationHandler;
import com.sheridan.gcr.client.render.IGlobalAnimationHandler;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * 客户端武器线程：一个长期存活的小循环，按绝对截止时刻驱动三件事——开火时间轴、
 * 100Hz 后座力推进、100Hz 动画推进。
 *
 * <h2>为什么不再用固定周期轮询</h2>
 * 旧实现是 {@code scheduleAtFixedRate(..., 5ms)} 每 5ms 看一眼「冷却计数到 0 没有」，
 * 冷却值是整数个 5ms 步进（{@code 60000 / rpm / 5}）。于是每一发的间隔被量化到 5ms 的整数倍：
 * <pre>
 *   理论 RPM   理论间隔     旧实现只能取      持续射击实际 RPM
 *   600       100.000ms    100ms            600
 *   700        85.714ms     85ms             705.88
 *   900        66.667ms     65ms / 70ms      923.08 / 857.14
 *   1100       54.545ms     50ms / 55ms      1200 / 1090.91
 * </pre>
 * 更糟的是 {@code scheduleAtFixedRate} 只保证「不早于」，任务实际执行时刻带着毫秒级抖动，
 * 每发的误差还会被累计进「还剩几发冷却」里。
 *
 * <h2>现在的做法</h2>
 * <ul>
 *   <li>间隔从 RPM 直接换算成纳秒（{@link com.sheridan.gcr.modularSys.fire.IFireMode#rpmToIntervalNanos(int)}），
 *       700 RPM 就是 85 714 285ns，不再经过整数步进。</li>
 *   <li>下一发的截止时刻 = <b>上一发的计划时刻</b> + 间隔，而不是「现在 + 间隔」。
 *       单次唤醒抖动因此不会累积，长期平均射速严格等于理论射速；只有落后超过一整个间隔
 *       （卡顿 / GC）时才丢掉补不回来的那几发、把节拍重新对齐到现在，不会停顿之后连着喷一串。</li>
 *   <li>等待分两段：先 {@code parkNanos} 睡到临近截止时刻，最后 {@link #spinNanos} 自旋收尾。
 *       Windows 上 park 的唤醒精度只有毫秒级，自旋窗口按实测偏差自适应，既不空转烧 CPU，
 *       也不会把唤醒误差传导到开火时刻上。</li>
 *   <li>扳机按下 → {@link #wake()} 直接叫醒线程，第一发不再需要等下一个轮询周期。</li>
 * </ul>
 *
 * <h2>锁</h2>
 * 客户端主线程在整个 tick（{@code ClientTickEvent.Pre ~ Post}）里持有 {@link Client#LOCK}，
 * 开火要改的物品数据 / 卡壳缓存 / 后座力都在它的保护范围内。本线程遵守两条纪律：
 * <ol>
 *   <li><b>只在真开火时拿锁</b>：平时只用 {@link WeaponStatus#isFireBlocked()} 等 volatile 旗标做
 *       无锁预检（见 {@link #fireWanted()}），连枪都不碰；确实要打这一发时才进
 *       {@link Client#handleClientShoot} 抢锁。</li>
 *   <li><b>持锁期间不反向等主线程</b>：拿锁后绝不 {@code Minecraft.execute} 之后 join、
 *       也不等待主线程持有的其它锁，因此不存在「主线程等武器线程、武器线程等主线程」的环。
 *       主线程侧的 tick 锁也保证异常退出时一定会归还（见 {@code ClientEvents}）。</li>
 * </ol>
 */
@OnlyIn(Dist.CLIENT)
public final class ClientWeaponLooper implements Runnable {

    // ------------------------------------------------------------------
    // 时间轴参数
    // ------------------------------------------------------------------

    /** 后座力 / 动画推进周期，与旧实现的两个 10ms 周期任务一致（100Hz）。 */
    private static final long UPDATE_PERIOD_NANOS = 10_000_000L;
    private static final float UPDATE_DELTA_SECONDS = 0.01f;

    /**
     * 开火前的自旋收尾窗口。
     *
     * <p>{@code parkNanos} 的唤醒精度由操作系统决定（Windows 上是毫秒级），所以最后一小段
     * 必须自旋才能把单发时刻压到亚毫秒；窗口按实测唤醒偏差自适应，避免长期空转。</p>
     */
    private static final long SPIN_START_NANOS = 500_000L;
    private static final long SPIN_MIN_NANOS = 200_000L;
    private static final long SPIN_MAX_NANOS = 2_000_000L;
    private static final long SPIN_GROW_NANOS = 250_000L;
    private static final long SPIN_SHRINK_NANOS = 50_000L;

    /** 这一发被状态拒绝（拔枪动画 / 换弹 / 冲刺 / 空仓 / 卡壳）后的重试间隔。 */
    private static final long RETRY_WAIT_NANOS = 5_000_000L;

    /** 保证只会有一个武器循环：重复提交时后来的直接退出，免得两套时间轴抢着开火。 */
    private static final AtomicBoolean STARTED = new AtomicBoolean();

    /** 当前武器线程，供 {@link #wake()} 唤醒。 */
    private static volatile Thread worker;

    /** 下一发的计划开火时刻（{@link System#nanoTime()} 时间轴）；{@code < 0} 表示不在射击时间轴里。 */
    private long nextFireNanos = -1L;
    /** 本次等待是否需要精确到点：正常射击节奏需要，被拒绝后的重试不需要（自旋不划算）。 */
    private boolean firePrecise;
    private long nextRecoilNanos;
    private long nextAnimationNanos;
    private long spinNanos = SPIN_START_NANOS;

    /**
     * 叫醒武器线程。
     *
     * <p>扳机按下、以及枪从「不可开火」恢复成「可开火」（换弹结束 / 拔枪完成 / 冲刺停下）时调用，
     * 避免第一发白等一个周期。线程没起来时是空操作。</p>
     */
    public static void wake() {
        LockSupport.unpark(worker);
    }

    @Override
    public void run() {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        Thread self = Thread.currentThread();
        self.setName("GCR-Weapon-Thread");
        try {
            // 开火要落在纳秒级的时间点上，被系统调度甩开就没意义了。这个线程绝大多数时间在 park，
            // 不会和主线程抢 CPU。
            self.setPriority(Math.min(Thread.MAX_PRIORITY, Thread.NORM_PRIORITY + 1));
        } catch (Throwable ignored) {
        }
        worker = self;

        long now = System.nanoTime();
        nextRecoilNanos = now;
        nextAnimationNanos = now;

        while (true) {
            try {
                step();
            } catch (Throwable t) {
                GCR.LOGGER.error("[GCR] weapon thread step failed", t);
                LockSupport.parkNanos(RETRY_WAIT_NANOS);
            }
        }
    }

    // ------------------------------------------------------------------
    // 主循环
    // ------------------------------------------------------------------

    private void step() {
        long now = System.nanoTime();

        // 1) 开火：只有「按住扳机 + 枪可用 + 到点」才会去碰共享状态
        updateFire(now);

        // 2) 100Hz 推进后座力与动画。刻意和开火留在同一个线程上串行执行：
        //    RecoilUpdater 的 onShoot / update 并不是线程安全的，拆到两个线程会写坏内部队列。
        now = System.nanoTime();
        if (now - nextRecoilNanos >= 0L) {
            updateRecoil();
            nextRecoilNanos = advanceDeadline(nextRecoilNanos, UPDATE_PERIOD_NANOS, now);
        }
        if (now - nextAnimationNanos >= 0L) {
            updateAnimation();
            nextAnimationNanos = advanceDeadline(nextAnimationNanos, UPDATE_PERIOD_NANOS, now);
        }

        // 3) 睡到下一个待办时刻
        sleepUntil(nextDeadline(now));
    }

    /**
     * 推进开火时间轴。
     *
     * <p>不满足 {@link #fireWanted()} 时把时间轴清空，这样重新按下扳机的那一发是立刻打出去的，
     * 而不是接着上一条时间轴慢慢等。</p>
     */
    private void updateFire(long now) {
        if (!fireWanted()) {
            nextFireNanos = -1L;
            firePrecise = false;
            return;
        }

        if (nextFireNanos < 0L) {
            nextFireNanos = now;
            firePrecise = true;
        }

        long remaining = nextFireNanos - now;

        if (remaining > (firePrecise ? spinNanos : 0L)) {
            return;
        }

        if (firePrecise) {
            spinUntil(nextFireNanos);
        }

        fireOnce();
    }

    /**
     * 无锁预检：只读几个 volatile / atomic 旗标。
     *
     * <p>为 {@code false} 时才值得去抢 {@link Client#LOCK}。这里的判断允许过期，判断错了最多多抢一次锁；
     * 真正的判定永远在锁里做。</p>
     */
    private boolean fireWanted() {
        return !Client.CANCEL_WEAPON_LOOPER.get()
                && Client.LEFT_BUTTON_PRESSED.get()
                && !Client.WEAPON_STATUS.isFireBlocked();
    }

    /**
     * 打这一发，并排好下一发的截止时刻。
     *
     * <p>误差不累积的关键在这里：下一发以 {@code planned}（本发的计划时刻）为基准，
     * 而不是以 {@code now}（本发的实际时刻）为基准。哪怕这一次晚了 1ms，下一发就会相应地早 1ms，
     * 时间轴整体不会漂移。</p>
     */
    private void fireOnce() {
        long planned = nextFireNanos;
        long interval = attemptShoot();
        long shootEnd = System.nanoTime();
        if (interval <= 0L) {
            // 本次没有真正开火。
            // 从任务结束时刻开始计算重试等待，避免 attemptShoot 本身的耗时
            // 让重试时间缩短。
            nextFireNanos = shootEnd + RETRY_WAIT_NANOS;
            firePrecise = false;
            return;
        }
        // 正常情况下：
        //
        //   下一发 = 上一发计划时刻 + 射击间隔
        //
        // 因此 attemptShoot() 的执行耗时不会累计到射击周期中。
        long next = planned + interval;

        // 如果 attemptShoot() 本身执行太久，以至于已经超过了下一发
        // 的计划时刻，则丢掉已经无法按时补回的这一发，从任务结束时刻
        // 重新建立完整的射击周期。
        if (next <= shootEnd) {
            next = shootEnd + interval;
        }

        nextFireNanos = next;
        firePrecise = true;
    }


    /**
     * 真正开火，全部共享状态都在 {@link Client#handleClientShoot} 的锁里读写。
     *
     * @return 到下一发的精确间隔（纳秒）；{@code <= 0} 表示这一发没打出去
     */
    private long attemptShoot() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0L;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) {
            return 0L;
        }
        IGun gun = gunItem.getGun();
        if (gun == null) {
            return 0L;
        }
        return Client.handleClientShoot(stack, gun, player);
    }

    // ------------------------------------------------------------------
    // 100Hz 推进
    // ------------------------------------------------------------------

    private void updateRecoil() {
        try {
            RecoilHandler.INSTANCE.update(UPDATE_DELTA_SECONDS);
            IRecoilCameraHandler cameraHandler = RecoilCameraHandler.getInstance();
            if (cameraHandler != null) {
                cameraHandler.update(UPDATE_DELTA_SECONDS);
            }
        } catch (Throwable t) {
            GCR.LOGGER.error("[GCR] recoil update failed", t);
        }
    }

    private void updateAnimation() {
        try {
            IGlobalAnimationHandler handler = HardCodeAnimationHandler.getInstance();
            if (handler != null) {
                handler.update(UPDATE_DELTA_SECONDS);
            }
        } catch (Throwable t) {
            GCR.LOGGER.error("[GCR] animation update failed", t);
        }
    }

    // ------------------------------------------------------------------
    // 时间工具
    // ------------------------------------------------------------------

    /**
     * 周期任务的截止时刻推进：下一次 = 上一次计划 + 周期。
     *
     * <p>和开火一样以计划时刻为基准，长时间运行也不会漂；落后太多时直接对齐到现在，
     * 免得一觉醒来要连补几十帧。</p>
     */
    private static long advanceDeadline(long plannedNanos, long periodNanos, long now) {
        long next = plannedNanos + periodNanos;
        return next <= now ? now + periodNanos : next;
    }

    /** 下一个必须醒来的绝对时刻（可能已经过去，此时立刻返回上一层干活）。 */
    private long nextDeadline(long now) {
        long deadline = Math.min(nextRecoilNanos, nextAnimationNanos);
        if (nextFireNanos >= 0L) {
            long fireWake = firePrecise ? nextFireNanos - spinNanos : nextFireNanos;
            if (fireWake < deadline) {
                deadline = fireWake;
            }
        }
        return Math.max(deadline, now);
    }

    /** 自旋等到绝对时刻。窗口最多 {@link #SPIN_MAX_NANOS}，不会长时间占着 CPU。 */
    private static void spinUntil(long deadlineNanos) {
        while (System.nanoTime() - deadlineNanos < 0L) {
            Thread.onSpinWait();
        }
    }

    private void sleepUntil(long wakeAtNanos) {
        long wait = wakeAtNanos - System.nanoTime();
        if (wait <= 0L) {
            return;
        }
        LockSupport.parkNanos(wait);
        // 用实测的唤醒偏差调整自旋窗口：睡得比预期晚就加宽窗口（宁可多自旋一点也要准），
        // 被提前叫醒（wake / 其它中断）则慢慢收窄，避免一直空转。
        long overshoot = System.nanoTime() - wakeAtNanos;
        if (overshoot > 0L) {
            spinNanos = Math.min(SPIN_MAX_NANOS, Math.max(spinNanos, overshoot + SPIN_GROW_NANOS));
        } else if (overshoot < -SPIN_SHRINK_NANOS) {
            spinNanos = Math.max(SPIN_MIN_NANOS, spinNanos - SPIN_SHRINK_NANOS);
        }
    }
}
