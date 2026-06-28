package com.sheridan.gcr.entity.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class GrenadeEntity extends Entity {
    public GrenadeEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {

    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {

    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {

    }
}
