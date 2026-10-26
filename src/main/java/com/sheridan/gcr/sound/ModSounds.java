package com.sheridan.gcr.sound;

import com.sheridan.gcr.GCR;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> MOD_SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, GCR.MODID);
    public static final Map<String, DeferredRegister<SoundEvent>> ADDON_SOUNDS = new HashMap<>();

    public static RegistryObject<SoundEvent> AR_CHARGE_BACK = registerSound("ar_charge_back", "gcr");
    public static RegistryObject<SoundEvent> AR_CHARGE_FORWARD = registerSound("ar_charge_forward", "gcr");
    public static RegistryObject<SoundEvent> M203_CLOSE = registerSound("m203_close", "gcr");
    public static RegistryObject<SoundEvent> M203_FIRE = registerSound("m203_fire", "gcr");
    public static RegistryObject<SoundEvent> GP25_FIRE = registerSound("gp25_fire", "gcr");
    public static RegistryObject<SoundEvent> M203_OPEN = registerSound("m203_open", "gcr");
    public static RegistryObject<SoundEvent> M203_SHELL_INSERT = registerSound("m203_shell_insert", "gcr");
    public static RegistryObject<SoundEvent> M203_SHELL_OUT = registerSound("m203_shell_out", "gcr");
    public static RegistryObject<SoundEvent> M4A1_FIRE = registerSound("m4a1_fire", "gcr");
    public static RegistryObject<SoundEvent> M4A1_FIRE_SUPPRESSED = registerSound("m4a1_fire_suppressed", "gcr");
    public static RegistryObject<SoundEvent> AR_SWITCH_FIRE_MODE = registerSound("ar_switch_fire_mode", "gcr");
    public static RegistryObject<SoundEvent> AR_ARM_WAVE = registerSound("ar_arm_wave", "gcr");
    public static RegistryObject<SoundEvent> AR_MAG_IN = registerSound("ar_mag_in", "gcr");
    public static RegistryObject<SoundEvent> AR_MAG_OUT = registerSound("ar_mag_out", "gcr");
    public static RegistryObject<SoundEvent> AR_BOLT_LOCK = registerSound("ar_bolt_lock", "gcr");
    public static RegistryObject<SoundEvent> AR_SHELL_IN = registerSound("ar_shell_in", "gcr");
    public static RegistryObject<SoundEvent> AR_STUCK_REMOVE = registerSound("ar_stuck_remove", "gcr");
    public static RegistryObject<SoundEvent> GP25_LOAD_GRENADE = registerSound("gp25_load_grenade", "gcr");

    public static RegistryObject<SoundEvent> AK74M_BOLT_BACK = registerSound("ak74m_boltback", "gcr");
    public static RegistryObject<SoundEvent> AK74M_BOLT_FORWARD = registerSound("ak74m_boltforward", "gcr");
    public static RegistryObject<SoundEvent> AK74M_MAG_IN = registerSound("ak74m_magin", "gcr");
    public static RegistryObject<SoundEvent> AK74M_MAG_OUT = registerSound("ak74m_magout", "gcr");
    public static RegistryObject<SoundEvent> AK74M_EMPTY_MAG_IN = registerSound("ak74m_empty_magin", "gcr");
    public static RegistryObject<SoundEvent> AK74M_EMPTY_MAG_OUT = registerSound("ak74m_empty_magout", "gcr");

    public static RegistryObject<SoundEvent> AK74M_FIRE = registerSound("ak74m_fire", "gcr");
    public static RegistryObject<SoundEvent> AK74M_FIRE_SUPPRESSED = registerSound("ak74m_fire_suppressed", "gcr");


    public static final RegistryObject<SoundEvent> MAG_ROUND_LOAD =
            registerSound("mag_round_load", "gcr");
    public static final RegistryObject<SoundEvent> MAG_ROUND_UNLOAD =
            registerSound("mag_round_unload", "gcr");

    public static RegistryObject<SoundEvent> BTN = registerSound("btn", "gcr");
    public static RegistryObject<SoundEvent> GUN_STUCK = registerSound("gun_stuck", "gcr");

    public static RegistryObject<SoundEvent> GENERIC_CLEAR_SHELL_1 = registerSound("generic_clearshell1", "gcr");

    public static RegistryObject<SoundEvent> GENERIC_GET_MAG_1 = registerSound("generic_getmag1", "gcr");
    public static RegistryObject<SoundEvent> GENERIC_GET_MAG_2 = registerSound("generic_getmag2", "gcr");
    public static RegistryObject<SoundEvent> GENERIC_GET_MAG_3 = registerSound("generic_getmag3", "gcr");

    public static RegistryObject<SoundEvent> M203NEW_CLOSE = registerSound("m203new_close", "gcr");
    public static RegistryObject<SoundEvent> M203NEW_OPEN = registerSound("m203new_open", "gcr");
    public static RegistryObject<SoundEvent> M203NEW_SHELL_IN = registerSound("m203new_shellin", "gcr");
    public static RegistryObject<SoundEvent> M203NEW_SHELL_OUT = registerSound("m203new_shellout", "gcr");


    public static RegistryObject<SoundEvent> AK24_FIRE = registerSound("ak24_fire", "gcr");
    public static RegistryObject<SoundEvent> AK24_FIRE_SUPPRESSED = registerSound("ak24_fire_suppressed", "gcr");

    public static RegistryObject<SoundEvent> MCX_SPEAR_FIRE = registerSound("mcx_spear_fire", "gcr");
    public static RegistryObject<SoundEvent> MCX_SPEAR_FIRE_SUPPRESSED = registerSound("mcx_spear_fire_suppressed", "gcr");

    public static RegistryObject<SoundEvent> AKMN_FIRE = registerSound("akmn_fire", "gcr");
    public static RegistryObject<SoundEvent> AKMN_FIRE_SUPPRESSED = registerSound("akmn_fire_suppressed", "gcr");

    // M4NEW
    public static RegistryObject<SoundEvent> M4NEW_FIRE_V2 = registerSound("m4new_fire_v2", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_FIRE_SUP_V2 = registerSound("m4new_fire_sup_v2", "gcr");

    public static RegistryObject<SoundEvent> M4NEW_CHARGE_BACK = registerSound("m4new_chargeback", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_CHARGE_FORWARD = registerSound("m4new_chargeforward", "gcr");

    public static RegistryObject<SoundEvent> M4NEW_BOLT_RELEASE_1 = registerSound("m4new_boltrelease1", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_BOLT_RELEASE_2 = registerSound("m4new_boltrelease2", "gcr");

    public static RegistryObject<SoundEvent> M4NEW_CHECK_BACK = registerSound("m4new_checkback", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_CHECK_FORWARD = registerSound("m4new_checkforward", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_CLEAR_BACK = registerSound("m4new_clearback", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_CLEAR_FORWARD = registerSound("m4new_clearforward", "gcr");

    public static RegistryObject<SoundEvent> M4NEW_MAG_HIT_1 = registerSound("m4new_maghit1", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_MAG_HIT_2 = registerSound("m4new_maghit2", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_MAG_HIT_3 = registerSound("m4new_maghit3", "gcr");

    public static RegistryObject<SoundEvent> M4NEW_MAG_IN_1 = registerSound("m4new_magin1", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_MAG_IN_2 = registerSound("m4new_magin2", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_MAG_IN_3 = registerSound("m4new_magin3", "gcr");

    public static RegistryObject<SoundEvent> M4NEW_MAG_MOVE_1 = registerSound("m4new_magmove1", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_MAG_MOVE_2 = registerSound("m4new_magmove2", "gcr");

    public static RegistryObject<SoundEvent> M4NEW_MAG_OUT_1 = registerSound("m4new_magout1", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_MAG_OUT_2 = registerSound("m4new_magout2", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_MAG_OUT_3 = registerSound("m4new_magout3", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_MAG_OUT_4 = registerSound("m4new_magout4", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_MAG_OUT_CHECK = registerSound("m4new_magoutcheck", "gcr");

    public static RegistryObject<SoundEvent> M4NEW_BIG_MOVE_1 = registerSound("m4new_bigmove1", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_BIG_MOVE_2 = registerSound("m4new_bigmove2", "gcr");

    public static RegistryObject<SoundEvent> M4NEW_SMALL_MOVE_1 = registerSound("m4new_smallmove1", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_SMALL_MOVE_2 = registerSound("m4new_smallmove2", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_SMALL_MOVE_3 = registerSound("m4new_smallmove3", "gcr");

    public static RegistryObject<SoundEvent> M4NEW_REGRIP_1 = registerSound("m4new_regrip1", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_REGRIP_2 = registerSound("m4new_regrip2", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_REGRIP_3 = registerSound("m4new_regrip3", "gcr");

    public static RegistryObject<SoundEvent> M4NEW_REMOVE_STUCK = registerSound("m4new_removestuck", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_SELECTOR_1 = registerSound("m4new_selector1", "gcr");
    public static RegistryObject<SoundEvent> M4NEW_SELECTOR_2 = registerSound("m4new_selector2", "gcr");

    // AK24
    public static RegistryObject<SoundEvent> AK24_FIRE_V2 = registerSound("ak24_fire_v2", "gcr");
    public static RegistryObject<SoundEvent> AK24_FIRE_SUP_V2 = registerSound("ak24_fire_sup_v2", "gcr");

    public static RegistryObject<SoundEvent> AK24_CHARGE_BACK = registerSound("ak24_chargeback", "gcr");
    public static RegistryObject<SoundEvent> AK24_CHARGE_FORWARD = registerSound("ak24_chargeforward", "gcr");

    public static RegistryObject<SoundEvent> AK24_BOLT_RELEASE_1 = registerSound("ak24_boltrelease1", "gcr");
    public static RegistryObject<SoundEvent> AK24_BOLT_RELEASE_2 = registerSound("ak24_boltrelease2", "gcr");

    public static RegistryObject<SoundEvent> AK24_CHECK_BACK = registerSound("ak24_checkback", "gcr");
    public static RegistryObject<SoundEvent> AK24_CHECK_FORWARD = registerSound("ak24_checkforward", "gcr");

    public static RegistryObject<SoundEvent> AK24_MAG_HIT_1 = registerSound("ak24_maghit1", "gcr");
    public static RegistryObject<SoundEvent> AK24_MAG_HIT_2 = registerSound("ak24_maghit2", "gcr");
    public static RegistryObject<SoundEvent> AK24_MAG_HIT_3 = registerSound("ak24_maghit3", "gcr");
    public static RegistryObject<SoundEvent> AK24_MAG_HIT_4 = registerSound("ak24_maghit4", "gcr");
    public static RegistryObject<SoundEvent> AK24_MAG_HIT_5 = registerSound("ak24_maghit5", "gcr");
    public static RegistryObject<SoundEvent> AK24_MAG_HIT_6 = registerSound("ak24_maghit6", "gcr");

    public static RegistryObject<SoundEvent> AK24_MAG_IN_1 = registerSound("ak24_magin1", "gcr");
    public static RegistryObject<SoundEvent> AK24_MAG_IN_2 = registerSound("ak24_magin2", "gcr");
    public static RegistryObject<SoundEvent> AK24_MAG_IN_3 = registerSound("ak24_magin3", "gcr");
    public static RegistryObject<SoundEvent> AK24_MAG_IN_4 = registerSound("ak24_magin4", "gcr");

    public static RegistryObject<SoundEvent> AK24_MAG_OUT_1 = registerSound("ak24_magout1", "gcr");
    public static RegistryObject<SoundEvent> AK24_MAG_OUT_2 = registerSound("ak24_magout2", "gcr");
    public static RegistryObject<SoundEvent> AK24_MAG_OUT_3 = registerSound("ak24_magout3", "gcr");
    public static RegistryObject<SoundEvent> AK24_MAG_OUT_4 = registerSound("ak24_magout4", "gcr");
    public static RegistryObject<SoundEvent> AK24_MAG_OUT_5 = registerSound("ak24_magout5", "gcr");

    public static RegistryObject<SoundEvent> AK24_BIG_MOVE_1 = registerSound("ak24_bigmove1", "gcr");
    public static RegistryObject<SoundEvent> AK24_BIG_MOVE_2 = registerSound("ak24_bigmove2", "gcr");

    public static RegistryObject<SoundEvent> AK24_SMALL_MOVE_1 = registerSound("ak24_smallmove1", "gcr");
    public static RegistryObject<SoundEvent> AK24_SMALL_MOVE_2 = registerSound("ak24_smallmove2", "gcr");
    public static RegistryObject<SoundEvent> AK24_SMALL_MOVE_3 = registerSound("ak24_smallmove3", "gcr");

    public static RegistryObject<SoundEvent> AK24_EASTEREGG_MAG_IN = registerSound("ak24_easteregg_magin", "gcr");
    public static RegistryObject<SoundEvent> AK24_EASTEREGG_MAG_OUT = registerSound("ak24_easteregg_magout", "gcr");
    public static RegistryObject<SoundEvent> AK24_EASTEREGG_MOVE_1 = registerSound("ak24_easteregg_move1", "gcr");
    public static RegistryObject<SoundEvent> AK24_EASTEREGG_MOVE_2 = registerSound("ak24_easteregg_move2", "gcr");
    public static RegistryObject<SoundEvent> AK24_EASTEREGG_MOVE_3 = registerSound("ak24_easteregg_move3", "gcr");
    public static RegistryObject<SoundEvent> AK24_EASTEREGG_MOVE_4 = registerSound("ak24_easteregg_move4", "gcr");

    private static RegistryObject<SoundEvent> registerSound(String name) {
        return registerSound(name, GCR.MODID);
    }

    public static RegistryObject<SoundEvent> registerSound(String name, String id) {
        if (GCR.MODID.equals(id)) {
            return MOD_SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(GCR.RL(GCR.MODID, name)));
        } else {
            if (!ADDON_SOUNDS.containsKey(id)) {
                ADDON_SOUNDS.put(id, DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, id));
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
