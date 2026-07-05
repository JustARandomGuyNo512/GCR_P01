package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.fire.IFireMode;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.Codec;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class GunFirePacket implements CustomPacketPayload, IPacket<GunFirePacket> {
    public static final ResourceLocation ID = GCR.RL("gun_fire");
    public static final Type<GunFirePacket> TYPE = new Type<>(ID);
    public static final Codec<GunFirePacket> STREAM_CODEC = new Codec<> (
            GunFirePacket::decode,
            (buf, p) -> p.encode(buf));


    public int shootId;
    public float gunKickPitch;
    public float gunKickYaw;

    public GunFirePacket(int shootId, float gunKickPitch, float gunKickYaw) {
        this.shootId = shootId;
        this.gunKickPitch = gunKickPitch;
        this.gunKickYaw = gunKickYaw;
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeInt(shootId);
        buf.writeFloat(gunKickPitch);
        buf.writeFloat(gunKickYaw);
    }

    private static GunFirePacket decode(FriendlyByteBuf buf) {
        return new GunFirePacket(
                buf.readInt(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    @Override
    public void onClient(GunFirePacket packet, IPayloadContext context) {

    }

    @Override
    public void onServer(GunFirePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem gunItem) {
                IGun gunModule = gunItem.getGun();
                IFireMode<?> fireMode = gunModule.getFireMode(heldItem);
                if (fireMode == null) {
                    return;
                }
                Class<?> gunClass = fireMode.getGunClass();
                if (gunClass.isInstance(gunModule)) {
                    invokeServerShoot(fireMode, player, heldItem, gunModule, GunFirePacket.this);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T extends IGun> void invokeServerShoot(
            IFireMode<T> fireMode, ServerPlayer player, ItemStack stack, IGun gunModule, GunFirePacket packet) {
        fireMode.triggerServerShoot(player, stack, (T) gunModule, packet);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}


