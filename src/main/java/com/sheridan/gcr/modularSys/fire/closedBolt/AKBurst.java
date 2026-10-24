package com.sheridan.gcr.modularSys.fire.closedBolt;

import com.sheridan.gcr.modularSys.modules.guns.ak.AK;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class AKBurst extends AKFireMode{
    public static final AKBurst TWO = new AKBurst(2);
    public static final AKBurst THREE = new AKBurst(3);

    private final int burstCount;

    public AKBurst(int burstCount) {
        super("AK_BURST_" + burstCount);
        this.burstCount = burstCount;
    }

    @Override
    public void triggerClientShoot(Player player, ItemStack stack, AK gun) {
        super.triggerClientShootBurst(player, stack, gun, burstCount);
    }
}
