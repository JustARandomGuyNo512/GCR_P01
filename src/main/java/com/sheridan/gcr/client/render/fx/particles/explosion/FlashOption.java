package com.sheridan.gcr.client.render.fx.particles.explosion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sheridan.gcr.client.render.fx.particles.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class FlashOption implements ParticleOptions {

    public static final MapCodec<FlashOption> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.fieldOf("scale").forGetter(o -> o.scale)
    ).apply(inst, FlashOption::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FlashOption> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, o -> o.scale,
                    FlashOption::new
            );

    public final float scale;     // RGB int

    public FlashOption(float scale) {
        this.scale = scale;
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.FLASH.get();
    }
}
