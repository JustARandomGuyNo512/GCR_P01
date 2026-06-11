package com.sheridan.gcr.modularSys.fire.closedBolt;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.modularSys.fire.IFireMode;
import com.sheridan.gcr.modularSys.modules.guns.ar.AR;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ARSemi extends ARFireMode{
    public static final ARSemi SEMI = new ARSemi();

    public ARSemi() {
        super("AR_SEMI");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void triggerClientShoot(Player player, ItemStack stack, AR gun) {
        if (useAmmoClient(player, stack, gun)) {
            boolean stuck = gun.isStuck(stack);
            gun.clientShoot(player, stack);
            sendPacket(stuck);
            IFireMode.stopFire();
            Client.WEAPON_STATUS.setMuzzleFlashRadius((float) (4 + Math.random()));
            Client.getGunRenderer().dispatchAnimationEvent(EventType.SHOOT);
        }
    }
}
