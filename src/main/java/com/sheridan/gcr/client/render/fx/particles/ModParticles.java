package com.sheridan.gcr.client.render.fx.particles;

import com.mojang.serialization.MapCodec;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.render.fx.particles.explosion.FlashOption;
import com.sheridan.gcr.client.render.fx.particles.explosion.FragmentOption;
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
            PARTICLE_TYPES.register("fragment", () -> new ParticleType<FragmentOption>(false) {
                @Override
                public @NotNull MapCodec<FragmentOption> codec() {
                    return FragmentOption.CODEC;
                }

                @Override
                public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, FragmentOption> streamCodec() {
                    return FragmentOption.STREAM_CODEC;
                }
            });
}