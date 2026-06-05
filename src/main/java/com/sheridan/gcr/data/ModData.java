package com.sheridan.gcr.data;

import com.sheridan.gcr.GCR;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;


public class ModData {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, GCR.MODID);

    // Serialization via INBTSerializable
    public static final Supplier<AttachmentType<PlayerCommonStatus>> PLAYER_STATUS =
            ATTACHMENT_TYPES.register("player_status", () ->
                    AttachmentType
                            .builder(PlayerCommonStatus::new)
                            .serialize(PlayerStatusSerializer.INSTANCE)
                            .copyOnDeath()
                            .build()
            );

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }
}
