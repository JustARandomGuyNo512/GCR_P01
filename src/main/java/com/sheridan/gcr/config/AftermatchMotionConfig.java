package com.sheridan.gcr.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Aftermatch-only first-person presentation options.
 * Sway defaults reproduce the supplied TaCZ Additions client configuration.
 * Wall-tuck is configured separately because it is not in that client TOML.
 */
public final class AftermatchMotionConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_GUN_MOVEMENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_STRAFE_MOVEMENT;
    public static final ForgeConfigSpec.DoubleValue DRAG_SMOOTHING;
    public static final ForgeConfigSpec.DoubleValue DECAY_FACTOR;
    public static final ForgeConfigSpec.DoubleValue MOMENTUM_FACTOR;
    public static final ForgeConfigSpec.DoubleValue ROLL_SENSITIVITY;
    public static final ForgeConfigSpec.DoubleValue MAX_TILT_ANGLE;

    public static final ForgeConfigSpec.DoubleValue HIP_YAW_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue HIP_PITCH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue HIP_ROLL_FACTOR;
    public static final ForgeConfigSpec.DoubleValue HIP_MAX_PITCH;
    public static final ForgeConfigSpec.DoubleValue HIP_MAX_YAW;

    // Optional test-only inertial roll when the weapon is held at the hip.
    public static final ForgeConfigSpec.BooleanValue ENABLE_HIPFIRE_BANK;
    public static final ForgeConfigSpec.DoubleValue HIPFIRE_BANK_MAX_ANGLE;
    public static final ForgeConfigSpec.DoubleValue HIPFIRE_BANK_TURN_SENSITIVITY;
    public static final ForgeConfigSpec.DoubleValue HIPFIRE_BANK_STRAFE_ANGLE;
    public static final ForgeConfigSpec.DoubleValue HIPFIRE_BANK_RESPONSE;
    public static final ForgeConfigSpec.DoubleValue HIPFIRE_BANK_DAMPING;

    public static final ForgeConfigSpec.DoubleValue AIM_YAW_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue AIM_ROLL_FACTOR;
    public static final ForgeConfigSpec.DoubleValue AIM_MAX_PITCH;
    public static final ForgeConfigSpec.DoubleValue AIM_MAX_YAW;

    public static final ForgeConfigSpec.DoubleValue STRAFE_HIP_YAW;
    public static final ForgeConfigSpec.DoubleValue STRAFE_HIP_ROLL;
    public static final ForgeConfigSpec.DoubleValue STRAFE_AIM_YAW;
    public static final ForgeConfigSpec.DoubleValue STRAFE_AIM_ROLL;
    public static final ForgeConfigSpec.DoubleValue STRAFE_MAX_YAW;
    public static final ForgeConfigSpec.DoubleValue STRAFE_MAX_ROLL;
    public static final ForgeConfigSpec.DoubleValue STRAFE_SMOOTHING;

    public static final ForgeConfigSpec.DoubleValue AIM_IDLE_SWAY_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue AIM_IDLE_SWAY_CYCLE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SCOPE_SWAY;
    public static final ForgeConfigSpec.DoubleValue SCOPE_SWAY_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue SCOPE_SWAY_SPEED;
    public static final ForgeConfigSpec.DoubleValue SCOPE_SWAY_MIN_ZOOM;
    public static final ForgeConfigSpec.DoubleValue CROUCH_STABILIZE_TIME;
    public static final ForgeConfigSpec.DoubleValue CROUCH_SPORADIC_TIME;
    public static final ForgeConfigSpec.DoubleValue CROUCH_COOLDOWN_TIME;
    public static final ForgeConfigSpec.DoubleValue SPORADIC_SWAY_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue SPORADIC_SWAY_SPEED;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("client");
        ENABLE_GUN_MOVEMENT = b.comment("Enable camera-turn inertia, sway and roll.").define("enableGunMovement", true);
        ENABLE_STRAFE_MOVEMENT = b.comment("Enable strafing sway and tilt.").define("enableStrafeMovement", true);
        DRAG_SMOOTHING = b.defineInRange("dragSmoothing", 0.2, 0.0, 1.0);
        DECAY_FACTOR = b.defineInRange("decayFactor", 0.85, 0.0, 1.0);
        MOMENTUM_FACTOR = b.defineInRange("momentumFactor", 0.45, 0.0, 1.0);
        ROLL_SENSITIVITY = b.defineInRange("rollSensitivity", 1.2, 0.0, 10.0);
        MAX_TILT_ANGLE = b.defineInRange("maxTiltAngle", 20.0, 0.0, 180.0);

        b.push("hipfire");
        HIP_YAW_MULTIPLIER = b.defineInRange("yawMultiplier", 1.25, 0.0, 10.0);
        HIP_PITCH_MULTIPLIER = b.defineInRange("pitchMultiplier", 1.2, 0.0, 10.0);
        HIP_ROLL_FACTOR = b.defineInRange("rollFactor", 2.75, 0.0, 10.0);
        HIP_MAX_PITCH = b.defineInRange("maxHipPitch", 6.0, 0.0, 45.0);
        HIP_MAX_YAW = b.defineInRange("maxHipYaw", 12.0, 0.0, 45.0);
        b.pop();

        b.push("hipfireBank");
        ENABLE_HIPFIRE_BANK = b.comment("TEST: lean the weapon and both arms when turning or strafing outside ADS. Disable to restore the previous motion.")
                .define("enabled", true);
        HIPFIRE_BANK_MAX_ANGLE = b.comment("Maximum additional hip-fire roll in degrees.")
                .defineInRange("maxAngle", 9.0, 0.0, 20.0);
        HIPFIRE_BANK_TURN_SENSITIVITY = b.comment("Additional lean degrees per degree/second of mouse turning.")
                .defineInRange("turnSensitivity", 0.055, 0.0, 0.20);
        HIPFIRE_BANK_STRAFE_ANGLE = b.comment("Maximum extra roll contributed by moving sideways.")
                .defineInRange("strafeAngle", 3.5, 0.0, 12.0);
        HIPFIRE_BANK_RESPONSE = b.comment("Spring response in radians/second; larger is quicker.")
                .defineInRange("springResponse", 12.0, 3.0, 30.0);
        HIPFIRE_BANK_DAMPING = b.comment("Spring damping ratio: less than 1 adds a little overshoot.")
                .defineInRange("springDamping", 0.78, 0.4, 1.5);
        b.pop();

        b.push("aim");
        AIM_YAW_MULTIPLIER = b.defineInRange("yawMultiplier", 0.6, 0.0, 10.0);
        AIM_ROLL_FACTOR = b.defineInRange("rollFactor", 2.75, 0.0, 10.0);
        AIM_MAX_PITCH = b.defineInRange("maxAimPitch", 2.0, 0.0, 45.0);
        AIM_MAX_YAW = b.defineInRange("maxAimYaw", 10.0, 0.0, 45.0);
        b.pop();

        b.push("strafe");
        STRAFE_HIP_YAW = b.defineInRange("hipfireYawMultiplier", 0.0, 0.0, 40.0);
        STRAFE_HIP_ROLL = b.defineInRange("hipfireRollMultiplier", 20.0, 0.0, 40.0);
        STRAFE_AIM_YAW = b.defineInRange("aimingYawMultiplier", 0.0, 0.0, 40.0);
        STRAFE_AIM_ROLL = b.defineInRange("aimingRollMultiplier", 20.0, 0.0, 40.0);
        STRAFE_MAX_YAW = b.defineInRange("maxStrafeYaw", 6.0, 0.0, 20.0);
        STRAFE_MAX_ROLL = b.defineInRange("maxStrafeRoll", 20.0, 0.0, 40.0);
        STRAFE_SMOOTHING = b.defineInRange("strafeSmoothing", 0.15, 0.01, 1.0);
        b.pop();
        b.pop();

        b.push("adsBreathing");
        AIM_IDLE_SWAY_STRENGTH = b.comment("Gentle continuous first-person sway while aiming, degrees.")
                .defineInRange("aimIdleSwayStrength", 0.12, 0.0, 1.0);
        AIM_IDLE_SWAY_CYCLE = b.comment("Seconds per full general ADS breathing oscillation.")
                .defineInRange("aimIdleSwayCycle", 3.2, 1.0, 30.0);
        b.pop();

        b.push("scopeSway");
        ENABLE_SCOPE_SWAY = b.comment("Subtle 4x+ scope sway, as in supplied TaCZ Additions client config.")
                .define("enableScopeSway", true);
        SCOPE_SWAY_STRENGTH = b.defineInRange("strength", 0.01, 0.0, 1.0);
        SCOPE_SWAY_SPEED = b.defineInRange("speed", 40.2, 1.0, 120.0);
        SCOPE_SWAY_MIN_ZOOM = b.defineInRange("minZoom", 4.0, 1.0, 100.0);
        CROUCH_STABILIZE_TIME = b.defineInRange("crouchStabilizeTime", 8000.0, 0.0, 10000.0);
        CROUCH_SPORADIC_TIME = b.defineInRange("crouchSporadicTime", 8000.0, 0.0, 10000.0);
        CROUCH_COOLDOWN_TIME = b.defineInRange("crouchCooldownTime", 8000.0, 0.0, 20000.0);
        SPORADIC_SWAY_STRENGTH = b.defineInRange("sporadicSwayStrength", 7.0, 1.0, 10.0);
        SPORADIC_SWAY_SPEED = b.defineInRange("sporadicSwaySpeed", 0.3, 0.1, 5.0);
        b.pop();

        SPEC = b.build();
    }

    private AftermatchMotionConfig() {}
}
