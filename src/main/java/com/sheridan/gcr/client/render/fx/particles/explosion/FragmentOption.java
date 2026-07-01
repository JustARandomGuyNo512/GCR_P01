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
import org.joml.Vector3f;

public class FragmentOption implements ParticleOptions {
    // 配置项
    public final float radius;
    public final int count;
    public final float speed;
    public final float r;
    public final float g;
    public final float b;
    public FragmentOption(float radius, int count, float speed, float r, float g, float b) {
        this.radius = radius;
        this.count = count;
        this.speed = speed;
        this.r = r;
        this.g = g;
        this.b = b;
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
            Codec.FLOAT.fieldOf("r").forGetter(o -> o.r),
            Codec.FLOAT.fieldOf("g").forGetter(o -> o.g),
            Codec.FLOAT.fieldOf("b").forGetter(o -> o.b)
    ).apply(inst, FragmentOption::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FragmentOption> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, o -> o.radius,
                    ByteBufCodecs.INT, o -> o.count,
                    ByteBufCodecs.FLOAT, o -> o.speed,
                    ByteBufCodecs.VECTOR3F, o -> new Vector3f(o.r, o.g, o.b),
                    (r, c, s, col) -> new FragmentOption(r, c, s, col.x, col.y, col.z)
            );

}
