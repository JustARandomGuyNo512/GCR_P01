package com.sheridan.gcr.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * GCR's player-only anatomical hit detection and damage tuning.
 * Based on the HEAD/TORSO/LEGS/FEET categories from AftermatchDayZ.
 * No dependency on AftermatchDayZ is required in the public release.
 */
public final class PlayerHitZoneConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue HEAD;
    public static final ForgeConfigSpec.DoubleValue TORSO;
    public static final ForgeConfigSpec.DoubleValue LEGS;
    public static final ForgeConfigSpec.DoubleValue FEET;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("playerHitZones");
        ENABLED = builder.comment("Apply anatomical collision and location damage to players only.")
                .define("enabled", true);
        HEAD = builder.comment("Head damage multiplier, original AftermatchDayZ default 1.4.")
                .defineInRange("headMultiplier", 1.4d, 0d, 20d);
        TORSO = builder.comment("Torso damage multiplier.")
                .defineInRange("torsoMultiplier", 1d, 0d, 20d);
        LEGS = builder.comment("Thigh and shin damage multiplier.")
                .defineInRange("legsMultiplier", 0.6d, 0d, 20d);
        FEET = builder.comment("Foot damage multiplier.")
                .defineInRange("feetMultiplier", 0.75d, 0d, 20d);
        builder.pop();
        SPEC = builder.build();
    }

    public static float multiplier(PlayerAnatomyZone zone) {
        if (!ENABLED.get()) return 1f;
        return switch (zone) {
            case HEAD -> HEAD.get().floatValue();
            case TORSO -> TORSO.get().floatValue();
            case LEGS -> LEGS.get().floatValue();
            case FEET -> FEET.get().floatValue();
        };
    }

    public enum PlayerAnatomyZone { HEAD, TORSO, LEGS, FEET }
    private PlayerHitZoneConfig() {}
}
