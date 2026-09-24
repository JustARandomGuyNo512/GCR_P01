package com.sheridan.gcr.client.stuck;

import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端卡壳（故障）缓存——把「谁卡了壳」这件事从物品 NBT 里搬到客户端内存里。
 *
 * <h2>为什么需要它</h2>
 * 卡壳现在由客户端在开火那一刻判定（这样才能同步命中 {@code shoot_stuck} 动画，并且不会出现
 * “客户端连发、服务端其实卡壳没打出去”的幻影射击）。判定结果会随开火包一起发给服务端，
 * 但客户端本地必须先把这一发记为“卡壳”，否则：
 * <ul>
 *   <li>判定发生在 {@code useAmmo} → {@code SHOOT} 事件派发之间，动画控制器
 *       {@code view.stuck(states)} 需要立刻读到 true；</li>
 *   <li>多人游戏里物品数据由服务端同步，快速切枪 / 拔枪会触发一次容器同步，把客户端本地
 *       写进 CUSTOM_DATA 的 stuck 位直接覆盖回服务端的旧值。如果这一发开火包还没被服务端
 *       处理，本地卡壳就会凭空消失（或者在别的时序下变成服务端已卡壳、客户端不知道）。</li>
 * </ul>
 *
 * <h2>记录的生命周期</h2>
 * <ul>
 *   <li><b>PREDICTED</b>：客户端本地掷出的卡壳，尚未被服务端回执确认。带超时
 *       （{@link #PREDICTION_TIMEOUT_MS}）：服务端始终会对每一发处理过的开火回执，收不到回执
 *       说明这一发根本没被处理（切枪导致身份不匹配、连接中断等），必须自愈，
 *       否则就是玩家口中的“虚假卡壳”。</li>
 *   <li><b>CONFIRMED</b>：服务端回执/主动同步确认的卡壳，不会超时。只能被服务端解除
 *       （排障、清障动作完成后的 {@code GunStuckSyncPacket}）。</li>
 * </ul>
 * 记录以 {@link IGun#getIdentityID(ItemStack)} 为键——它由服务端生成、随物品数据持久化，
 * 切枪、换维度、退出重进都不会变，因此缓存不会被这些行为打断。玩家退出连接时整体清空：
 * 真实卡壳被服务端保存在物品数据里，重进时随背包同步回来，缓存只是“预测桥”，
 * 跨连接保留预测反而会制造虚假卡壳。
 *
 * <h2>如何不影响既有状态读取</h2>
 * {@link #tick(LocalPlayer)} / {@link #projectToNbt(ItemStack, IGun)} 会把缓存里的卡壳
 * <b>投影回物品的 states</b>。动画控制器读的 {@code ReadOnlyTag}、{@code Gun#isStuck}、
 * 任务系统里的 {@code isStuck} 判定读的都是这份 states，所以既有读取路径一行都不用改，
 * 缓存只是让它们在被容器同步覆盖后重新变成正确值。
 */
public final class ClientGunStuckCache {

    /**
     * 本地预测多久收不到服务端回执就判定为“这一发根本没发生”。
     * 服务端在 {@code GunFirePacket} 的处理里对每一发都会回 {@code GunFireAckPacket}
     * （打出、拒绝都回），因此正常网络下几百毫秒内必然到达；这里给足抖动余量。
     */
    public static final long PREDICTION_TIMEOUT_MS = 3000L;

    /** 兜底上限：防止长期游戏里 tombstone 记录无限堆积。 */
    private static final int MAX_ENTRIES = 256;

    private static final ClientGunStuckCache INSTANCE = new ClientGunStuckCache();

    public static ClientGunStuckCache get() {
        return INSTANCE;
    }

    /**
     * 一条卡壳记录。
     *
     * <p>字段全部 {@code volatile}：写入来自客户端主线程（回执/同步/tick），读取却可能发生在
     * 500Hz 的开火线程上（{@code ClientWeaponLooper}），必须保证可见性。</p>
     */
    private static final class Entry {
        /** 当前信念：这把枪是不是卡壳。 */
        volatile boolean stuck;
        /** 是否已被服务端确认（确认后不再超时）。 */
        volatile boolean confirmed;
        /** 这条记录锚定的最后一发开火 id，用于丢弃迟到的旧回执。 */
        volatile int shootId;
        /** 最后一次更新的毫秒时间戳。 */
        volatile long stampMs;
        /** 我们本地是否往物品 NBT 写过 stuck=true（解除时要负责擦掉）。 */
        volatile boolean nbtWritten;
        /** 记录已作废，但物品 NBT 里可能还留着本地写进去的 stuck=true，等待投影擦除。 */
        volatile boolean needsNbtClear;
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    private ClientGunStuckCache() {
    }

    // =====================================================================
    // 读
    // =====================================================================

    /** 纯缓存查询：这条 identityID 是否被客户端认为是卡壳的。 */
    public boolean isStuck(String gunId) {
        if (gunId == null || IGun.NONE.equals(gunId)) {
            return false;
        }
        Entry entry = entries.get(gunId);
        return entry != null && entry.stuck;
    }

    /**
     * 客户端统一入口：物品 NBT 里写着卡壳，<b>或</b> 缓存里记着卡壳。
     *
     * <p>先读 NBT 保证服务端同步来的权威状态永远优先；再读缓存覆盖“本地预测还没写回 NBT /
     * NBT 刚被容器同步冲掉”的窗口。</p>
     */
    public boolean isStuck(ItemStack stack, IGun gun) {
        if (gun.isStuck(stack)) {
            return true;
        }
        return isStuck(gun.getIdentityID(stack));
    }

    // =====================================================================
    // 写：客户端本地判定
    // =====================================================================

    /**
     * 客户端在开火那一刻判定出卡壳。调用方必须已经确认 {@code gun.isStuck(stack)} 为 true
     * （即 {@code useAmmo} 已经把这一位写进物品 states），否则不该登记预测。
     *
     * @param gunId   枪械 identityID
     * @param shootId 这一发的开火 id，用于和服务端回执配对
     */
    public void onLocalJam(String gunId, int shootId) {
        if (gunId == null || IGun.NONE.equals(gunId)) {
            return;
        }
        long now = System.currentTimeMillis();
        entries.compute(gunId, (key, old) -> {
            Entry entry = old == null ? new Entry() : old;
            if (entry.stuck && entry.confirmed) {
                // 服务端已经确认卡壳，本地不可能真的打出这一发；保留更权威的记录
                entry.shootId = Math.max(entry.shootId, shootId);
                return entry;
            }
            entry.stuck = true;
            entry.confirmed = false;
            entry.shootId = shootId;
            entry.stampMs = now;
            entry.nbtWritten = true;
            entry.needsNbtClear = false;
            return entry;
        });
        trimIfNeeded();
    }

    /**
     * 清障动作已经把 {@code RemoveStuckPacket} 发给服务端。
     *
     * <p>只作废<b>本地预测</b>：预测本来就不该长期存在（服务端从未确认过它），玩家做完动作
     * 就应该能继续射击。已确认的卡壳必须等服务端的准信（服务端收到清障包后一定会回
     * {@code GunStuckSyncPacket}），否则会出现“客户端以为清了、服务端还卡着”的抖动。</p>
     */
    public void onLocalClearRequested(String gunId) {
        if (gunId == null || IGun.NONE.equals(gunId)) {
            return;
        }
        entries.computeIfPresent(gunId, (key, entry) -> {
            if (entry.stuck && !entry.confirmed) {
                markCleared(entry);
            }
            return entry;
        });
    }

    // =====================================================================
    // 写：服务端消息
    // =====================================================================

    /**
     * 服务端对某一发的回执。
     *
     * @param shootId    客户端的开火 id
     * @param serverStuck 服务端处理完这一发之后的卡壳状态
     * @param fired      服务端是否真的打出了这一发（false 表示因为已经卡壳 / 没弹被拒）
     */
    public void onServerAck(String gunId, int shootId, boolean serverStuck, boolean fired) {
        if (gunId == null || IGun.NONE.equals(gunId)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (serverStuck) {
            entries.compute(gunId, (key, old) -> {
                Entry entry = old == null ? new Entry() : old;
                if (entry.stuck && entry.confirmed && entry.shootId > shootId) {
                    // 已经有更新的权威记录，别被迟到的旧回执推翻
                    return entry;
                }
                entry.stuck = true;
                entry.confirmed = true;
                entry.shootId = shootId;
                entry.stampMs = now;
                entry.needsNbtClear = false;
                return entry;
            });
        } else {
            entries.computeIfPresent(gunId, (key, entry) -> {
                if (entry.shootId > shootId) {
                    // 迟到的旧回执：这一发之后客户端/服务端已经推进到更新的状态
                    return entry;
                }
                markCleared(entry);
                return entry;
            });
        }
        // fired=false 表示服务端没有真的打出这一发（已经卡壳 / 膛内没弹）。它对卡壳信念的
        // 影响已经由上面的 serverStuck 分支覆盖：真正要修正的是“服务端是不是卡壳”，
        // 而 ammoLeft/fired 只是留给客户端做弹药预测校对的附加信息。
        trimIfNeeded();
    }

    /**
     * 服务端主动同步卡壳状态（清障包处理完成、排障等非开火路径）。
     * 这是「服务端排障后客户端必须同步解除故障」的唯一权威通道。
     */
    public void onServerSync(String gunId, boolean stuck) {
        if (gunId == null || IGun.NONE.equals(gunId)) {
            return;
        }
        if (stuck) {
            long now = System.currentTimeMillis();
            entries.compute(gunId, (key, old) -> {
                Entry entry = old == null ? new Entry() : old;
                entry.stuck = true;
                entry.confirmed = true;
                entry.stampMs = now;
                entry.needsNbtClear = false;
                return entry;
            });
        } else {
            entries.computeIfPresent(gunId, (key, entry) -> {
                markCleared(entry);
                return entry;
            });
        }
        trimIfNeeded();
    }

    // =====================================================================
    // 投影 / 生命周期
    // =====================================================================

    /**
     * 把缓存里的卡壳状态写回物品 NBT，让所有既有读取路径立刻看到正确值。
     *
     * <p>只做两件事：缓存说卡壳但 NBT 没写 → 补上；记录已解除且本地写过 → 擦掉。
     * 缓存没有记录时什么都不做——此时物品 NBT 里可能是服务端同步来的权威卡壳，
     * 不能因为“缓存不知道”就清掉。</p>
     */
    public void projectToNbt(ItemStack stack, IGun gun) {
        Entry entry = entryOf(stack, gun);
        if (entry == null) {
            return;
        }
        if (entry.stuck) {
            if (!gun.isStuck(stack)) {
                gun.setStuck(true, gun.rootNodeTag(stack));
            }
            entry.nbtWritten = true;
            return;
        }
        if (entry.needsNbtClear) {
            if (gun.isStuck(stack)) {
                gun.setStuck(false, gun.rootNodeTag(stack));
            }
            entry.needsNbtClear = false;
        }
    }

    /**
     * 客户端每 tick 调用一次：先让超时的本地预测自愈，再把当前信念投影到手持枪的 NBT。
     *
     * <p>不监听世界卸载/维度切换：换维度只是换 ClientLevel，玩家的枪还是那把枪，
     * 缓存必须继续生效。只有真正断线才清空（见 {@link #clearAll()}）。</p>
     */
    @OnlyIn(Dist.CLIENT)
    public void tick(LocalPlayer player) {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Entry> mapEntry : entries.entrySet()) {
            Entry entry = mapEntry.getValue();
            if (entry.stuck && !entry.confirmed && now - entry.stampMs > PREDICTION_TIMEOUT_MS) {
                // 服务端始终会对处理过的开火回执；超时说明这一发根本没有被服务端接收
                // （切枪后身份不匹配被丢弃、掉包、连接断开），本地预测必须作废。
                markCleared(entry);
            }
        }

        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) {
            return;
        }
        IGun gun = gunItem.getGun();
        String gunId = gun.getIdentityID(stack);
        if (gunId == null || IGun.NONE.equals(gunId)) {
            return;
        }
        Entry entry = entries.get(gunId);
        if (entry == null) {
            return;
        }
        projectToNbt(stack, gun);
        // 已解除且 NBT 也擦干净了：tombstone 没有存在意义
        if (!entry.stuck && !entry.needsNbtClear) {
            entries.remove(gunId, entry);
        }
    }

    /**
     * 断线时清空。
     *
     * <p>退出游戏不会丢真实卡壳：它由服务端保存在物品数据里，重进后随背包同步回来，
     * 既有 NBT 读取路径会照常显示。反过来，把本地预测带过连接边界才是危险的——
     * 那个预测可能永远等不到确认，就会变成跨存档的虚假卡壳。</p>
     */
    public void clearAll() {
        entries.clear();
    }

    /** 仅用于调试/测试：当前是否有任何记录。 */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    // =====================================================================
    // 内部工具
    // =====================================================================

    private Entry entryOf(ItemStack stack, IGun gun) {
        if (stack == null || stack.isEmpty() || gun == null) {
            return null;
        }
        String gunId = gun.getIdentityID(stack);
        if (gunId == null || IGun.NONE.equals(gunId)) {
            return null;
        }
        return entries.get(gunId);
    }

    /** 把记录标记为“已解除”，并保留 NBT 擦除义务（如果我们本地写过 stuck=true）。 */
    private static void markCleared(Entry entry) {
        entry.stuck = false;
        entry.confirmed = false;
        entry.stampMs = System.currentTimeMillis();
        entry.needsNbtClear = entry.nbtWritten;
    }

    private void trimIfNeeded() {
        if (entries.size() <= MAX_ENTRIES) {
            return;
        }
        // 优先丢弃已经没有 NBT 擦除义务的 tombstone
        for (Map.Entry<String, Entry> mapEntry : entries.entrySet()) {
            Entry entry = mapEntry.getValue();
            if (!entry.stuck && !entry.needsNbtClear) {
                entries.remove(mapEntry.getKey(), entry);
                if (entries.size() <= MAX_ENTRIES) {
                    return;
                }
            }
        }
    }
}
