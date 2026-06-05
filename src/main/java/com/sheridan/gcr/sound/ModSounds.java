package com.sheridan.gcr.sound;

import com.sheridan.gcr.GCR;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> MOD_SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, GCR.MODID);
    public static final Map<String, DeferredRegister<SoundEvent>> ADDON_SOUNDS = new HashMap<>();

    public static DeferredHolder<SoundEvent, SoundEvent> AR_CHARGE_BACK = registerSound("ar_charge_back", "gcr");
    public static DeferredHolder<SoundEvent, SoundEvent> AR_CHARGE_FORWARD = registerSound("ar_charge_forward", "gcr");
    public static DeferredHolder<SoundEvent, SoundEvent> M203_CLOSE = registerSound("m203_close", "gcr");
    public static DeferredHolder<SoundEvent, SoundEvent> M203_FIRE = registerSound("m203_fire", "gcr");
    public static DeferredHolder<SoundEvent, SoundEvent> M203_OPEN = registerSound("m203_open", "gcr");
    public static DeferredHolder<SoundEvent, SoundEvent> M203_SHELL_INSERT = registerSound("m203_shell_insert", "gcr");
    public static DeferredHolder<SoundEvent, SoundEvent> M203_SHELL_OUT = registerSound("m203_shell_out", "gcr");
    public static DeferredHolder<SoundEvent, SoundEvent> M4A1_FIRE = registerSound("m4a1_fire", "gcr");
    public static DeferredHolder<SoundEvent, SoundEvent> M4A1_FIRE1 = registerSound("m4a1_fire1", "gcr");
    public static DeferredHolder<SoundEvent, SoundEvent> AR_SWITCH_FIRE_MODE = registerSound("ar_switch_fire_mode", "gcr");
    public static DeferredHolder<SoundEvent, SoundEvent> AR_ARM_WAVE = registerSound("ar_arm_wave", "gcr");
    public static DeferredHolder<SoundEvent, SoundEvent> AR_MAG_IN = registerSound("ar_mag_in", "gcr");
    public static DeferredHolder<SoundEvent, SoundEvent> AR_MAG_OUT = registerSound("ar_mag_out", "gcr");
    public static DeferredHolder<SoundEvent, SoundEvent> AR_BOLT_LOCK = registerSound("ar_bolt_lock", "gcr");
    public static DeferredHolder<SoundEvent, SoundEvent> AR_SHELL_IN = registerSound("ar_shell_in", "gcr");
    public static DeferredHolder<SoundEvent, SoundEvent> AR_STUCK_REMOVE = registerSound("ar_stuck_remove", "gcr");

    public static DeferredHolder<SoundEvent, SoundEvent> BTN = registerSound("btn", "gcr");
    public static DeferredHolder<SoundEvent, SoundEvent> GUN_STUCK = registerSound("gun_stuck", "gcr");

    private static DeferredHolder<SoundEvent, SoundEvent> registerSound(String name) {
        return registerSound(name, GCR.MODID);
    }

    public static DeferredHolder<SoundEvent, SoundEvent> registerSound(String name, String id) {
        if (GCR.MODID.equals(id)) {
            return MOD_SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(GCR.RL(GCR.MODID, name)));
        } else {
            if (!ADDON_SOUNDS.containsKey(id)) {
                ADDON_SOUNDS.put(id, DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, id));
            }
            DeferredRegister<SoundEvent> soundEventDeferredRegister = ADDON_SOUNDS.get(id);
            return soundEventDeferredRegister.register(name, () -> SoundEvent.createVariableRangeEvent(GCR.RL(id, name)));
        }
    }

    public static void sound(float vol, float pit, Player player, ResourceLocation name) {
        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(name);
        if (soundEvent != null) {
            sound(vol, pit, player, soundEvent);
        }
    }


    public static void sound(float vol, float pit, double x, double y, double z, Player player, ResourceLocation name) {
        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(name);
        if (soundEvent != null) {
            player.level().playSeededSound(player, x, y, z, soundEvent, player.getSoundSource(), vol, pit, 0);
        }
    }

    public static void sound(float vol, float pit, Player player, SoundEvent soundEvent) {
        player.level().playSeededSound(player, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), vol, pit, 0);
    }

    public static void handleRegister(IEventBus modEventBus) {
        MOD_SOUNDS.register(modEventBus);
        for (DeferredRegister<SoundEvent> soundEventDeferredRegister : ADDON_SOUNDS.values()) {
            soundEventDeferredRegister.register(modEventBus);
        }
    }
}
