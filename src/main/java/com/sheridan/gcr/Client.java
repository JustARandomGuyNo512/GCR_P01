package com.sheridan.gcr;


import com.sheridan.gcr.client.ClientWeaponLooper;
import com.sheridan.gcr.client.DrawHolsterHandler;
import com.sheridan.gcr.client.WeaponStatus;
import com.sheridan.gcr.client.events.ControllerEvents;
import com.sheridan.gcr.client.render.DefaultGunRenderer;
import com.sheridan.gcr.client.render.IGunRenderer;
import com.sheridan.gcr.events.LivingFireEvent;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.fire.IFireMode;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.s2c.BroadcastLivingFirePacket;
import com.sheridan.gcr.network.s2c.GunFireAckPacket;
import com.sheridan.gcr.network.s2c.InitClientGunDataPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Client {
    /**
     * 客户端武器线程：只跑一个长期存活的 {@link ClientWeaponLooper}。
     *
     * <p>开火时间轴、后座力推进、动画推进现在都由这一个循环按各自的截止时刻驱动，
     * 保证它们仍然串行在同一个线程上（{@code RecoilUpdater} 之类并不线程安全）。</p>
     */
    @OnlyIn(Dist.CLIENT)
    public static final ScheduledExecutorService WEAPON_SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    @OnlyIn(Dist.CLIENT)
    public static boolean isUsingIrisShader;
    @OnlyIn(Dist.CLIENT)
    public static final WeaponStatus WEAPON_STATUS = new WeaponStatus();
    @OnlyIn(Dist.CLIENT)
    public static boolean handleWeaponBobbing;
    @OnlyIn(Dist.CLIENT)
    public static final Matrix4f FIRST_PERSON_PROJECTION_MAT = new Matrix4f();
    @OnlyIn(Dist.CLIENT)
    public static RenderLevelStageEvent.Stage currentStage;

    @OnlyIn(Dist.CLIENT)
    private static IGunRenderer GUN_RENDERER_INSTANCE;
    @OnlyIn(Dist.CLIENT)
    private static final IGunRenderer DEFAULT_GUN_RENDERER_INSTANCE = new DefaultGunRenderer();
    @NotNull
    @OnlyIn(Dist.CLIENT)
    public static IGunRenderer getGunRenderer() {
        return GUN_RENDERER_INSTANCE == null ? DEFAULT_GUN_RENDERER_INSTANCE : GUN_RENDERER_INSTANCE;
    }

    @OnlyIn(Dist.CLIENT)
    public static void setGunRenderer(IGunRenderer gunRenderer) {
        GUN_RENDERER_INSTANCE = gunRenderer;
    }

    @OnlyIn(Dist.CLIENT)
    public static void useDefaultGunRenderer() {
        setGunRenderer(DEFAULT_GUN_RENDERER_INSTANCE);
    }

    static {
        setGunRenderer(DEFAULT_GUN_RENDERER_INSTANCE);
    }

    @OnlyIn(Dist.CLIENT)
    public static final AtomicBoolean LEFT_BUTTON_PRESSED = new AtomicBoolean(false);

    @OnlyIn(Dist.CLIENT)
    public static final AtomicBoolean RIGHT_BUTTON_PRESSED = new AtomicBoolean(false);

    @OnlyIn(Dist.CLIENT)
    public static final AtomicBoolean CANCEL_WEAPON_LOOPER = new AtomicBoolean(false);

    @OnlyIn(Dist.CLIENT)
    public static final ReentrantLock LOCK = new ReentrantLock();

    @OnlyIn(Dist.CLIENT)
    public static int LOCAL_PLAYER_ID = -1;

    @OnlyIn(Dist.CLIENT)
    public static float distFromLastJump() {
        return (System.currentTimeMillis() - WEAPON_STATUS.getLastJump()) * 0.001f;
    }

    @OnlyIn(Dist.CLIENT)
    public static void exitAds() {
        RIGHT_BUTTON_PRESSED.set(false);
        WEAPON_STATUS.isAiming = false;
        ControllerEvents.adsDelay = 1;
    }

    @OnlyIn(Dist.CLIENT)
    public static AtomicInteger CLIENT_SHOOT_ID = new AtomicInteger(0);

    @OnlyIn(Dist.CLIENT)
    public static volatile int LAST_SERVER_ACK_SHOOT_ID = 0;

    @OnlyIn(Dist.CLIENT)
    public static volatile long LAST_SERVER_ACK_SHOOT_TIME = 0;

    @OnlyIn(Dist.CLIENT)
    public static int MAX_SHADER_TEXTURES = 16;


    @OnlyIn(Dist.CLIENT)
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 旧实现是三个固定周期任务（开火 5ms / 后座力 10ms / 动画 10ms）：开火被量化到
        // 整数个 5ms 步进上，实际射速永远对不上理论值。现在换成一个按绝对截止时刻驱动的
        // 小循环，开火与那两路 100Hz 推进仍然串行在同一个线程上，详见 ClientWeaponLooper。
        WEAPON_SCHEDULER.execute(new ClientWeaponLooper());

        ClientTestingResources.init(event);

    }


    /**
     * 在武器线程上尝试打出这一发。调用方（{@link ClientWeaponLooper}）只在确实要开火时才进来，
     * 因此这里的锁是真开火事件才付出代价。
     *
     * <p>锁的纪律：客户端主线程在整个 tick（{@code ClientTickEvent.Pre ~ Post}）里持有这把锁，
     * 本方法也在持锁状态下改物品数据、卡壳缓存、后座力，两边因此不会同时动同一份状态。
     * 拿锁期间不能反过来去等主线程（例如 {@code Minecraft.execute} 之后 join），否则会死锁。</p>
     *
     * @return 这一发打出后到下一发的精确间隔（纳秒）；{@code <= 0} 表示这一发没打出去
     *         （拔枪动画未完成 / 没有开火模式 / 冲刺 / 换弹 / 空仓 / 卡壳）
     */
    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("unchecked")
    public static long handleClientShoot(ItemStack stack, IGun gun, Player player) {
        try {
            LOCK.lock();
            if (DrawHolsterHandler.get().getEquipProgress() < 1f) {
                return 0L;
            }
            IFireMode fireMode = gun.getFireMode(stack);
            if (fireMode == null) {
                IFireMode.stopFire();
                return 0L;
            }
            IFireMode.FireControl fireControl = fireMode.clientIntentToFire(player, stack, gun);
            if (fireControl == IFireMode.FireControl.ALLOW_FIRE) {
                // 间隔取「开火之前」的枪械状态：这一发本身可能打空弹匣、卡壳或触发换弹，改掉射速
                long interval = WEAPON_STATUS.getFireIntervalNanos();
                try {
                    fireMode.triggerClientShoot(player, stack, gun);
                } catch (Exception ignored) {}
                return interval;
            }  else {
                if (fireControl == IFireMode.FireControl.EXIT_FIRE_STATE) {
                    IFireMode.stopFire();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            LOCK.unlock();
        }
        return 0L;
    }

    public static void serverShootAck(GunFireAckPacket packet, ItemStack itemStack, IGun gun) {
        LAST_SERVER_ACK_SHOOT_ID = packet.shootId;
        LAST_SERVER_ACK_SHOOT_TIME = System.currentTimeMillis();
        gun.serverShootAck(packet, itemStack);
    }

    public static void onReceivedGunDataFromServer(InitClientGunDataPacket packet) {
        Minecraft instance = Minecraft.getInstance();
        LocalPlayer player = instance.player;
        if (player != null && player.getMainHandItem().getItem() instanceof GunItem gunItem) {
            String identityID = gunItem.getGun().getIdentityID(player.getMainHandItem());
            if (!IGun.NONE.equals(identityID)) {
                return;
            }
            if (packet.itemId != Item.getId(gunItem)) {
                return;
            }
            IGun gun = gunItem.getGun();
            if (!Objects.equals(gun.getID(), packet.moduleId)) {
                return;
            }
            gun.fullSyncFromServer(player.getMainHandItem(), packet.data);
        }
    }

    public static void handleLivingFire(BroadcastLivingFirePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            Level level = player.level();

            Entity entity = level.getEntity(packet.entityId());
            if (!(entity instanceof LivingEntity living)) {
                return;
            }
            ItemStack mainHandItem = living.getMainHandItem();
            if (mainHandItem.getItem() instanceof GunItem gunItem) {
                String gunId = packet.gunId;
                IGun gun = gunItem.getGun();
                if (gunId.equals(gun.getIdentityID(mainHandItem))) {
                    NeoForge.EVENT_BUS.post(
                            new LivingFireEvent(
                                    living,
                                    gun,
                                    packet.fireModuleId,
                                    gunId,
                                    mainHandItem,
                                    packet.latency
                            )
                    );
                }
            }
        });
    }

    public static void onReceivedLivingFire(int entityID, int shootID) {

    }


    public static boolean isAiming() {
        return WEAPON_STATUS.isAiming();
    }

    public static float getAimingProgress() {
        return WEAPON_STATUS.getAimingProgress();
    }

    public static float getAimingProgress(float partialTicks) {
        return WEAPON_STATUS.getAimingProgress(partialTicks);
    }

    public static float distFromLastShoot() {
        return (System.nanoTime() - WEAPON_STATUS.lastShoot) * 1e-9f;
    }

//    public static void syncGunHeatData(SyncHeatDataPacket packet) {
//        LocalPlayer player = Minecraft.getInstance().player;
//        if (player != null) {
//            ItemStack mainHandItem = player.getMainHandItem();
//            if (mainHandItem.getItem() instanceof GunItem gunItem) {
//                IGun gun = gunItem.getGun();
//                String identityID = gun.getIdentityID(mainHandItem);
//                if (Objects.equals(identityID, packet.gunId)) {
//                    gun.setCurrHeat(mainHandItem, packet.heat, packet.lastHeatUpdateTime, packet.lastShootTime);
//                }
//            }
//        }
//    }
}
