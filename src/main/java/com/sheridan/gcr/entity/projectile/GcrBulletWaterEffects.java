package com.sheridan.gcr.entity.projectile;

import com.sheridan.gcr.client.render.fx.particles.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * TaCZ Additions-style, server-authoritative water entry / exit and underwater
 * bullet wake. Called from GCR's existing bullet sweep, with no changes to
 * trajectory, damage, collisions or bullet lifetime.
 */
public final class GcrBulletWaterEffects {
    private static final double SAMPLE_STEP = 0.25;
    private static final int MAX_SAMPLES = 160;
    private static final double TRAIL_SPACING = 0.30;
    private static final int MAX_CROSSINGS_PER_SEGMENT = 4;
    private static final int PARTICLE_BUDGET_PER_TICK = 400;
    private static final int MAX_SOUNDS_PER_TICK = 12;
    private static final int MAX_PENDING_WAVES = 128;
    private static final Deque<Wave> PENDING = new ArrayDeque<>();

    private static int remaining = PARTICLE_BUDGET_PER_TICK;
    private static int sounds;

    private GcrBulletWaterEffects() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            remaining = PARTICLE_BUDGET_PER_TICK;
            sounds = 0;
            return;
        }
        int processed = PENDING.size();
        for (int i = 0; i < processed; i++) {
            Wave wave = PENDING.pollFirst();
            if (wave == null) break;
            if (wave.level.getGameTime() < wave.atTick) {
                PENDING.addLast(wave);
            } else {
                ring(wave.level, wave.position, wave.radius, wave.count);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PENDING.clear();
    }

    public static void trace(ServerLevel level, Vec3 from, Vec3 to) {
        if (remaining <= 0 || !level.hasChunkAt(BlockPos.containing(from))) return;
        Vec3 movement = to.subtract(from);
        double distance = movement.length();
        if (distance <= 1.0e-4) return;
        int steps = Mth.clamp((int) Math.ceil(distance / SAMPLE_STEP), 2, MAX_SAMPLES);
        Vec3 direction = movement.scale(1.0 / distance);
        RandomSource rng = level.random;
        boolean inside = underwater(level, from);
        Vec3 last = from;
        Vec3 entry = inside ? from : null;
        boolean enteredFromSurface = false;
        int crossings = 0;

        for (int i = 1; i <= steps; i++) {
            Vec3 here = from.add(movement.scale(i / (double) steps));
            if (!level.hasChunkAt(BlockPos.containing(here))) return;
            boolean nowInside = underwater(level, here);
            if (nowInside != inside) {
                Vec3 crossing = refine(level, last, here, inside);
                if (nowInside) {
                    entry = crossing;
                    enteredFromSurface = surface(level, crossing);
                } else {
                    if (entry != null) wake(level, rng, entry, crossing, direction, enteredFromSurface);
                    entry = null;
                    enteredFromSurface = false;
                }
                if (crossings++ < MAX_CROSSINGS_PER_SEGMENT && surface(level, crossing)) {
                    splash(level, rng, crossing, direction, nowInside, distance);
                }
            }
            inside = nowInside;
            last = here;
        }
        if (entry != null) wake(level, rng, entry, to, direction, enteredFromSurface);
    }

    private static Vec3 refine(ServerLevel level, Vec3 a, Vec3 b, boolean aInside) {
        for (int i = 0; i < 8; i++) {
            Vec3 mid = a.add(b).scale(0.5);
            if (underwater(level, mid) == aInside) a = mid;
            else b = mid;
        }
        return a.add(b).scale(0.5);
    }

    private static boolean underwater(ServerLevel level, Vec3 p) {
        BlockPos pos = BlockPos.containing(p);
        if (!level.hasChunkAt(pos)) return false;
        FluidState fluid = level.getFluidState(pos);
        return fluid.is(FluidTags.WATER)
                && p.y <= pos.getY() + fluid.getHeight(level, pos);
    }

    private static boolean surface(ServerLevel level, Vec3 at) {
        return underwater(level, at.subtract(0, 0.06, 0))
                && !underwater(level, at.add(0, 0.06, 0))
                && level.getBlockState(BlockPos.containing(at.add(0, 0.06, 0)))
                        .getCollisionShape(level, BlockPos.containing(at.add(0, 0.06, 0))).isEmpty();
    }

    private static void wake(ServerLevel level, RandomSource rng, Vec3 start, Vec3 end,
                             Vec3 dir, boolean surfaceEntry) {
        double length = start.distanceTo(end);
        if (surfaceEntry) {
            double inset = Math.min(0.22, length * 0.5);
            start = start.add(dir.scale(inset));
            length -= inset;
        }
        if (length < 0.03) return;
        int bubbles = Mth.clamp((int) Math.round(length / TRAIL_SPACING), 1, 115);
        Vec3 spacing = end.subtract(start).scale(1.0 / bubbles);
        for (int i = 0; i < bubbles && remaining > 0; i++) {
            Vec3 point = start.add(spacing.scale(i + rng.nextDouble()));
            double jitter = 0.03 + rng.nextDouble() * 0.05;
            point = point.add((rng.nextDouble() - 0.5) * jitter,
                    (rng.nextDouble() - 0.5) * jitter,
                    (rng.nextDouble() - 0.5) * jitter);
            if (underwater(level, point)) {
                spawn(level, ModParticles.WATER_BUBBLE.get(), point,
                        dir.x * 0.2, dir.y * 0.1, dir.z * 0.2);
            }
        }
    }

    private static void splash(ServerLevel level, RandomSource rng, Vec3 at,
                               Vec3 direction, boolean entering, double traveled) {
        double force = Mth.clamp(traveled / 10.0, 0.6, 1.7);
        for (int i = 0; i < (entering ? 8 : 5) && remaining > 0; i++) {
            double angle = rng.nextDouble() * (Math.PI * 2.0);
            double speed = (0.045 + rng.nextDouble() * 0.08) * force;
            Vec3 velocity = new Vec3(Math.cos(angle) * speed,
                    0.09 + rng.nextDouble() * 0.12 + Math.abs(direction.y) * 0.04,
                    Math.sin(angle) * speed);
            spawn(level, ModParticles.WATER_DROP.get(), at.add(0, 0.025, 0),
                    velocity.x, velocity.y, velocity.z);
        }
        for (int i = 0; i < 4 && remaining > 0; i++) {
            spawn(level, ModParticles.WATER_PUFF.get(), at.add(0, 0.01, 0),
                    (rng.nextDouble() - 0.5) * 0.05,
                    0.015 + rng.nextDouble() * 0.025,
                    (rng.nextDouble() - 0.5) * 0.05);
        }
        ring(level, at, 0.12 * force, 10);
        if (PENDING.size() < MAX_PENDING_WAVES) {
            PENDING.addLast(new Wave(level, at, 0.22 * force, 12, level.getGameTime() + 2));
            PENDING.addLast(new Wave(level, at, 0.34 * force, 14, level.getGameTime() + 4));
        }
        if (sounds++ < MAX_SOUNDS_PER_TICK) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_SPLASH,
                    SoundSource.PLAYERS, (float) (0.25 * force),
                    (float) (1.15 + rng.nextDouble() * 0.2));
        }
    }

    private static void ring(ServerLevel level, Vec3 at, double radius, int count) {
        for (int i = 0; i < count && remaining > 0; i++) {
            double a = i * (Math.PI * 2 / count);
            Vec3 pos = at.add(Math.cos(a) * radius, 0.014, Math.sin(a) * radius);
            spawn(level, ModParticles.WATER_PUFF.get(), pos,
                    Math.cos(a) * 0.009, 0.002, Math.sin(a) * 0.009);
        }
    }

    private static void spawn(ServerLevel level, ParticleOptions particle, Vec3 at,
                              double dx, double dy, double dz) {
        if (remaining-- <= 0) return;
        if (!level.hasChunkAt(BlockPos.containing(at))) return;
        level.sendParticles(particle, at.x, at.y, at.z, 0, dx, dy, dz, 1);
    }

    private record Wave(ServerLevel level, Vec3 position, double radius, int count, long atTick) {}
}
