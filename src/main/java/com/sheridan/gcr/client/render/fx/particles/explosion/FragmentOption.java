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

public class FragmentOption implements ParticleOptions {
    // 配置项
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

    @Override
    public @NotNull ParticleType<?> getType() {
        return ModParticles.FRAGMENT.get();
    }

    // 1.21.1 Codec 序列化（用于数据包和存档）
    public static final MapCodec<FragmentOption> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.fieldOf("radius").forGetter(o -> o.radius),
            Codec.INT.fieldOf("count").forGetter(o -> o.count),
            Codec.FLOAT.fieldOf("speed").forGetter(o -> o.speed),
            Codec.INT.fieldOf("color").forGetter(o -> o.color)
    ).apply(inst, FragmentOption::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FragmentOption> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, o -> o.radius,
                    ByteBufCodecs.INT, o -> o.count,
                    ByteBufCodecs.FLOAT, o -> o.speed,
                    ByteBufCodecs.INT, o -> o.color,
                    FragmentOption::new
            );

}
