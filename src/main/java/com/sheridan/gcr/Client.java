package com.sheridan.gcr;

import com.sheridan.gcr.client.ClientWeaponLooper;
import com.sheridan.gcr.client.DrawHolsterHandler;
import com.sheridan.gcr.client.WeaponStatus;
import com.sheridan.gcr.client.recoil.IRecoilCameraHandler;
import com.sheridan.gcr.client.recoil.RecoilCameraHandler;
import com.sheridan.gcr.client.recoil.RecoilHandler;
import com.sheridan.gcr.client.render.DefaultGunRenderer;
import com.sheridan.gcr.client.render.HardCodeAnimationHandler;
import com.sheridan.gcr.client.render.IGunRenderer;
import com.sheridan.gcr.client.render.IHardCodeAnimationHandler;
import com.sheridan.gcr.events.LivingFireEvent;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.fire.IFireMode;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.s2c.BroadcastLivingFirePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class Client {
    @OnlyIn(Dist.CLIENT)
    public static boolean DEBUG_ALWAYS_STUCK = false;
    @OnlyIn(Dist.CLIENT)
    public static boolean DEV_FREE_CAMERA = false;
    @OnlyIn(Dist.CLIENT)
    public static final ScheduledExecutorService WEAPON_SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    @OnlyIn(Dist.CLIENT)
    public static boolean isIrisShaderInUse;
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
    public static float distFromLastJump() {
        return (System.currentTimeMillis() - WEAPON_STATUS.getLastJump()) * 0.001f;
    }

    @OnlyIn(Dist.CLIENT)
    public static void exitAds() {
        RIGHT_BUTTON_PRESSED.set(false);
        WEAPON_STATUS.isAiming = false;
    }

    @OnlyIn(Dist.CLIENT)
    public static void onClientSetup(FMLClientSetupEvent event) {
        WEAPON_SCHEDULER.scheduleAtFixedRate(new ClientWeaponLooper(), 0, 5L, TimeUnit.MILLISECONDS); // 500Hz

        WEAPON_SCHEDULER.scheduleAtFixedRate(
                () -> {
                    RecoilHandler.INSTANCE.update(0.01f);
                    IRecoilCameraHandler instance = RecoilCameraHandler.getInstance();
                    if (instance != null) {
                        instance.update(0.01f);
                    }
                },
                0,
                10L,
                TimeUnit.MILLISECONDS);

        WEAPON_SCHEDULER.scheduleAtFixedRate(
                () -> {
                    IHardCodeAnimationHandler instance = HardCodeAnimationHandler.getInstance();
                    if (instance != null) {
                        instance.update(0.01f);
                    }
                },
                0,
                10L,
                TimeUnit.MILLISECONDS);

        ClientTestingResources.init(event);

    }


    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("unchecked")
    public static int handleClientShoot(ItemStack stack, IGun gun, Player player) {
        try {
            LOCK.lock();
            if (DrawHolsterHandler.get().getEquipProgress() < 1f) {
                return 0;
            }
            IFireMode fireMode = gun.getFireMode(stack);
            if (fireMode != null && fireMode.clientIntentToFire(player, stack, gun)) {
                int delay = WEAPON_STATUS.getFireDelayTick();
                try {
                    fireMode.triggerClientShoot(player, stack, gun);
                } catch (Exception ignored) {}
                return delay;
            }  else {
                IFireMode.stopFire();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            LOCK.unlock();
        }
        return 0;
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

    public static long lastShootMain() {
        return WEAPON_STATUS.lastShoot;
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
        return (System.currentTimeMillis() - WEAPON_STATUS.lastShoot) * 0.001f;
    }
}
