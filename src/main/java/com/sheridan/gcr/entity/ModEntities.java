package com.sheridan.gcr.entity;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.entity.projectile.BulletEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, GCR.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BulletEntity>> BULLET =
            ENTITIES.register("bullet", () ->
                    EntityType.Builder
                            .of(BulletEntity::new, MobCategory.MISC)
                            .updateInterval(1)
                            .sized(0, 0)
                            .build("bullet")
            );
}
