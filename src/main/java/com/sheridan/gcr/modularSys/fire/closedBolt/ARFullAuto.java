package com.sheridan.gcr.modularSys.fire.closedBolt;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.modularSys.fire.IFireMode;
import com.sheridan.gcr.modularSys.modules.guns.ar.AR;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ARFullAuto extends ARFireMode {
    public static final ARFireMode FULL_AUTO = new ARFullAuto();

    public ARFullAuto() {
        super("AR_FULL_AUTO");
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
                Client.WEAPON_STATUS.fireCount++;
            }
            Client.WEAPON_STATUS.setMuzzleFlashRadius((float) (4 + Math.random()));
            Client.getGunRenderer().dispatchAnimationEvent(EventType.SHOOT);
        }
    }

}
