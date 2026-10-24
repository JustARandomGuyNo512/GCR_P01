package com.sheridan.gcr.client.render.fx.particles.explosion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sheridan.gcr.client.render.fx.particles.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public class FlashOption implements ParticleOptions {
    public final float scale;

    public FlashOption(float scale) {
        this.scale = scale;
    }

    public static final Codec<FlashOption> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.fieldOf("scale").forGetter(o -> o.scale)
    ).apply(inst, FlashOption::new));

    @Override
    public ParticleType<?> getType() { return ModParticles.FLASH.get(); }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeFloat(scale);
    }

    @Override
    public String writeToString() { return "gcr:flash " + scale; }
}
