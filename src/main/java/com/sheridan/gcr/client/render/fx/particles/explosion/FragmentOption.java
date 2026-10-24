package com.sheridan.gcr.client.render.fx.particles.explosion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sheridan.gcr.client.render.fx.particles.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public class FragmentOption implements ParticleOptions {
    public final float radius;
    public final int count;
    public final float speed;
    public final int color;

    public FragmentOption(float radius, int count, float speed, int color) {
        this.radius = radius;
        this.count = count;
        this.speed = speed;
        this.color = color;
    }

    public static final Codec<FragmentOption> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.fieldOf("radius").forGetter(o -> o.radius),
            Codec.INT.fieldOf("count").forGetter(o -> o.count),
            Codec.FLOAT.fieldOf("speed").forGetter(o -> o.speed),
            Codec.INT.fieldOf("color").forGetter(o -> o.color)
    ).apply(inst, FragmentOption::new));

    @Override
    public ParticleType<?> getType() { return ModParticles.FRAGMENT.get(); }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeFloat(radius);
        buf.writeInt(count);
        buf.writeFloat(speed);
        buf.writeInt(color);
    }

    @Override
    public String writeToString() { return "gcr:fragment " + radius + " " + count + " " + speed + " " + color; }
}
