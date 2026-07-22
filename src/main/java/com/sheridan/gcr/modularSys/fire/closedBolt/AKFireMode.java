package com.sheridan.gcr.modularSys.fire.closedBolt;

import com.sheridan.gcr.modularSys.fire.AssaultRifeFireMode;
import com.sheridan.gcr.modularSys.modules.IAmmoSource;
import com.sheridan.gcr.modularSys.modules.guns.ak.AK;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class AKFireMode extends AssaultRifeFireMode<AK> {
    @Override
    public int modifyRpm(int baseRpm) {
        return baseRpm;
    }

    public AKFireMode(String name) {
        super(name);
    }

    @Override
    protected void useMagAmmo(ItemStack itemStack, AK gun, Player player, boolean handleStuck) {
        IAmmoSource mag = gun.getMagAttachment(itemStack);
        CompoundTag gunStates = gun.rootNodeTag(itemStack);
        if (mag == null) {
            return;
        }
        CompoundTag magStates = gun.getAmmoSourceTag(itemStack);
        if (mag.getAmmoLeft(magStates) <= 0) {
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
    public Class<AK> getGunClass() {
        return AK.class;
    }
}
