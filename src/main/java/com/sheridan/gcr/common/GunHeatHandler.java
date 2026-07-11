package com.sheridan.gcr.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;


public class GunHeatHandler {



    @SubscribeEvent
    public static void onServerPlayerTickPre(PlayerTickEvent.Pre event) {
        Player entity = event.getEntity();
        if (entity instanceof ServerPlayer player) {
            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }
            int tickCount = server.getTickCount();
            Level level = entity.level();
            long gameTime = level.getGameTime();

        }

    }

    @SubscribeEvent
    public static void onServerPlayerTickPre(PlayerTickEvent.Post event) {

    }
}
