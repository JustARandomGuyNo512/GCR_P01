package com.sheridan.gcr.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class GunEffectManager {
    private static int updateDelay = 0;
    private static final Map<Integer, Map<String, Map<GunEffect, Long>>> EFFECT_TIMESTAMPS = new HashMap<>();


    public static long getEffectTimestampNano(LivingEntity livingEntity, GunEffect effect, String moduleId) {
        if (livingEntity == null) {
            return -1;
        }
        return EFFECT_TIMESTAMPS.getOrDefault(livingEntity.getId(), new HashMap<>())
                .getOrDefault(moduleId, new HashMap<>())
                .getOrDefault(effect, -1L);
    }

    public static long getEffectTimestamp(int entityId, GunEffect effect, String moduleId) {
        return EFFECT_TIMESTAMPS.getOrDefault(entityId, new HashMap<>())
                .getOrDefault(moduleId, new HashMap<>())
                .getOrDefault(effect, -1L);
    }

    public static void updateEffectTimestamp(int entityId, GunEffect effect, String moduleId, long timestamp) {
        EFFECT_TIMESTAMPS.computeIfAbsent(entityId, id -> new HashMap<>())
                .computeIfAbsent(moduleId, module -> new HashMap<>())
                .put(effect, timestamp);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre event) {
        if (updateDelay < 20) {
            updateDelay++;
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            Level level = player.level();

            // 使用 removeIf 一句话搞定，安全高效
            EFFECT_TIMESTAMPS.keySet().removeIf(entityId -> level.getEntity(entityId) == null);
        }
        updateDelay = 0;
    }
}
