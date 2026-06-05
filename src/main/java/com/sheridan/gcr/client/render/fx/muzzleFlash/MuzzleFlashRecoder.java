package com.sheridan.gcr.client.render.fx.muzzleFlash;

import com.sheridan.gcr.Client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.WeakHashMap;

@OnlyIn(Dist.CLIENT)
public class MuzzleFlashRecoder {
    private static final Map<LivingEntity, Long> LAST_MUZZLE_FLASH = new WeakHashMap<>();

    public static void onEntityFire(LivingEntity livingEntity) {
        LAST_MUZZLE_FLASH.put(livingEntity, System.currentTimeMillis());
    }

    public static long get(LivingEntity livingEntity) {
        if (livingEntity == null) {
            return 0;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (livingEntity == player && livingEntity.getId() == player.getId()) {
            return Client.WEAPON_STATUS.lastShoot;
        } else {
            return LAST_MUZZLE_FLASH.getOrDefault(livingEntity, 0L);
        }
    }

}
