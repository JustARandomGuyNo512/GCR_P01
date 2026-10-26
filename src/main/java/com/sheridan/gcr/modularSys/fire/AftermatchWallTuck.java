package com.sheridan.gcr.modularSys.fire;

import com.sheridan.gcr.config.AftermatchWallConfig;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared client/server obstacle ray: never trusts a client-supplied bullet direction. */
public final class AftermatchWallTuck {
    public record State(float progress, float distance, float raiseDegrees, boolean fireBlocked) {
        public static final State CLEAR = new State(0f, Float.POSITIVE_INFINITY, 0f, false);
    }

    private AftermatchWallTuck() {}

    public static State check(LivingEntity shooter, float partialTicks) {
        if (shooter == null || shooter.level() == null || !AftermatchWallConfig.ENABLE_GUN_TUCK.get()) {
            return State.CLEAR;
        }
        final float distance = AftermatchWallConfig.TUCK_DISTANCE.get().floatValue();
        final Vec3 eyes = shooter.getEyePosition(partialTicks);
        final Vec3 direction = shooter.getViewVector(partialTicks);
        final Vec3 end = eyes.add(direction.scale(distance));

        BlockHitResult hit = shooter.level().clip(new ClipContext(
                eyes, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        if (hit.getType() != HitResult.Type.BLOCK
                || hit.getDirection().getAxis() == Direction.Axis.Y) {
            return State.CLEAR;
        }

        float hitDistance = (float) eyes.distanceTo(hit.getLocation());
        float proximity = Mth.clamp(1f - hitDistance / distance, 0f, 1f);
        float eased = proximity * proximity * (3f - 2f * proximity);
        float angle = eased * Math.min(AftermatchWallConfig.TUCK_MAX_ANGLE.get().floatValue(),
                AftermatchWallConfig.TUCK_VISUAL_ANGLE.get().floatValue());
        boolean blocked = AftermatchWallConfig.BLOCK_FIRE_WHEN_TUCKED.get()
                && hitDistance <= AftermatchWallConfig.BLOCK_DISTANCE.get().floatValue();
        return new State(proximity, hitDistance, angle, blocked);
    }
}
