package com.sheridan.gcr.data;

import com.sheridan.gcr.network.c2s.SyncPlayerStatusPacket;
import com.sheridan.gcr.network.s2c.BroadcastPlayerStatusPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import com.sheridan.gcr.network.GCRNetwork;


public class PlayerStatusEvents {

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Player player = event.player;
        PlayerCommonStatus status = ModData.get(player);

        if (player.level().isClientSide()) {
            // --- 客户端逻辑：向服务器同步状态 ---
            if (status.dataChanged) {
                GCRNetwork.sendToServer(new SyncPlayerStatusPacket(
                        status.getGcrCredit(),
                        status.isReloading()
                ));
                status.dataChanged = false;
            }
        } else {
            // --- 服务器逻辑：向客户端广播状态 ---
            if (status.dataChanged) {
                GCRNetwork.sendToPlayersTrackingEntity(
                        player,
                        new BroadcastPlayerStatusPacket(
                                player.getId(),
                                status.getGcrCredit(),
                                status.isReloading()
                        ));
                status.dataChanged = false;
            }
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof ServerPlayer targetPlayer)) {
            return;
        }
        PlayerCommonStatus status = ModData.get(targetPlayer);
        ServerPlayer trackingPlayer = (ServerPlayer) event.getEntity();
        GCRNetwork.sendToPlayer(trackingPlayer,
                new BroadcastPlayerStatusPacket(
                        targetPlayer.getId(),
                        status.getGcrCredit(),
                        status.isReloading()
                ));
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {//server side
        ServerPlayer player = (ServerPlayer) event.getEntity();
        if (!ModData.has(player)) {
            ModData.set(player, new PlayerCommonStatus());
        }
        PlayerCommonStatus status = ModData.get(player);
        GCRNetwork.sendToAllPlayers(new BroadcastPlayerStatusPacket(
                player.getId(),
                status.getGcrCredit(),
                status.isReloading()
        ));
        status.dataChanged = false;
    }

    public static void serverReceivedClientSync(ServerPlayer player, SyncPlayerStatusPacket packet) {
        if (!ModData.has(player)) {
            ModData.set(player, new PlayerCommonStatus());
        }
        PlayerCommonStatus data = ModData.get(player);
        // Client packets must never change server-owned currency balances.
        // Only the animation/reload state is accepted here.
        data.reloading = packet.isReloading;
        data.dataChanged = true;
    }

    @OnlyIn(Dist.CLIENT)
    public static void clientReceivedServerBroadcast(BroadcastPlayerStatusPacket packet) {
        int entityID = packet.entityID;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            Level level = player.level();
            Entity entity = level.getEntity(entityID);
            if (entity instanceof Player thatPlayer) {
                if (!ModData.has(thatPlayer)) {
                    ModData.set(thatPlayer, new PlayerCommonStatus());
                }
                PlayerCommonStatus data = ModData.get(thatPlayer);
                data.gcrCredit = packet.gcrCredit;
                data.reloading = packet.isReloading;
                data.dataChanged = false;
            }
        }
    }
}
