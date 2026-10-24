package com.sheridan.gcr.client.render.fx.particles.ember;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.util.StringRepresentable;
import net.minecraft.network.FriendlyByteBuf;
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

    }

    public enum EasingType implements StringRepresentable {
        LINEAR("linear"), SQR("sqr"), CUBIC("cubic");

        private final String name;
        EasingType(String name) { this.name = name; }

        @Override
        public @NotNull String getSerializedName() { return this.name; }

        public static final Codec<EasingType> CODEC = StringRepresentable.fromEnum(EasingType::values);
    }

    public static Codec<EmberOption> createCodec(ParticleType<EmberOption> typeInstance) {
        return RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.fieldOf("grid_size").forGetter(o -> o.gridSize),
                EasingType.CODEC.fieldOf("easing").forGetter(o -> o.easing),
                Codec.INT.fieldOf("lifetime").forGetter(o -> o.lifetime),
                Codec.FLOAT.fieldOf("scale").forGetter(o -> o.scale),
                Codec.BOOL.optionalFieldOf("should_sort", false).forGetter(o -> o.shouldSort),
                Entry.CODEC.listOf().optionalFieldOf("entries", Collections.emptyList()).forGetter(o -> o.entries)
        ).apply(inst, (gridSize, easing, lifetime, scale, shouldSort, entries) ->
                new EmberOption(typeInstance, gridSize, easing, lifetime, scale, shouldSort, entries)));
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeVarInt(gridSize);
        buf.writeVarInt(easing.ordinal());
        buf.writeVarInt(lifetime);
        buf.writeFloat(scale);
        buf.writeBoolean(shouldSort);
        buf.writeVarInt(entries.size());
        for (Entry e : entries) {
            buf.writeFloat(e.dx());
            buf.writeFloat(e.dy());
            buf.writeFloat(e.dz());
        }
    }

    public static EmberOption fromNetwork(ParticleType<EmberOption> type, FriendlyByteBuf buf) {
        int gridSize = buf.readVarInt();
        int index = buf.readVarInt();
        EasingType[] modes = EasingType.values();
        if (index < 0 || index >= modes.length) throw new IllegalArgumentException("Invalid easing " + index);
        int lifetime = buf.readVarInt();
        float scale = buf.readFloat();
        boolean shouldSort = buf.readBoolean();
        int count = buf.readVarInt();
        if (count < 0 || count > 8192) throw new IllegalArgumentException("Invalid entry count: " + count);
        EmberOption o = new EmberOption(type, gridSize, modes[index], lifetime, scale, shouldSort);
        for (int i = 0; i < count; ++i) o.addEntry(buf.readFloat(), buf.readFloat(), buf.readFloat());
        return o;
    }

    @Override
    public String writeToString() {
        return "gcr:heat_smoke " + gridSize + " " + easing.getSerializedName() + " " + lifetime + " " + scale + " " + shouldSort;
    }
}
