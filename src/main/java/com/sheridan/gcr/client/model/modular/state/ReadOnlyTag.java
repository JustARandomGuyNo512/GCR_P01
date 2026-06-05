package com.sheridan.gcr.client.model.modular.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ReadOnlyTag extends CompoundTag {
    private static final CompoundTag EMPTY_REF = new CompoundTag();
    public static final ReadOnlyTag TAG = new ReadOnlyTag(EMPTY_REF);

    private CompoundTag ref;
    public ReadOnlyTag(CompoundTag tag) {
        super(Map.of());
        this.ref = tag;
    }

    public void setRef(CompoundTag ref) {
        this.ref = ref;
    }

    public static void clear() {
        TAG.ref = EMPTY_REF;
    }

    public static ReadOnlyTag of(CompoundTag tag) {
        if (tag == null) {
            tag = EMPTY_REF;
        }
        TAG.setRef(tag);
        return TAG;
    }

    @Override
    public @NotNull Set<String> getAllKeys() {
        return ref.getAllKeys();
    }

    @Override
    public byte getId() {
        return ref.getId();
    }

    @Override
    public @NotNull TagType<CompoundTag> getType() {
        return ref.getType();
    }

    @Override
    public @NotNull UUID getUUID(@NotNull String key) {
        return ref.getUUID(key);
    }

    @Override
    public @Nullable Tag get(@NotNull String key) {
        return ref.get(key);
    }

    @Override
    public byte getTagType(@NotNull String key) {
        return ref.getTagType(key);
    }

    @Override
    public byte getByte(@NotNull String key) {
        return ref.getByte(key);
    }

    @Override
    public short getShort(@NotNull String key) {
        return ref.getShort(key);
    }

    @Override
    public int getInt(@NotNull String key) {
        return ref.getInt(key);
    }

    @Override
    public long getLong(@NotNull String key) {
        return ref.getLong(key);
    }

    @Override
    public float getFloat(@NotNull String key) {
        return ref.getFloat(key);
    }

    @Override
    public @NotNull String getString(@NotNull String key) {
        return ref.getString(key);
    }

    @Override
    public byte @NotNull [] getByteArray(@NotNull String key) {
        return ref.getByteArray(key);
    }

    @Override
    public double getDouble(@NotNull String key) {
        return ref.getDouble(key);
    }

    @Override
    public int @NotNull [] getIntArray(@NotNull String key) {
        return ref.getIntArray(key);
    }

    @Override
    public long @NotNull [] getLongArray(@NotNull String key) {
        return ref.getLongArray(key);
    }

    @Override
    public @NotNull CompoundTag getCompound(@NotNull String key) {
        return ref.getCompound(key);
    }

    @Override
    public @NotNull ListTag getList(@NotNull String key, int tagType) {
        return ref.getList(key, tagType);
    }

    @Override
    public boolean getBoolean(@NotNull String key) {
        return ref.getBoolean(key);
    }

    @Override
    public @Nullable Tag put(@NotNull String key, @NotNull Tag value) {
        return value;
    }

    @Override
    public void putByte(@NotNull String key, byte value) {
        return;
    }

    @Override
    public void putShort(@NotNull String key, short value) {
        return;
    }

    @Override
    public void putInt(@NotNull String key, int value) {
        return;
    }

    @Override
    public void putLong(@NotNull String key, long value) {
        return;
    }

    @Override
    public void putUUID(@NotNull String key, @NotNull UUID value) {
        return;
    }

    @Override
    public void putFloat(@NotNull String key, float value) {
        return;
    }

    @Override
    public void putDouble(@NotNull String key, double value) {
        return;
    }

    @Override
    public void putString(@NotNull String key, @NotNull String value) {
        return;
    }

    @Override
    public void putByteArray(@NotNull String key, byte @NotNull [] value) {
        return;
    }

    @Override
    public void putByteArray(@NotNull String key,@NotNull List<Byte> value) {
        return;
    }

    @Override
    public void putIntArray(@NotNull String key, int @NotNull [] value) {
        return;
    }

    @Override
    public void putIntArray(@NotNull String key, @NotNull List<Integer> value) {
        return;
    }

    @Override
    public void putLongArray(@NotNull String key, long @NotNull [] value) {
        return;
    }

    @Override
    public void putLongArray(@NotNull String key, @NotNull List<Long> value) {
        return;
    }

    @Override
    public void putBoolean(@NotNull String key, boolean value) {
        return;
    }

    @Override
    public boolean contains(@NotNull String key) {
        return ref.contains(key);
    }

    @Override
    public boolean contains(@NotNull String key, int tagType) {
        return ref.contains(key, tagType);
    }

    @Override
    public boolean hasUUID(@NotNull String key) {
        return ref.hasUUID(key);
    }

    @Override
    public int hashCode() {
        return ref.hashCode();
    }
}
