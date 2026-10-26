package com.sheridan.gcr.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.config.AftermatchMotionConfig;
import com.sheridan.gcr.config.AftermatchWallConfig;
import com.sheridan.gcr.modularSys.fire.AftermatchWallTuck;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.builder.Node;
import com.sheridan.gcr.modularSys.modules.IScope;
import com.sheridan.gcr.modularSys.modules.ISight;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Purely visual Aftermatch first-person weapon presentation.
 * Operates on GCR's common model pose so the weapon AND both animated arms
 * move together. Never modifies recoil calculations, ADS state or bullet rays.
 */
@OnlyIn(Dist.CLIENT)
public final class AftermatchWeaponMotion {
    private long lastFrameNanos;
    private long lastWallProbeNanos;
    private Item lastItem;
    private float lastYaw;
    private float lastPitch;
    private boolean hasViewAngles;
    private float yawVelocity;
    private float pitchVelocity;
    private float rollVelocity;
    private float swayYaw;
    private float swayPitch;
    private float swayRoll;
    private float strafeSmoothed;
    // Independent, frame-rate-correct spring roll for the hip-fire test.
    private float hipBankAngle;
    private float hipBankVelocity;
    private float tuckProgress;
    private float wallTarget;
    private long crouchStartedNanos;

    /** Immutable cross-thread render sample for the client's fire worker. */
    public record ShotOffset(float yawDegrees, float pitchDegrees, long timestampNanos) {}
    private static final ShotOffset ZERO_SHOT_OFFSET = new ShotOffset(0f, 0f, 0L);

    // Static initialization order is significant: INSTANCE constructs the
    // class and copies ZERO_SHOT_OFFSET into its instance field. Initializing
    // INSTANCE first left latestShotOffset null until the first weapon render,
    // causing a HUD crash if the hipfire dot requested an early shot sample.
    public static final AftermatchWeaponMotion INSTANCE = new AftermatchWeaponMotion();

    private volatile ShotOffset latestShotOffset = ZERO_SHOT_OFFSET;

    /**
     * Never fire with a stale render pose (unfocused client, switched weapon,
     * camera loading). Coordinates are additional yaw and pitch in degrees.
     */
    public ShotOffset shotOffset() {
        ShotOffset sample = latestShotOffset;
        // Fail safe during first render, resource reloads and weapon changes.
        // Never let a missing visual-only sample crash the client HUD.
        if (sample == null || sample.timestampNanos() <= 0L) {
            return ZERO_SHOT_OFFSET;
        }
        long age = System.nanoTime() - sample.timestampNanos();
        return age >= 0L && age < 250_000_000L ? sample : ZERO_SHOT_OFFSET;
    }

    private AftermatchWeaponMotion() {}

