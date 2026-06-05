package com.sheridan.gcr.modularSys.modules.guns.ar;

import com.sheridan.gcr.client.recoil.RecoilData;
import com.sheridan.gcr.items.DisplayData;
import com.sheridan.gcr.modularSys.fire.IFireMode;
import com.sheridan.gcr.modularSys.modules.IAmmoSource;
import com.sheridan.gcr.modularSys.modules.gunProperties.impl.BaseProperties;
import com.sheridan.gcr.modularSys.modules.guns.SlottedGunMainPart;
import com.sheridan.gcr.modularSys.modules.states.Bool;
import com.sheridan.gcr.modularSys.modules.views.ARView;
import com.sheridan.gcr.modularSys.task.IGunTask;
import com.sheridan.gcr.modularSys.task.other.ARRemoveStuckTask;
import com.sheridan.gcr.modularSys.task.reload.ARReloadTask;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class AR extends SlottedGunMainPart implements ARView {
    protected Bool BOLT_LOCKED = new Bool("bolt_locked");

    public AR(ResourceLocation id, ResourceLocation pivotMapPath, BaseProperties baseDataModule, DisplayData displayData, RecoilData recoilData, List<IFireMode<?>> fireModes) {
        super(id, pivotMapPath, baseDataModule, displayData, recoilData, fireModes);
    }

    public void setBoltLocked(boolean locked, CompoundTag states) {
        BOLT_LOCKED.set(locked, states);
    }

    @Override
    public @Nullable IGunTask<?> getTask(ItemStack itemStack, IGunTask.TaskType type, Map<String, Object> args) {
        if (type == IGunTask.TaskType.RELOAD) {
            if (!shouldReload(itemStack)) {
                return null;
            }
            return new ARReloadTask(itemStack, this);
        }
        if (type == IGunTask.TaskType.REMOVE_STUCK && isStuck(itemStack)) {
            return new ARRemoveStuckTask(itemStack, this);
        }
        return super.getTask(itemStack, type, args);
    }

    protected boolean shouldReload(ItemStack itemStack) {
        IAmmoSource magAttachment = getMagAttachment(itemStack);
        if (magAttachment == null) {
            return getGunAmmoLeft(itemStack) < 1;
        } else {
            int ammoLeft = getAmmoLeft(itemStack);
            return ammoLeft < magAttachment.getMaxCapacity() + 1;
        }
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
            setBoltLocked(false, rootNodeTag(itemStack));
        } else {
            setGunAmmoLeft(itemStack, 1);
            setBoltLocked(false, rootNodeTag(itemStack));
        }
    }



    @Override
    public void removeStuck(ItemStack itemStack) {
        super.removeStuck(itemStack);
        IAmmoSource mag = getMagAttachment(itemStack);
        if (mag != null) {
            CompoundTag states = getAmmoSourceTag(itemStack);
            int ammoLeft = mag.getAmmoLeft(states);
            if (ammoLeft >= 1 && getGunAmmoLeft(itemStack) <= 0) {
                setGunAmmoLeft(itemStack, 1);
                mag.setAmmoLeft(mag.getAmmoLeft(states) - 1, states);
            }
        }
    }

    @Override
    public int getAmmoLeft(CompoundTag states) {
        return super.getAmmoLeft(states);
    }

    @Override
    public boolean boltLocked(CompoundTag states) {
        return BOLT_LOCKED.get(states);
    }

    @Override
    public AdditionalPropModifier getModifier() {
        return null;
    }
}
