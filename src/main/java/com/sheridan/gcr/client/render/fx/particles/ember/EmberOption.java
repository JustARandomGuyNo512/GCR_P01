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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EmberOption implements ParticleOptions {
    private final ParticleType<EmberOption> type;
    public int gridSize;
    public EasingType easing;
    public int lifetime;
    public float scale;


    public boolean shouldSort;
    public final List<Entry> entries = new ArrayList<>();


    public EmberOption(ParticleType<EmberOption> type, int gridSize, EasingType easing, int lifetime, float scale, boolean shouldSort) {
        this.type = type;
        this.gridSize = gridSize;
        this.easing = easing;
        this.lifetime = lifetime;
        this.scale = scale;
        this.shouldSort = shouldSort;
    }


    public EmberOption(ParticleType<EmberOption> type, int gridSize, EasingType easing, int lifetime, float scale, boolean shouldSort, List<Entry> entries) {
        this(type, gridSize, easing, lifetime, scale, shouldSort);
        this.entries.addAll(entries);
    }


    public EmberOption addEntry(float dx, float dy, float dz) {
        this.entries.add(new Entry(dx, dy, dz));
        return this;
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return this.type;
    }


    public record Entry(float dx, float dy, float dz) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.FLOAT.fieldOf("dx").forGetter(Entry::dx),
                Codec.FLOAT.fieldOf("dy").forGetter(Entry::dy),
                Codec.FLOAT.fieldOf("dz").forGetter(Entry::dz)
        ).apply(inst, Entry::new));

        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Entry::dx,
                ByteBufCodecs.FLOAT, Entry::dy,
                ByteBufCodecs.FLOAT, Entry::dz,
                Entry::new
        );
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
                Codec.FLOAT.fieldOf("scale").forGetter(o -> o.scale),
                Codec.BOOL.optionalFieldOf("should_sort", false).forGetter(o -> o.shouldSort),
                Entry.CODEC.listOf().optionalFieldOf("entries", Collections.emptyList()).forGetter(o -> o.entries)
        ).apply(inst, (gridSize, easing, lifetime, scale, shouldSort, entries) ->
                new EmberOption(typeInstance, gridSize, easing, lifetime, scale, shouldSort, entries)));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, EmberOption> createStreamCodec(ParticleType<EmberOption> typeInstance) {
        return StreamCodec.composite(
                ByteBufCodecs.INT, o -> o.gridSize,
                EasingType.STREAM_CODEC, o -> o.easing,
                ByteBufCodecs.INT, o -> o.lifetime,
                ByteBufCodecs.FLOAT, o -> o.scale,
                ByteBufCodecs.BOOL, o -> o.shouldSort,
                ByteBufCodecs.collection(ArrayList::new, Entry.STREAM_CODEC), o -> o.entries,
                (gridSize, easing, lifetime, scale, shouldSort, entries) ->
                        new EmberOption(typeInstance, gridSize, easing, lifetime, scale, shouldSort, entries)
        );
    }
}