package com.sheridan.gcr.damageTypes;

import com.sheridan.gcr.GCR;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public class ModDamageTypes {
    public static final ResourceKey<DamageType> CUSTOM_EXPLOSION = ResourceKey.create(Registries.DAMAGE_TYPE, GCR.RL("custom_explosion"));
}
