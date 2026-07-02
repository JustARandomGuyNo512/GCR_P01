package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.modules.IInteractiveModular;
import com.sheridan.gcr.modularSys.modules.UniqueModule;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.c2s.SubWeaponFirePacket;
import com.sheridan.gcr.network.c2s.SubWeaponReloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SubWeapon extends UniqueModule implements IInteractiveModular {
    public SubWeapon(ResourceLocation id, boolean fixedPosition, float weight, Direction direction) {
        super(id, fixedPosition, weight, direction);
    }

    public void serverShoot(SubWeaponFirePacket packet, ItemStack itemStack, ServerPlayer player, IGun gun, SubWeapon subWeapon) {

    }

    public void serverReload(SubWeaponReloadPacket packet, ItemStack itemStack, ServerPlayer player, IGun gun, SubWeapon subWeapon) {

    }
}
