package com.sheridan.gcr.modularSys.fire.closedBolt;

import com.sheridan.gcr.modularSys.fire.AssaultRifeFireMode;
import com.sheridan.gcr.modularSys.modules.IAmmoSource;
import com.sheridan.gcr.modularSys.modules.guns.ar.AR;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class ARFireMode extends AssaultRifeFireMode<AR> {
    public ARFireMode(String name) {
        super(name);
    }

    @Override
    public int modifyRpm(int baseRpm) {
        return baseRpm;
    }

    protected void useMagAmmo(ItemStack itemStack, AR gun, Player player, boolean handleStuck) {
        IAmmoSource mag = gun.getMagAttachment(itemStack);
        CompoundTag gunStates = gun.rootNodeTag(itemStack);
        if (mag == null) {
            return;
        }
        CompoundTag magStates = gun.getAmmoSourceTag(itemStack);
        if (mag.getAmmoLeft(magStates) <= 0) {
            gun.setBoltLocked(true, gunStates);
            return;
        }
        if (handleStuck) {
            gun.setStuck(true, gunStates);
            return;
        }
        int magAmmoLeft = mag.getAmmoLeft(magStates);
        gun.setGunAmmoLeft(itemStack, 1);
        mag.setAmmoLeft(magAmmoLeft - 1, magStates);
    }

    @Override
    public Class<AR> getGunClass() {
        return AR.class;
    }
}
