package com.sheridan.gcr.events;

import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

public class LivingFireEvent extends Event {
    private LivingEntity entity;
    private IGun gun;
    private String fireModuleId;
    private String gunId;
    private ItemStack itemStack;
    private int shooterLatency;

    public LivingFireEvent(LivingEntity entity, IGun gun, String fireModuleId, String gunId, ItemStack itemStack, int shooterLatency) {
        this.entity = entity;
        this.gun = gun;
        this.fireModuleId = fireModuleId;
        this.gunId = gunId;
        this.itemStack = itemStack;
    }

    public int getShooterLatency() {
        return shooterLatency;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public IGun getGun() {
        return gun;
    }

    public String getFireModuleId() {
        return fireModuleId;
    }

    public String getGunId() {
        return gunId;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
