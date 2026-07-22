package com.sheridan.gcr.modularSys.fire.closedBolt;

import com.sheridan.gcr.modularSys.modules.guns.ak.AK;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class AKSemi extends AKFireMode{
    public static final AKSemi SEMI = new AKSemi();

    public AKSemi() {
        super("AK_SEMI");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void triggerClientShoot(Player player, ItemStack stack, AK gun) {
        super.triggerClientShootSemi(player, stack, gun);
    }
}