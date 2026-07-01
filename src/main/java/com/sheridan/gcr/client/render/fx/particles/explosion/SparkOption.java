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
import org.jetbrains.annotations.NotNull;

public class SparkOption implements ParticleOptions {
    // 配置项
    public final float radius;
    public final int count;
    public final float speed;
    public int startColor;
    public int fadeColor;
    public SparkOption(float radius, int count, float speed, int startColor, int fadeColor) {
        this.radius = radius;
        this.count = count;
        this.speed = speed;
        this.startColor = startColor;
        this.fadeColor = fadeColor;
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return ModParticles.SPARK.get();
    }

    public static final MapCodec<SparkOption> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.fieldOf("radius").forGetter(o -> o.radius),
            Codec.INT.fieldOf("count").forGetter(o -> o.count),
            Codec.FLOAT.fieldOf("speed").forGetter(o -> o.speed),
            Codec.INT.fieldOf("s").forGetter(o -> o.startColor),
            Codec.INT.fieldOf("f").forGetter(o -> o.fadeColor)
    ).apply(inst, SparkOption::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SparkOption> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, o -> o.radius,
                    ByteBufCodecs.INT, o -> o.count,
                    ByteBufCodecs.FLOAT, o -> o.speed,
                    ByteBufCodecs.INT, o -> o.startColor,
                    ByteBufCodecs.INT, o -> o.fadeColor,
                    SparkOption::new
            );

}