    public void apply(PoseStack poseStack, LocalPlayer player, float partialTicks) {
        if (player == null || player.level() == null) {
            return;
        }
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 1f / 60f
                : Mth.clamp((now - lastFrameNanos) / 1_000_000_000f, 0.001f, 0.05f);
        lastFrameNanos = now;
        float dtFrames = dt * 60f;

        Item currentItem = player.getMainHandItem().getItem();
        float pitch = player.getViewXRot(partialTicks);
        float yaw = player.getViewYRot(partialTicks);
        if (!hasViewAngles || currentItem != lastItem) {
            resetSway();
            lastYaw = yaw;
            lastPitch = pitch;
            hasViewAngles = true;
            lastItem = currentItem;
        }

        // Probe only twenty times a second even at high client FPS.
        if (now - lastWallProbeNanos >= 50_000_000L || lastWallProbeNanos == 0L) {
            wallTarget = AftermatchWallTuck.check(player, partialTicks).progress();
            lastWallProbeNanos = now;
        }
        float tuckBlend = 1f - (float) Math.exp(-AftermatchWallConfig.TUCK_SMOOTHING.get() * dt);
        tuckProgress = Mth.lerp(tuckBlend, tuckProgress, wallTarget);
        if (tuckProgress < 0.0001f) tuckProgress = 0f;

        float aim = Mth.clamp(Client.getAimingProgress(partialTicks), 0f, 1f);
        if (AftermatchMotionConfig.ENABLE_GUN_MOVEMENT.get()) {
            float dYaw = Mth.clamp(Mth.wrapDegrees(yaw - lastYaw), -30f, 30f);
            float dPitch = Mth.clamp(pitch - lastPitch, -30f, 30f);
            float drag = AftermatchMotionConfig.DRAG_SMOOTHING.get().floatValue();
            float damping = (float) Math.pow(AftermatchMotionConfig.DECAY_FACTOR.get(), dtFrames);
            float momentum = AftermatchMotionConfig.MOMENTUM_FACTOR.get().floatValue();
            float yawFactor = Mth.lerp(aim, AftermatchMotionConfig.HIP_YAW_MULTIPLIER.get().floatValue(),
                    AftermatchMotionConfig.AIM_YAW_MULTIPLIER.get().floatValue());
            float pitchFactor = Mth.lerp(aim, AftermatchMotionConfig.HIP_PITCH_MULTIPLIER.get().floatValue(), 1f);
            float rollFactor = Mth.lerp(aim, AftermatchMotionConfig.HIP_ROLL_FACTOR.get().floatValue(),
                    AftermatchMotionConfig.AIM_ROLL_FACTOR.get().floatValue());

            yawVelocity = yawVelocity * damping + dYaw * drag * yawFactor;
            pitchVelocity = pitchVelocity * damping + dPitch * drag * pitchFactor;
            rollVelocity = rollVelocity * damping - dYaw * drag * 0.2f * rollFactor;

            float settling = (float) Math.pow(AftermatchMotionConfig.DECAY_FACTOR.get(), dtFrames * 0.7f);
            swayYaw = Mth.clamp((swayYaw + yawVelocity * momentum * dtFrames) * settling,
                    -Mth.lerp(aim, AftermatchMotionConfig.HIP_MAX_YAW.get().floatValue(), AftermatchMotionConfig.AIM_MAX_YAW.get().floatValue()),
                    Mth.lerp(aim, AftermatchMotionConfig.HIP_MAX_YAW.get().floatValue(), AftermatchMotionConfig.AIM_MAX_YAW.get().floatValue()));
            float maxPitch = Mth.lerp(aim, AftermatchMotionConfig.HIP_MAX_PITCH.get().floatValue(),
                    AftermatchMotionConfig.AIM_MAX_PITCH.get().floatValue());
            swayPitch = Mth.clamp((swayPitch + pitchVelocity * momentum * dtFrames) * settling, -maxPitch, maxPitch);
            float maxRoll = AftermatchMotionConfig.MAX_TILT_ANGLE.get().floatValue();
            swayRoll = Mth.clamp((swayRoll + rollVelocity * momentum * dtFrames
                    * AftermatchMotionConfig.ROLL_SENSITIVITY.get().floatValue()) * settling, -maxRoll, maxRoll);

            if (AftermatchMotionConfig.ENABLE_STRAFE_MOVEMENT.get()) {
                Vec3 v = player.getDeltaMovement();
                float sideways = (float) (v.x * Math.cos(Math.toRadians(yaw))
                        + v.z * Math.sin(Math.toRadians(yaw)));
                float targetStrafe = Mth.clamp(sideways / 0.22f, -1f, 1f);
                float strafeRate = AftermatchMotionConfig.STRAFE_SMOOTHING.get().floatValue();
                float strafeBlend = 1f - (float) Math.pow(1f - strafeRate, dtFrames);
                strafeSmoothed = Mth.lerp(strafeBlend, strafeSmoothed, targetStrafe);
            } else {
                strafeSmoothed *= damping;
            }

            float strafeYawFactor = Mth.lerp(aim, AftermatchMotionConfig.STRAFE_HIP_YAW.get().floatValue(),
                    AftermatchMotionConfig.STRAFE_AIM_YAW.get().floatValue());
            float strafeRollFactor = Mth.lerp(aim, AftermatchMotionConfig.STRAFE_HIP_ROLL.get().floatValue(),
                    AftermatchMotionConfig.STRAFE_AIM_ROLL.get().floatValue());

            float strafeYaw = Mth.clamp(strafeSmoothed * strafeYawFactor,
                    -AftermatchMotionConfig.STRAFE_MAX_YAW.get().floatValue(),
                    AftermatchMotionConfig.STRAFE_MAX_YAW.get().floatValue());
            float strafeRoll = Mth.clamp(strafeSmoothed * strafeRollFactor,
                    -AftermatchMotionConfig.STRAFE_MAX_ROLL.get().floatValue(),
                    AftermatchMotionConfig.STRAFE_MAX_ROLL.get().floatValue());

            // TEST: spring-based camera-relative banking of the ENTIRE gun
            // and hands when hip firing. Unlike the existing swayRoll this
            // responds to angular speed rather than per-frame displacement,
            // producing consistent tilt at different client FPS.
            float additionalBank = 0f;
            if (AftermatchMotionConfig.ENABLE_HIPFIRE_BANK.get()) {
                float hipWeight = (1f - aim) * (1f - aim);
                float turnRate = Mth.clamp(dYaw / dt, -220f, 220f);
                float targetBank = Mth.clamp(
                        -turnRate * AftermatchMotionConfig.HIPFIRE_BANK_TURN_SENSITIVITY.get().floatValue()
                                - strafeSmoothed * AftermatchMotionConfig.HIPFIRE_BANK_STRAFE_ANGLE.get().floatValue(),
                        -AftermatchMotionConfig.HIPFIRE_BANK_MAX_ANGLE.get().floatValue(),
                        AftermatchMotionConfig.HIPFIRE_BANK_MAX_ANGLE.get().floatValue()) * hipWeight;

                float spring = AftermatchMotionConfig.HIPFIRE_BANK_RESPONSE.get().floatValue();
                float dampingRatio = AftermatchMotionConfig.HIPFIRE_BANK_DAMPING.get().floatValue();
                float acceleration = (targetBank - hipBankAngle) * spring * spring
                        - hipBankVelocity * (2f * dampingRatio * spring);
                hipBankVelocity = Mth.clamp(hipBankVelocity + acceleration * dt, -160f, 160f);
                hipBankAngle = Mth.clamp(hipBankAngle + hipBankVelocity * dt, -12f, 12f);
                if (Math.abs(hipBankAngle) < 0.001f && Math.abs(hipBankVelocity) < 0.01f) {
                    hipBankAngle = 0f;
                    hipBankVelocity = 0f;
                }
                // ADS fades the entire experimental bank, even while the
                // underlying spring is settling, so optics remain unchanged.
                additionalBank = hipBankAngle * hipWeight;
            } else {
                hipBankAngle = hipBankVelocity = 0f;
            }

            // Apply full-strength inertia even while sprinting. Previously
            // motionStrength was reduced to 0.35 in sprint, making the effect
            // almost invisible underneath GCR's sprint animation.
            // This remains additive to the existing breathing / walking /
            // sprinting pose and never replaces their animation tracks.
            Quaternionf inertiaRotation = new Quaternionf().rotateXYZ(
                    radians(-swayPitch),
                    radians(-(swayYaw + strafeYaw)),
                    radians(swayRoll + strafeRoll));
            // Side sights rotate the gun strongly about Z (canted rail).
            // Right-multiplying inertia directly rotates in the gun's tilted
            // axes, displacing the sight along the wrong screen direction.
            // Conjugate by the actual current gun pose so the inertial swing
            // remains camera-relative even with a canted/offset optic.
            Node currentSight = Client.WEAPON_STATUS.getActiveSight();
            boolean cantedAds = aim > 0.001f && currentSight != null
                    && currentSight.getUnit().getCustomParam(ISight.ON_SIDE_POSITION) != -1;
            Quaternionf cameraToGun = cantedAds
                    ? poseStack.last().pose().getUnnormalizedRotation(new Quaternionf()).normalize()
                    : null;
            Quaternionf renderInertia = cantedAds
                    ? new Quaternionf(cameraToGun).invert().mul(inertiaRotation).mul(cameraToGun)
                    : inertiaRotation;
            poseStack.mulPose(renderInertia);
            if (Math.abs(additionalBank) > 0.0001f) {
                // Lean about a point slightly below the weapon centre rather
                // than only spinning around the camera origin. The extra
                // roll is around the local bore axis: it visibly tilts the
                // weapon and hands, but does not change the gun's forward ray.
                poseStack.translate(0f, -0.13f, 0f);
                poseStack.mulPose(new Quaternionf().rotateZ(radians(additionalBank)));
                poseStack.translate(0f, 0.13f, 0f);
            }
            // Ballistics still use the camera-space rotation. Do not rotate
            // the server projectile a second time by the canted sight angle.
            Quaternionf shotRotation = new Quaternionf(inertiaRotation);

            // ADS is deliberately not static: breathing continues even with no mouse input.
            if (aim > 0f) {
                double seconds = now / 1_000_000_000.0;
                double baseCycle = AftermatchMotionConfig.AIM_IDLE_SWAY_CYCLE.get();
                float strength = AftermatchMotionConfig.AIM_IDLE_SWAY_STRENGTH.get().floatValue() * aim;
                float breathePitch = (float) Math.sin(seconds * Math.PI * 2.0 / baseCycle) * strength;
                float breatheYaw = (float) Math.sin(seconds * Math.PI * 2.0 / (baseCycle * 1.31) + 0.8) * strength * 0.75f;
                float zoom = getActiveScopeZoom(player);
                if (AftermatchMotionConfig.ENABLE_SCOPE_SWAY.get()
                        && zoom >= AftermatchMotionConfig.SCOPE_SWAY_MIN_ZOOM.get()) {
                    double scopeCycle = AftermatchMotionConfig.SCOPE_SWAY_SPEED.get();
                    float scopeStrength = AftermatchMotionConfig.SCOPE_SWAY_STRENGTH.get().floatValue()
                            * getCrouchScopeFactor(player, now) * aim;
                    breathePitch += (float) Math.sin(seconds * Math.PI * 2.0 / scopeCycle) * scopeStrength;
                    breatheYaw += (float) Math.cos(seconds * Math.PI * 2.0 / (scopeCycle * 0.71)) * scopeStrength;
                } else {
                    crouchStartedNanos = 0L;
                }
                Quaternionf breathingRotation = new Quaternionf()
                        .rotateXYZ(radians(breathePitch), radians(breatheYaw), 0f);
                Quaternionf renderBreathing = cantedAds
                        ? new Quaternionf(cameraToGun).invert().mul(breathingRotation).mul(cameraToGun)
                        : breathingRotation;
                poseStack.mulPose(renderBreathing);
                shotRotation.mul(breathingRotation);
            } else {
                crouchStartedNanos = 0L;
            }
            // Forward in first-person model space is negative Z.
            Vector3f barrel = shotRotation.transform(new Vector3f(0f, 0f, -1f));
            float shotYaw = (float) Math.toDegrees(Math.atan2(barrel.x, -barrel.z));
            float shotPitch = (float) Math.toDegrees(Math.atan2(-barrel.y,
                    Math.hypot(barrel.x, barrel.z)));
            latestShotOffset = new ShotOffset(
                    Mth.clamp(shotYaw, -20f, 20f),
                    Mth.clamp(shotPitch, -10f, 10f), now);
        } else {
            resetSway();
            latestShotOffset = ZERO_SHOT_OFFSET;
        }

        lastYaw = yaw;
        lastPitch = pitch;

        // Move both animated arms and the gun as ONE unit. Positive local Z
        // pulls the weapon toward the camera; negative Y lowers the hands.
        // This hides the clipped arm ends seen when raising by rotation alone.
        if (tuckProgress > 0f) {
            float eased = tuckProgress * tuckProgress * (3f - 2f * tuckProgress);
            float angle = Math.min(AftermatchWallConfig.TUCK_MAX_ANGLE.get().floatValue(),
                    AftermatchWallConfig.TUCK_VISUAL_ANGLE.get().floatValue()) * eased;
            float pullback = AftermatchWallConfig.TUCK_MAX_TRANSLATE.get().floatValue()
                    + AftermatchWallConfig.TUCK_ADDITIONAL_PULLBACK.get().floatValue();
            float lower = AftermatchWallConfig.TUCK_DOWNWARD_SHIFT.get().floatValue();
            poseStack.translate(0f, -lower * eased, pullback * eased);
            poseStack.translate(0f, 0f, 0.50f);
            poseStack.mulPose(new Quaternionf().rotateX(radians(angle)));
            poseStack.translate(0f, 0f, -0.50f);
        }
    }

