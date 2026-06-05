package com.sheridan.gcr.data;

import com.sheridan.gcr.network.c2s.SyncPlayerStatusPacket;
import com.sheridan.gcr.network.s2c.BroadcastPlayerStatusPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;


@EventBusSubscriber
public class PlayerStatusEvents {

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        PlayerCommonStatus status = player.getData(ModData.PLAYER_STATUS);

        if (player.level().isClientSide()) {
            // --- 客户端逻辑：向服务器同步状态 ---
            if (status.dataChanged) {
                PacketDistributor.sendToServer(new SyncPlayerStatusPacket(
                        status.getGcrCredit(),
                        status.isReloading()
                ));
                status.dataChanged = false;
            }
        } else {
            // --- 服务器逻辑：向客户端广播状态 ---
            if (status.dataChanged) {
                PacketDistributor.sendToPlayersTrackingEntity(
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
        PlayerCommonStatus status = targetPlayer.getData(ModData.PLAYER_STATUS);
        ServerPlayer trackingPlayer = (ServerPlayer) event.getEntity();
        PacketDistributor.sendToPlayer(trackingPlayer,
                new BroadcastPlayerStatusPacket(
                        targetPlayer.getId(),
                        status.getGcrCredit(),
                        status.isReloading()
                ));
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {//server side
        ServerPlayer player = (ServerPlayer) event.getEntity();
        if (!player.hasData(ModData.PLAYER_STATUS)) {
            player.setData(ModData.PLAYER_STATUS, new PlayerCommonStatus());
        }
        PlayerCommonStatus status = player.getData(ModData.PLAYER_STATUS);
        PacketDistributor.sendToAllPlayers(new BroadcastPlayerStatusPacket(
                player.getId(),
                status.getGcrCredit(),
                status.isReloading()
        ));
        status.dataChanged = false;
    }

    public static void serverReceivedClientSync(ServerPlayer player, SyncPlayerStatusPacket packet) {
        if (!player.hasData(ModData.PLAYER_STATUS)) {
            player.setData(ModData.PLAYER_STATUS, new PlayerCommonStatus());
        }
        PlayerCommonStatus data = player.getData(ModData.PLAYER_STATUS);
        data.gcrCredit = packet.gcrCredit;
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
                if (!thatPlayer.hasData(ModData.PLAYER_STATUS)) {
                    thatPlayer.setData(ModData.PLAYER_STATUS, new PlayerCommonStatus());
                }
                PlayerCommonStatus data = thatPlayer.getData(ModData.PLAYER_STATUS);
                data.gcrCredit = packet.gcrCredit;
                data.reloading = packet.isReloading;
                data.dataChanged = false;
            }
        }
    }
}
