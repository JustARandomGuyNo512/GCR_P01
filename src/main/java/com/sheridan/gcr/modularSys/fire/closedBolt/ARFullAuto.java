package com.sheridan.gcr.modularSys.fire.closedBolt;

import com.sheridan.gcr.modularSys.modules.guns.ar.AR;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ARFullAuto extends ARFireMode {
    public static final ARFireMode FULL_AUTO = new ARFullAuto();

    public ARFullAuto() {
        super("AR_FULL_AUTO");
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void triggerClientShoot(Player player, ItemStack stack, AR gun) {
        super.triggerClientShootFullAuto(player, stack, gun);
    }

}