    private static float getActiveScopeZoom(LocalPlayer player) {
        Node sight = Client.WEAPON_STATUS.getActiveSight();
        if (sight == null || !(sight.getModule() instanceof IScope scope)
                || !(player.getMainHandItem().getItem() instanceof GunItem gunItem)) {
            return 1f;
        }
        CompoundTag states = gunItem.getGun().getNodeStatesTag(player.getMainHandItem(), sight.getID());
        return states == null ? 1f : scope.getRate(states);
    }

    private float getCrouchScopeFactor(LocalPlayer player, long now) {
        if (!player.isCrouching()) {
            crouchStartedNanos = 0L;
            return 1f;
        }
        if (crouchStartedNanos == 0L) crouchStartedNanos = now;
        double time = (now - crouchStartedNanos) / 1_000_000.0;
        double stabilize = AftermatchMotionConfig.CROUCH_STABILIZE_TIME.get();
        double sporadic = AftermatchMotionConfig.CROUCH_SPORADIC_TIME.get();
        double cooldown = AftermatchMotionConfig.CROUCH_COOLDOWN_TIME.get();
        if (time < stabilize) return 1f;
        time -= stabilize;
        if (time < sporadic) return 0.25f;
        time -= sporadic;
        if (time < cooldown) {
            return AftermatchMotionConfig.SPORADIC_SWAY_STRENGTH.get().floatValue()
                    * AftermatchMotionConfig.SPORADIC_SWAY_SPEED.get().floatValue();
        }
        crouchStartedNanos = now;
        return 1f;
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    private void resetSway() {
        yawVelocity = pitchVelocity = rollVelocity = 0f;
        swayYaw = swayPitch = swayRoll = 0f;
        strafeSmoothed = 0f;
        hipBankAngle = hipBankVelocity = 0f;
    }
}
