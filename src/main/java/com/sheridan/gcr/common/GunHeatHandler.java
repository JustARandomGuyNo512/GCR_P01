package com.sheridan.gcr.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;


public class GunHeatHandler {



    @SubscribeEvent
    public static void onServerPlayerTickPre(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Player entity = event.player;
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

}
