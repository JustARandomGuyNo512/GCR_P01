package com.sheridan.gcr.modularSys.task;

import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;


public abstract class GunTask<T extends IGun> implements IGunTask<T>{
    protected final ItemStack itemStack;
    protected final T gun;
    protected int tick = 0;
    protected int length;

    public GunTask(ItemStack itemStack, T gun, int lengthInTicks) {
        this.gun = gun;
        this.itemStack = itemStack;
        this.length = lengthInTicks;
    }

    public GunTask(ItemStack itemStack, T gun) {
        this.gun = gun;
        this.itemStack = itemStack;
        this.length = 1;
    }

    @Override
    public void onTick(Player player) {
        if (tick >= length) {
            return;
        }
        tick++;
    }

    @Override
    public boolean isCompleted() {
        return tick >= length;
    }

    @Override
    public ItemStack getStack() {
        return itemStack;
    }

    @Override
    public float getProgress() {
        return (float)tick / length;
    }

    @Override
    public T getGun() {
        return gun;
    }
}
