package com.sheridan.gcr.client.render.fx.particles.explosion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sheridan.gcr.client.render.fx.particles.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public class SparkOption implements ParticleOptions {
    public final float radius;
    public final int count;
    public final float speed;
    public final int startColor;
    public final int fadeColor;

    public SparkOption(float radius, int count, float speed, int startColor, int fadeColor) {
        this.radius = radius;
        this.count = count;
        this.speed = speed;
        this.startColor = startColor;
        this.fadeColor = fadeColor;
    }

    public static final Codec<SparkOption> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.fieldOf("radius").forGetter(o -> o.radius),
            Codec.INT.fieldOf("count").forGetter(o -> o.count),
            Codec.FLOAT.fieldOf("speed").forGetter(o -> o.speed),
            Codec.INT.fieldOf("startColor").forGetter(o -> o.startColor),
            Codec.INT.fieldOf("fadeColor").forGetter(o -> o.fadeColor)
    ).apply(inst, SparkOption::new));

    @Override
    public ParticleType<?> getType() { return ModParticles.SPARK.get(); }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeFloat(radius);
        buf.writeInt(count);
        buf.writeFloat(speed);
        buf.writeInt(startColor);
        buf.writeInt(fadeColor);
    }

    @Override
    public String writeToString() { return "gcr:spark " + radius + " " + count + " " + speed + " " + startColor + " " + fadeColor; }
}
