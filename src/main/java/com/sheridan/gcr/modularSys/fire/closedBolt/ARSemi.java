package com.sheridan.gcr.modularSys.fire.closedBolt;

import com.sheridan.gcr.modularSys.modules.guns.ar.AR;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ARSemi extends ARFireMode{
    public static final ARSemi SEMI = new ARSemi();

    public ARSemi() {
        super("AR_SEMI");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void triggerClientShoot(Player player, ItemStack stack, AR gun) {
        super.triggerClientShootSemi(player, stack, gun);
    }
}
