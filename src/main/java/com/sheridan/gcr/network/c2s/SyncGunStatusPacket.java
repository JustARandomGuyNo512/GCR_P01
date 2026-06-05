package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.states.State;
import com.sheridan.gcr.network.Codec;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class SyncGunStatusPacket implements CustomPacketPayload, IPacket<SyncGunStatusPacket> {
    public static final ResourceLocation ID = GCR.RL("sync_gun_status");
    public static final Type<SyncGunStatusPacket> TYPE = new Type<>(ID);
    public static final Codec<SyncGunStatusPacket> STREAM_CODEC = new Codec<> (
            SyncGunStatusPacket::decode,
            (buf, p) -> p.encode(buf));

    public String identityID;
    public CompoundTag payloadTag;
    public String nodeId;

    public SyncGunStatusPacket(CompoundTag statesTag, String nodeId, String identityID, State<?>... states) {
        this.payloadTag = new CompoundTag();
        for (State<?> state : states) {
            state.encode(statesTag, payloadTag);
        }
        this.identityID = identityID;
        this.nodeId = nodeId;
    }

    public static void toServer(IGun gun, ItemStack itemStack, String nodeId, State<?>... states) {
        CompoundTag nodeStatesTag = gun.getNodeStatesTag(itemStack, nodeId);
        if (nodeStatesTag == null) {
            return;
        }
        String identityID = gun.getIdentityID(itemStack);
        SyncGunStatusPacket syncGunStatusPacket = new SyncGunStatusPacket(nodeStatesTag, nodeId, identityID, states);
        PacketDistributor.sendToServer(syncGunStatusPacket);
    }

    public static void toServer(IGun gun, ItemStack itemStack, State<?>... states) {
        toServer(gun, itemStack, gun.rootNodeId(itemStack), states);
    }

    public SyncGunStatusPacket() {}

    @Override
    public @NotNull Type<? extends SyncGunStatusPacket> type() {
        return TYPE;
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeUtf(identityID);
        buf.writeUtf(nodeId);
        buf.writeNbt(payloadTag);
    }

    private static SyncGunStatusPacket decode(FriendlyByteBuf buf) {
        SyncGunStatusPacket packet = new SyncGunStatusPacket();
        packet.identityID = buf.readUtf();
        packet.nodeId = buf.readUtf();
        packet.payloadTag = buf.readNbt();
        return packet;
    }

    @Override
    public void onClient(SyncGunStatusPacket packet, IPayloadContext context) {}

    @Override
    public void onServer(SyncGunStatusPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem gunItem) {
                IGun gunModule = gunItem.getGun();
                if (gunModule.getIdentityID(heldItem).equals(packet.identityID)) {
                    gunModule.onReceiveStatesDataFormClient(packet.payloadTag, heldItem, packet.nodeId, player);
                }
            }
        });
    }
}