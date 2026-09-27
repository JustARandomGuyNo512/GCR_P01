package com.sheridan.gcr.client.events;


import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.DrawHolsterHandler;
import com.sheridan.gcr.client.GunEffect;
import com.sheridan.gcr.client.GunEffectManager;
import com.sheridan.gcr.client.SprintingHandler;
import com.sheridan.gcr.client.animation.AnimationHandler;
import com.sheridan.gcr.client.render.GunPoseHandler;
import com.sheridan.gcr.client.render.HardCodeAnimationHandler;
import com.sheridan.gcr.client.render.Shaders;
import com.sheridan.gcr.client.stuck.ClientGunStuckCache;
import com.sheridan.gcr.events.LivingFireEvent;
import com.sheridan.gcr.modularSys.task.GunTaskHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

import java.util.concurrent.locks.ReentrantLock;


@OnlyIn(Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (Minecraft.getInstance().screen == null) {
            Client.getGunRenderer().setHideFPRender(false);
        }
        // 上一个 tick 如果在持锁期间异常退出，Post 就不会再被派发，这把锁会永远留在主线程手里，
        // 武器线程随即永久阻塞在 Client.LOCK.lock() 上——整局都开不了火。进临界区之前先把
        // 自己残留的持有次数还清；正常情况下这里本来就持有 0 次，是个空操作。
        releaseTickLock();
        Client.LOCK.lock();
        try {
            AnimationHandler.INSTANCE.onClientTick();
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                Client.LOCAL_PLAYER_ID = player.getId();
                Client.WEAPON_STATUS.onTickStart(player);
                SprintingHandler.INSTANCE.tick(player);
                GunTaskHandler.INSTANCE.tick(player);
            }
        } catch (Throwable t) {
            // 本 tick 提前失败：立刻归还，不能让异常把锁带走
            releaseTickLock();
            throw t;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(ClientTickEvent.Post event) {
        try {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                Client.WEAPON_STATUS.onTickEnd(player);
                GunPoseHandler.INSTANCE.tick(player);
                Client.getGunRenderer().tick(player);
                HardCodeAnimationHandler.getInstance().clientTick(player);
                DrawHolsterHandler.get().tick(player, player.getMainHandItem(), player.getInventory().selected);
                // 客户端卡壳缓存：超时的本地预测自愈 + 把信念投影回手持枪的 states。
                // 换维度不会中断它（同一把枪、同一个 identityID），只有断线才清空。
                ClientGunStuckCache.get().tick(player);
            }
            // 刷新开火线程的无锁预检：没拿枪 / 拔枪动画没完 / 换弹 / 冲刺都在这里体现，
            // 让武器线程平时不必为了「能不能开火」去抢这把锁。
            Client.WEAPON_STATUS.updateFireBlocked();
        } finally {
            releaseTickLock();
        }
        //ModularModel.debugHeat = Math.max(0, ModularModel.debugHeat - 0.005f);
    }

    /**
     * 归还当前线程（客户端主线程）持有的 tick 锁。
     *
     * <p>按持有次数归还而不是只 unlock 一次：{@code ClientModelHotReloader} 之类会在 tick 里
     * 重入这把锁，异常路径下可能留下多层持有。</p>
     */
    private static void releaseTickLock() {
        ReentrantLock lock = Client.LOCK;
        for (int holds = lock.getHoldCount(); holds > 0; holds = lock.getHoldCount()) {
            lock.unlock();
        }
    }

    /**
     * 断线时清空客户端卡壳缓存。
     *
     * <p>真实卡壳由服务端保存在物品数据里，重进后随背包同步回来，既有 NBT 读取路径照常显示；
     * 反向把本地预测带过连接边界才是危险的——那个预测可能永远等不到确认，就变成跨存档的虚假卡壳。</p>
     */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientGunStuckCache.get().clearAll();
    }

    public static void registerCustomVanillaShader(RegisterShadersEvent event) {
        Shaders.init(event.getResourceProvider());
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getId() == entity.getId()) {
            Client.WEAPON_STATUS.onPlayerJump();
        }
    }

    @SubscribeEvent
    public static void onOtherLivingFire(LivingFireEvent event) {
        GunEffectManager.updateEffectTimestamp(
                event.getEntity().getId(),
                GunEffect.SHOOT,
                event.getFireModuleId(),
                System.currentTimeMillis() - event.getShooterLatency()
        );
    }

}
