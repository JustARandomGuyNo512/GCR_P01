package com.sheridan.gcr.client.events;


import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.DrawHolsterHandler;
import com.sheridan.gcr.client.GunEffect;
import com.sheridan.gcr.client.GunEffectManager;
import com.sheridan.gcr.client.SprintingHandler;
import com.sheridan.gcr.client.animation.AnimationHandler;
import com.sheridan.gcr.client.model.modular.ModularModel;
import com.sheridan.gcr.client.render.GunPoseHandler;
import com.sheridan.gcr.client.render.HardCodeAnimationHandler;
import com.sheridan.gcr.client.render.Shaders;
import com.sheridan.gcr.events.LivingFireEvent;
import com.sheridan.gcr.modularSys.task.GunTaskHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;


@OnlyIn(Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (Minecraft.getInstance().screen == null) {
            Client.getGunRenderer().setHideFPRender(false);
        }
        try {
            Client.LOCK.lock();
        } catch (Exception ignored) {}
        AnimationHandler.INSTANCE.onClientTick();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            Client.LOCAL_PLAYER_ID = player.getId();
            Client.WEAPON_STATUS.onTickStart(player);
            SprintingHandler.INSTANCE.tick(player);
            GunTaskHandler.INSTANCE.tick(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            Client.WEAPON_STATUS.onTickEnd(player);
            GunPoseHandler.INSTANCE.tick(player);
            Client.getGunRenderer().tick(player);
            HardCodeAnimationHandler.getInstance().clientTick(player);
            DrawHolsterHandler.get().tick(player.getMainHandItem(), player.getInventory().selected);
        }
        Client.LOCK.unlock();
        //ModularModel.debugHeat = Math.max(0, ModularModel.debugHeat - 0.005f);
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
