package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.ModuleRegister;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.impl.SubWeapon;
import com.sheridan.gcr.network.Codec;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SubWeaponReloadPacket implements CustomPacketPayload, IPacket<SubWeaponReloadPacket> {
    public static final ResourceLocation ID = GCR.RL("sub_weapon_reload");
    public static final Type<SubWeaponReloadPacket> TYPE = new Type<>(ID);
    public static final Codec<SubWeaponReloadPacket> STREAM_CODEC = new Codec<> (
            SubWeaponReloadPacket::decode,
            (buf, p) -> p.encode(buf));

    public String gunId;
    public String nodeId;

    public SubWeaponReloadPacket(String gunId, String nodeId) {
        this.gunId = gunId;
        this.nodeId = nodeId;
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeUtf(gunId);
        buf.writeUtf(nodeId);
    }

    private static SubWeaponReloadPacket decode(FriendlyByteBuf buf) {
        return new SubWeaponReloadPacket(
                buf.readUtf(),
                buf.readUtf()
        );
    }

    @Override
    public void onClient(SubWeaponReloadPacket packet, IPayloadContext context) {

    }

    @Override
    public void onServer(SubWeaponReloadPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem gunItem) {
                IGun gun = gunItem.getGun();
                String identityID = gun.getIdentityID(heldItem);
                if (Objects.equals(identityID, packet.gunId)) {
                    ListTag modulesTag = gun.getModulesTag(heldItem);
                    for (int i = 0; i < modulesTag.size(); i++) {
                        CompoundTag compound = modulesTag.getCompound(i);
                        String id = compound.getString(Unit.IN_TIME_ID);
                        if (Objects.equals(id, packet.nodeId)) {
                            String moduleId = compound.getString(Unit.MODULE_ID);
                            SubWeapon subWeapon = ModuleRegister.get(moduleId, SubWeapon.class);
                            if (subWeapon != null) {
                                subWeapon.serverReload(packet, heldItem, player, gun, subWeapon);
                            }
                            return;
                        }
                    }
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}


