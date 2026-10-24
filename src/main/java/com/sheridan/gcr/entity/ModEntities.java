package com.sheridan.gcr.entity;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.entity.projectile.BulletEntity;
import com.sheridan.gcr.entity.projectile.GrenadeEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, GCR.MODID);

    public static final RegistryObject<EntityType<BulletEntity>> BULLET =
            ENTITIES.register("bullet", () ->
                    EntityType.Builder
                            .of(BulletEntity::new, MobCategory.MISC)
                            .updateInterval(1)
                            .sized(0, 0)
                            .build("bullet")
            );

    public static final RegistryObject<EntityType<GrenadeEntity>> GRENADE =
            ENTITIES.register("grenade", () ->
                    EntityType.Builder
                            .of(GrenadeEntity::new, MobCategory.MISC)
                            .updateInterval(1)
                            .build("grenade")
            );
}
