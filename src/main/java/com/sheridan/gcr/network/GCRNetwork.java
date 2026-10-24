package com.sheridan.gcr.network;

import com.sheridan.gcr.GCR;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import com.sheridan.gcr.network.c2s.CommitModuleTreePacket;
import com.sheridan.gcr.network.c2s.PhysicalMagazinePacket;
import com.sheridan.gcr.network.c2s.GunFirePacket;
import com.sheridan.gcr.network.c2s.GunReloadPacket;
import com.sheridan.gcr.network.c2s.PlaySoundPacket;
import com.sheridan.gcr.network.c2s.RemoveStuckPacket;
import com.sheridan.gcr.network.c2s.SubWeaponFirePacket;
import com.sheridan.gcr.network.c2s.SubWeaponReloadPacket;
import com.sheridan.gcr.network.c2s.SwitchUsingSightPacket;
import com.sheridan.gcr.network.c2s.SyncGunStatusPacket;
import com.sheridan.gcr.network.c2s.SyncPlayerStatusPacket;
import com.sheridan.gcr.network.s2c.BroadcastLivingFirePacket;
import com.sheridan.gcr.network.s2c.BroadcastPlayerStatusPacket;
import com.sheridan.gcr.network.s2c.CommitModuleTreeResponsePacket;
import com.sheridan.gcr.network.s2c.GunFireAckPacket;
import com.sheridan.gcr.network.s2c.GunStuckSyncPacket;
import com.sheridan.gcr.network.s2c.InitClientGunDataPacket;

/** Forge 1.20.1 network; explicit registration order is part of the wire protocol. */
public final class GCRNetwork {
    private static final String VERSION = "gcr-forge1201-physical-magazines-v1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(GCR.MODID, "main"),
        () -> VERSION,
        VERSION::equals,
        VERSION::equals
    );
    private static int nextId;
    private static boolean registered;
    private GCRNetwork() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        add(CommitModuleTreePacket.class, CommitModuleTreePacket::encode, CommitModuleTreePacket::decode);
        add(GunFirePacket.class, GunFirePacket::encode, GunFirePacket::decode);
        add(GunReloadPacket.class, GunReloadPacket::encode, GunReloadPacket::decode);
        add(PlaySoundPacket.class, PlaySoundPacket::encode, PlaySoundPacket::decode);
        add(RemoveStuckPacket.class, RemoveStuckPacket::encode, RemoveStuckPacket::decode);
        add(SubWeaponFirePacket.class, SubWeaponFirePacket::encode, SubWeaponFirePacket::decode);
        add(SubWeaponReloadPacket.class, SubWeaponReloadPacket::encode, SubWeaponReloadPacket::decode);
        add(SwitchUsingSightPacket.class, SwitchUsingSightPacket::encode, SwitchUsingSightPacket::decode);
        add(SyncGunStatusPacket.class, SyncGunStatusPacket::encode, SyncGunStatusPacket::decode);
        add(SyncPlayerStatusPacket.class, SyncPlayerStatusPacket::encode, SyncPlayerStatusPacket::decode);
        add(BroadcastLivingFirePacket.class, BroadcastLivingFirePacket::encode, BroadcastLivingFirePacket::decode);
        add(BroadcastPlayerStatusPacket.class, BroadcastPlayerStatusPacket::encode, BroadcastPlayerStatusPacket::decode);
        add(CommitModuleTreeResponsePacket.class, CommitModuleTreeResponsePacket::encode, CommitModuleTreeResponsePacket::decode);
        add(GunFireAckPacket.class, GunFireAckPacket::encode, GunFireAckPacket::decode);
        add(GunStuckSyncPacket.class, GunStuckSyncPacket::encode, GunStuckSyncPacket::decode);
        add(InitClientGunDataPacket.class, InitClientGunDataPacket::encode, InitClientGunDataPacket::decode);
        add(PhysicalMagazinePacket.class, PhysicalMagazinePacket::encode, PhysicalMagazinePacket::decode);
    }

    private static <T extends IPacket<T>> void add(Class<T> type,
            BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder) {
        CHANNEL.registerMessage(nextId++, type, encoder, decoder, (message, supplier) -> {
            NetworkEvent.Context context = supplier.get();
            PacketContext packetContext = new PacketContext(context);
            if (context.getDirection().getReceptionSide().isClient()) {
                message.onClient(message, packetContext);
            } else {
                message.onServer(message, packetContext);
            }
            context.setPacketHandled(true);
        });
    }

    public static void sendToServer(Object message) { CHANNEL.sendToServer(message); }
    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
    public static void sendToAllPlayers(Object message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }
    public static void sendToPlayersTrackingEntity(Entity entity, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
    }
}
