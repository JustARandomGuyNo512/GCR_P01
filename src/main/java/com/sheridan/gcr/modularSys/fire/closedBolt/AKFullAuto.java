package com.sheridan.gcr.modularSys.fire.closedBolt;

import com.sheridan.gcr.modularSys.modules.guns.ak.AK;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class AKFullAuto  extends AKFireMode {
    public static final AKFireMode FULL_AUTO = new AKFullAuto();

    public AKFullAuto() {
        super("AK_FULL_AUTO");
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void triggerClientShoot(Player player, ItemStack stack, AK gun) {
        super.triggerClientShootFullAuto(player, stack, gun);
    }

}