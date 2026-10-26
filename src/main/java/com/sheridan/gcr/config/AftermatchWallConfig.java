package com.sheridan.gcr.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Shared wall-obstruction settings. The server independently validates shot
 * blocking and trajectory; clients use the same formulas for presentation.
 */
public final class AftermatchWallConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_GUN_TUCK;
    public static final ForgeConfigSpec.BooleanValue BLOCK_FIRE_WHEN_TUCKED;
    public static final ForgeConfigSpec.DoubleValue TUCK_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue BLOCK_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue TUCK_MAX_ANGLE;
    public static final ForgeConfigSpec.DoubleValue TUCK_MAX_TRANSLATE;
    public static final ForgeConfigSpec.DoubleValue TUCK_VISUAL_ANGLE;
    public static final ForgeConfigSpec.DoubleValue TUCK_ADDITIONAL_PULLBACK;
    public static final ForgeConfigSpec.DoubleValue TUCK_DOWNWARD_SHIFT;
    public static final ForgeConfigSpec.DoubleValue TUCK_SMOOTHING;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("wallTuck");
        ENABLE_GUN_TUCK = b.comment("Raise gun barrel when it approaches a solid wall.").define("enableGunTuck", true);
        BLOCK_FIRE_WHEN_TUCKED = b.comment("Block shooting before ammo consumption when the wall is too close.")
                .define("blockFireWhenTucked", true);
        TUCK_DISTANCE = b.comment("Distance from the shooter's eyes where muzzle raising begins, in blocks.")
                .defineInRange("tuckDistance", 1.15, 0.2, 3.0);
        BLOCK_DISTANCE = b.comment("Shot-block distance from the eyes to the wall, in blocks.")
                .defineInRange("blockDistance", 0.45, 0.1, 1.5);
        TUCK_MAX_ANGLE = b.comment("Maximum muzzle raise and server-side bullet angle, in degrees.")
                .defineInRange("tuckMaxAngle", 55.0, 0.0, 85.0);
        TUCK_MAX_TRANSLATE = b.comment("Existing visual camera pullback (positive local Z).")
                .defineInRange("tuckMaxTranslate", 0.12, 0.0, 0.5);
        TUCK_VISUAL_ANGLE = b.comment("Visual muzzle-raise limit. Also caps server-side wall-raise for projectile alignment.")
                .defineInRange("tuckVisualAngle", 30.0, 0.0, 85.0);
        TUCK_ADDITIONAL_PULLBACK = b.comment("Extra pullback to conceal the clipped ends of animated arms.")
                .defineInRange("tuckAdditionalPullback", 0.22, 0.0, 0.8);
        TUCK_DOWNWARD_SHIFT = b.comment("Lower the whole gun and both arms while tucking, in model units.")
                .defineInRange("tuckDownwardShift", 0.22, 0.0, 0.8);
        TUCK_SMOOTHING = b.comment("Visual blend speed per second.")
                .defineInRange("tuckSmoothing", 12.0, 1.0, 40.0);
        b.pop();
        SPEC = b.build();
    }

    private AftermatchWallConfig() {}
}
