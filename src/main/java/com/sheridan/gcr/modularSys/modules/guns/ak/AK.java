package com.sheridan.gcr.modularSys.modules.guns.ak;

import com.sheridan.gcr.client.recoil.RecoilData;
import com.sheridan.gcr.items.DisplayData;
import com.sheridan.gcr.modularSys.fire.IFireMode;
import com.sheridan.gcr.modularSys.modules.IAmmoSource;
import com.sheridan.gcr.modularSys.modules.gunProperties.impl.BaseProperties;
import com.sheridan.gcr.modularSys.modules.guns.SlottedGunMainPart;
import com.sheridan.gcr.modularSys.modules.views.AKView;
import com.sheridan.gcr.modularSys.modules.views.IGunView;
import com.sheridan.gcr.modularSys.task.IGunTask;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class AK  extends SlottedGunMainPart implements AKView {
    public AK(ResourceLocation id, ResourceLocation pivotMapPath, BaseProperties baseDataModule, DisplayData displayData, RecoilData recoilData, List<IFireMode<?>> fireModes) {
        super(id, pivotMapPath, baseDataModule, displayData, recoilData, fireModes);
    }

    @Override
    public void reload(ItemStack itemStack, Player player) {
        IAmmoSource mag = getMagAttachment(itemStack);
        if (mag != null) {
            CompoundTag states = getAmmoSourceTag(itemStack);
            int ammoLeft = mag.getAmmoLeft(states);
            mag.setAmmoLeft(mag.getMaxCapacity(), states);
            if (ammoLeft < 1) {
                setGunAmmoLeft(itemStack, 1);
                mag.setAmmoLeft(mag.getAmmoLeft(states) - 1, states);
            }
        } else {
            setGunAmmoLeft(itemStack, 1);
        }
    }

    @Override
    protected IGunTask<?> getReloadTask(ItemStack itemStack) {
        return null;
    }

    @Override
    protected IGunTask<?> getRemoveStuckTask(ItemStack itemStack) {
        return null;
    }
}
