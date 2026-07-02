package com.sheridan.gcr.client.render.fx.particles.ember;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public class EmberOption implements ParticleOptions {
    private final ParticleType<EmberOption> type;
    public int gridSize;
    public EasingType easing;
    public int lifetime;
    public float scale;
    public EmberOption(ParticleType<EmberOption> type, int gridSize, EasingType easing, int lifetime, float scale) {
        this.gridSize = gridSize;
        this.easing = easing;
        this.lifetime = lifetime;
        this.scale = scale;
        this.type = type;
    }
    @Override
    public @NotNull ParticleType<?> getType() {
        return this.type; // 返回注入的动态类型
    }

    public enum EasingType implements StringRepresentable {
        LINEAR("linear"), SQR("sqr"), CUBIC("cubic");

        private final String name;
        EasingType(String name) { this.name = name; }

        @Override
        public @NotNull String getSerializedName() { return this.name; }

        public static final Codec<EasingType> CODEC = StringRepresentable.fromEnum(EasingType::values);
        public static final StreamCodec<ByteBuf, EasingType> STREAM_CODEC = new StreamCodec<>() {
            public @NotNull EasingType decode(@NotNull ByteBuf buf) {
                return EasingType.values()[buf.readInt()];
            }
            public void encode(ByteBuf buf, @NotNull EasingType type) {
                buf.writeInt(type.ordinal());
            }
        };
    }


    public static MapCodec<EmberOption> createCodec(ParticleType<EmberOption> typeInstance) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.INT.fieldOf("grid_size").forGetter(o -> o.gridSize),
                EasingType.CODEC.fieldOf("easing").forGetter(o -> o.easing),
                Codec.INT.fieldOf("lifetime").forGetter(o -> o.lifetime),
                Codec.FLOAT.fieldOf("scale").forGetter(o -> o.scale)
        ).apply(inst, (gridSize, easing, lifetime, scale) -> new EmberOption(typeInstance, gridSize, easing, lifetime, scale)));
    }


    public static StreamCodec<RegistryFriendlyByteBuf, EmberOption> createStreamCodec(ParticleType<EmberOption> typeInstance) {
        return StreamCodec.composite(
                ByteBufCodecs.INT, o -> o.gridSize,
                EasingType.STREAM_CODEC, o -> o.easing,
                ByteBufCodecs.INT, o -> o.lifetime,
                ByteBufCodecs.FLOAT, o -> o.scale,
                (gridSize, easing, lifetime, scale) -> new EmberOption(typeInstance, gridSize, easing, lifetime, scale)
        );
    }
}