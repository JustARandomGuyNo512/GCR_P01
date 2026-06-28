package com.sheridan.gcr.modularSys.task;

import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;


public interface IGunTask<T extends IGun> {

    enum TaskType {
        RELOAD,
        SWITCH_FIRE_MODE,
        OTHER,
        SWITCH_USING_SIGHT,
        REMOVE_STUCK,
        CHECKING,
    }

    void onTick(Player player);

    boolean isCompleted();

    default boolean equals(IGunTask<?> other) {
        if (this == other) {
            return true;
        }
        if (this.getGun() != other.getGun()) {
            return false;
        }
        String currID = this.getGun().getIdentityID(this.getStack());
        String newID = other.getGun().getIdentityID(other.getStack());
        if (!Objects.equals(currID, newID)) {
            return false;
        }
        if (this.getType() != other.getType() || this.getPriority() != other.getPriority()) {
            return false;
        }
        return this.getClass() == other.getClass();
    }

    ItemStack getStack();

    T getGun();

    void start();

    float getProgress();

    void onCancel();

    void onComplete();

    TaskType getType();

    int getPriority();

    default int getCustomVariable(String variableName) {
        return -1;
    }

    default boolean blockShoot() {
        return true;
    }

    default boolean blockSprinting() {
        return true;
    }
}
