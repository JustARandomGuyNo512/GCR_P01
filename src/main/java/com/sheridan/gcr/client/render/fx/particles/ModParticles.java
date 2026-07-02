package com.sheridan.gcr.client.render.fx.particles;

import com.mojang.serialization.MapCodec;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.render.fx.particles.ember.EmberOption;
import com.sheridan.gcr.client.render.fx.particles.explosion.FlashOption;
import com.sheridan.gcr.client.render.fx.particles.explosion.FragmentOption;
import com.sheridan.gcr.client.render.fx.particles.explosion.SparkOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, GCR.MODID);

    public static final DeferredHolder<ParticleType<?>, ParticleType<FlashOption>> FLASH =
            PARTICLE_TYPES.register("flash", () -> new ParticleType<>(true) {
                @Override
                public @NotNull MapCodec<FlashOption> codec() {
                    return FlashOption.CODEC;
                }
                @Override
                public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, FlashOption> streamCodec() {
                    return FlashOption.STREAM_CODEC;
                }
            });

    public static final DeferredHolder<ParticleType<?>, ParticleType<FragmentOption>> FRAGMENT =
            PARTICLE_TYPES.register("fragment", () -> new ParticleType<>(false) {
                @Override
                public @NotNull MapCodec<FragmentOption> codec() {
                    return FragmentOption.CODEC;
                }

                @Override
                public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, FragmentOption> streamCodec() {
                    return FragmentOption.STREAM_CODEC;
                }
            });

    public static final DeferredHolder<ParticleType<?>, ParticleType<SparkOption>> SPARK =
            PARTICLE_TYPES.register("spark", () -> new ParticleType<>(false) {
                @Override
                public @NotNull MapCodec<SparkOption> codec() {
                    return SparkOption.CODEC;
                }

                @Override
                public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, SparkOption> streamCodec() {
                    return SparkOption.STREAM_CODEC;
                }
            });

    public static final DeferredHolder<ParticleType<?>, ParticleType<EmberOption>> HEAT_SMOKE = registerEmber("heat_smoke");


    private static DeferredHolder<ParticleType<?>, ParticleType<EmberOption>> registerEmber(String name) {
        return PARTICLE_TYPES.register(name, () -> new ParticleType<>(false) {
            private final MapCodec<EmberOption> codec = EmberOption.createCodec((ParticleType<EmberOption>) this);
            private final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, EmberOption> streamCodec = EmberOption.createStreamCodec((ParticleType<EmberOption>) this);

            @Override
            public @NotNull MapCodec<EmberOption> codec() {
                return this.codec;
            }

            @Override
            public @NotNull StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, EmberOption> streamCodec() {
                return this.streamCodec;
            }
        });
    }
}