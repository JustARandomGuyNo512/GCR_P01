package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.ModuleRegister;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.impl.SubWeapon;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.sheridan.gcr.network.PacketContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SubWeaponReloadPacket implements IPacket<SubWeaponReloadPacket> {
    public static final ResourceLocation ID = GCR.RL("sub_weapon_reload");

    public String gunId;
    public String nodeId;

    public SubWeaponReloadPacket(String gunId, String nodeId) {
        this.gunId = gunId;
        this.nodeId = nodeId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(gunId);
        buf.writeUtf(nodeId);
    }

    public static SubWeaponReloadPacket decode(FriendlyByteBuf buf) {
        return new SubWeaponReloadPacket(
                buf.readUtf(),
                buf.readUtf()
        );
    }

    @Override
    public void onClient(SubWeaponReloadPacket packet, PacketContext context) {

    }

    @Override
    public void onServer(SubWeaponReloadPacket packet, PacketContext context) {
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
    }}


