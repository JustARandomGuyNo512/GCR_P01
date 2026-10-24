package com.sheridan.gcr.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerStatusSerializer implements IAttachmentSerializer<CompoundTag, PlayerCommonStatus> {
    public static final PlayerStatusSerializer INSTANCE = new PlayerStatusSerializer();

    @Override
    public @NotNull PlayerCommonStatus read(
            @NotNull IAttachmentHolder iAttachmentHolder, @NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        PlayerCommonStatus playerStatus = new PlayerCommonStatus();
        playerStatus.loadData(tag);
        return playerStatus;
    }

    @Override
    public @Nullable CompoundTag write(
            @NotNull PlayerCommonStatus playerStatus, HolderLookup.@NotNull Provider provider) {
        CompoundTag tag = new CompoundTag();
        playerStatus.writeData(tag);
        return tag;
    }
}
