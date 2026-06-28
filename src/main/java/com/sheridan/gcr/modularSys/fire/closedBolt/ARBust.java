package com.sheridan.gcr.modularSys.fire.closedBolt;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.modularSys.fire.IFireMode;
import com.sheridan.gcr.modularSys.modules.guns.ar.AR;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ARBust extends ARFireMode{
    public static final ARBust TWO = new ARBust(2);
    public static final ARBust THREE = new ARBust(3);

    private final int burstCount;

    public ARBust(int burstCount) {
        super("AR_BURST_" + burstCount);
        this.burstCount = burstCount;
    }

    @Override
    public void triggerClientShoot(Player player, ItemStack stack, AR gun) {
        if (useAmmoClient(player, stack, gun)) {
            boolean stuck = gun.isStuck(stack);
            gun.clientShoot(player, stack);
            sendPacket(stuck);
            if (stuck) {
                IFireMode.stopFire();
            } else {
                Client.WEAPON_STATUS.fireCount ++;
                if (Client.WEAPON_STATUS.fireCount >= burstCount) {
                    IFireMode.stopFire();
                }
            }
            Client.WEAPON_STATUS.onShoot();
            Client.getGunRenderer().dispatchAnimationEvent(EventType.SHOOT);
        }
    }
}
