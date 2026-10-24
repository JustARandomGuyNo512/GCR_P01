package com.sheridan.gcr.data;

import com.sheridan.gcr.GCR;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Replaces NeoForge 1.21.1 attachments with a 1.20.1 Forge capability. */
@Mod.EventBusSubscriber(modid = GCR.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModData {
    public static final Capability<PlayerCommonStatus> PLAYER_STATUS =
            CapabilityManager.get(new CapabilityToken<>() {});

    private ModData() {}

    public static void register(IEventBus bus) {
        bus.addListener(ModData::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerCommonStatus.class);
    }

    public static boolean has(Entity entity) {
        return entity.getCapability(PLAYER_STATUS).isPresent();
    }

    public static PlayerCommonStatus get(Entity entity) {
        return entity.getCapability(PLAYER_STATUS).orElse(PlayerCommonStatus.EMPTY);
    }

    public static void set(Entity entity, PlayerCommonStatus value) {
        entity.getCapability(PLAYER_STATUS).ifPresent(target -> {
            target.gcrCredit = value.gcrCredit;
            target.reloading = value.reloading;
            target.dataChanged = value.dataChanged;
        });
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            StatusProvider provider = new StatusProvider();
            event.addCapability(new ResourceLocation(GCR.MODID, "player_status"), provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void copyOnRespawn(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(PLAYER_STATUS).ifPresent(oldStatus -> {
            event.getEntity().getCapability(PLAYER_STATUS).ifPresent(newStatus -> {
                newStatus.copyFrom(oldStatus);
            });
        });
        event.getOriginal().invalidateCaps();
    }

    private static final class StatusProvider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerCommonStatus status = new PlayerCommonStatus();
        private final LazyOptional<PlayerCommonStatus> optional = LazyOptional.of(() -> status);

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == PLAYER_STATUS ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            status.writeData(tag);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            status.loadData(nbt);
        }

        public void invalidate() {
            optional.invalidate();
        }
    }
}
