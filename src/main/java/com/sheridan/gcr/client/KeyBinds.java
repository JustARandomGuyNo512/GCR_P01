package com.sheridan.gcr.client;

import com.sheridan.gcr.GCR;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;


@OnlyIn(Dist.CLIENT)
public class KeyBinds {
    public static final KeyMapping SWITCH_FIRE_MODE = new KeyMapping("key.gcr.switch_fire_mode", 86, "keys.categories.gcr");
    public static final KeyMapping RELOAD = new KeyMapping("key.gcr.reload", 82, "keys.categories.gcr");
    public static final KeyMapping OPEN_GUN_MODIFY_SCREEN = new KeyMapping("key.gcr.open_gun_modify_screen", 71, "keys.categories.gcr");

    public static final KeyMapping CHECK_MAG = new KeyMapping("key.gcr.check_mag", 78, "keys.categories.gcr");
    public static final KeyMapping CHECK_CHAMBER = new KeyMapping("key.gcr.check_chamber", 77, "keys.categories.gcr");


    public static final KeyMapping SWITCH_EFFECTIVE_SIGHT = new KeyMapping("key.gcr.switch_effective_sight", 89, "keys.categories.gcr");
    public static final KeyMapping USE_GRENADE_LAUNCHER = new KeyMapping("key.gcr.use_grenade_launcher", 342, "keys.categories.gcr");
    public static final KeyMapping TURN_FLASHLIGHT = new KeyMapping("key.gcr.turn_flashlight", 75, "keys.categories.gcr");

    public static final KeyMapping OPEN_DISPLAY_ADJUST_SCREEN = new KeyMapping("key.gcr.open_display_adjust_screen", 72, "keys.categories.gcr");


    public static final KeyMapping REMOVE_STUCK = new KeyMapping("key.gcr.remove_stuck", 85, "keys.categories.gcr");

    public static final KeyMapping DEBUG_HOT_RELOAD_CLASS = new KeyMapping("key.gcr.debug_hot_reload_class", 96, "keys.categories.gcr");

    public static final KeyMapping CHECK_SUB_WEAPON = new KeyMapping("key.gcr.check_sub_weapon", 73, "keys.categories.gcr");

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(SWITCH_FIRE_MODE);
        event.register(RELOAD);
        event.register(OPEN_GUN_MODIFY_SCREEN);
        event.register(CHECK_MAG);
        event.register(SWITCH_EFFECTIVE_SIGHT);
        event.register(USE_GRENADE_LAUNCHER);
        event.register(CHECK_SUB_WEAPON);
        event.register(TURN_FLASHLIGHT);

        if (GCR.IS_DEVELOPMENT) {
            event.register(OPEN_DISPLAY_ADJUST_SCREEN);
            event.register(REMOVE_STUCK);
            event.register(DEBUG_HOT_RELOAD_CLASS);
        }
    }
}
