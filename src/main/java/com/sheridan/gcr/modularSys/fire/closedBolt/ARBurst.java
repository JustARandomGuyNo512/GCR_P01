package com.sheridan.gcr.modularSys.fire.closedBolt;

import com.sheridan.gcr.modularSys.modules.guns.ar.AR;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ARBurst extends ARFireMode{
    public static final ARBurst TWO = new ARBurst(2);
    public static final ARBurst THREE = new ARBurst(3);

    private final int burstCount;

    public ARBurst(int burstCount) {
        super("AR_BURST_" + burstCount);
        this.burstCount = burstCount;
    }

    @Override
    public void triggerClientShoot(Player player, ItemStack stack, AR gun) {
        super.triggerClientShootBurst(player, stack, gun, burstCount);
    }
}
