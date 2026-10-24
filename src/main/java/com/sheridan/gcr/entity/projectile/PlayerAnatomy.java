package com.sheridan.gcr.entity.projectile;

import com.sheridan.gcr.config.PlayerHitZoneConfig;
import com.sheridan.gcr.config.PlayerHitZoneConfig.PlayerAnatomyZone;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Lightweight player-only combat rig inspired by AftermatchDayZ's anatomical
 * HEAD / CHEST / ABDOMEN / PELVIS / arms / thighs / shins layout.
 *
 * Uses rotated, ray-clipped sub-boxes with NO aim-assist margin. Every hit must
 * lie inside the actual Minecraft player bounding box; broadphase candidate
 * inflation in BulletEntity cannot cause a hit by itself.
 *
 * The volumes scale with the current player dimensions, including sneaking
 * and swimming. This does not attempt to synchronize render-only limb animations.
 */
public final class PlayerAnatomy {
    public enum Part {
        HEAD(PlayerAnatomyZone.HEAD),
        CHEST(PlayerAnatomyZone.TORSO),
        ABDOMEN(PlayerAnatomyZone.TORSO),
        PELVIS(PlayerAnatomyZone.TORSO),
        LEFT_UPPER_ARM(PlayerAnatomyZone.TORSO),
        RIGHT_UPPER_ARM(PlayerAnatomyZone.TORSO),
        LEFT_FOREARM(PlayerAnatomyZone.TORSO),
        RIGHT_FOREARM(PlayerAnatomyZone.TORSO),
        LEFT_HAND(PlayerAnatomyZone.TORSO),
        RIGHT_HAND(PlayerAnatomyZone.TORSO),
        LEFT_THIGH(PlayerAnatomyZone.LEGS),
        RIGHT_THIGH(PlayerAnatomyZone.LEGS),
        LEFT_SHIN(PlayerAnatomyZone.LEGS),
        RIGHT_SHIN(PlayerAnatomyZone.LEGS),
        LEFT_FOOT(PlayerAnatomyZone.FEET),
        RIGHT_FOOT(PlayerAnatomyZone.FEET);

        private final PlayerAnatomyZone zone;
        Part(PlayerAnatomyZone zone) { this.zone = zone; }
        public PlayerAnatomyZone zone() { return zone; }
    }

    public record Hit(Part part, Vec3 location, double segmentT) {}

    private PlayerAnatomy() {}

    public static Hit trace(Player player, Vec3 start, Vec3 end) {
        AABB box = player.getBoundingBox();
        if (!box.contains(start) && box.clip(start, end).isEmpty()) return null;

        // When disabled, retain strict ray-vs-vanilla-player-AABB behavior.
        if (!PlayerHitZoneConfig.ENABLED.get()) {
            Vec3 point = box.contains(start) ? start : box.clip(start, end).orElse(null);
            return point == null ? null : new Hit(Part.CHEST, point,
                    parameter(start, end, point));
        }

        double width = box.getXsize();
        double height = box.getYsize();
        double depth = box.getZsize();
        if (width < 0.05d || height < 0.05d || depth < 0.05d) return null;

        double yaw = Math.toRadians(player.getYRot());
        double cosine = Math.cos(yaw), sine = Math.sin(yaw);
        Vec3 center = box.getCenter();
        Vec3 from = local(start, center, box.minY, cosine, sine);
        Vec3 to = local(end, center, box.minY, cosine, sine);

        // The original model proportions are scaled to the player's actual
        // AABB. Local dimensions are expressed as fractions of that AABB.
        Hit nearest = null;
        nearest = test(nearest, Part.HEAD, 0, .865, -.015, .32, .125, .32,
                width, height, depth, start, end, from, to, center, box);
        nearest = test(nearest, Part.CHEST, 0, .66, 0, .32, .135, .22,
                width, height, depth, start, end, from, to, center, box);
        nearest = test(nearest, Part.ABDOMEN, 0, .475, 0, .30, .09, .21,
                width, height, depth, start, end, from, to, center, box);
        nearest = test(nearest, Part.PELVIS, 0, .355, 0, .29, .075, .21,
                width, height, depth, start, end, from, to, center, box);
        for (int side = -1; side <= 1; side += 2) {
            Part upper = side < 0 ? Part.LEFT_UPPER_ARM : Part.RIGHT_UPPER_ARM;
            Part forearm = side < 0 ? Part.LEFT_FOREARM : Part.RIGHT_FOREARM;
            Part hand = side < 0 ? Part.LEFT_HAND : Part.RIGHT_HAND;
            Part thigh = side < 0 ? Part.LEFT_THIGH : Part.RIGHT_THIGH;
            Part shin = side < 0 ? Part.LEFT_SHIN : Part.RIGHT_SHIN;
            Part foot = side < 0 ? Part.LEFT_FOOT : Part.RIGHT_FOOT;
            nearest = test(nearest, upper, side * .405, .635, 0, .09, .115, .19,
                    width, height, depth, start, end, from, to, center, box);
            nearest = test(nearest, forearm, side * .405, .475, -.02, .09, .09, .19,
                    width, height, depth, start, end, from, to, center, box);
            nearest = test(nearest, hand, side * .405, .36, -.04, .09, .06, .19,
                    width, height, depth, start, end, from, to, center, box);
            nearest = test(nearest, thigh, side * .215, .26, 0, .19, .105, .20,
                    width, height, depth, start, end, from, to, center, box);
            nearest = test(nearest, shin, side * .215, .135, 0, .18, .085, .19,
                    width, height, depth, start, end, from, to, center, box);
            nearest = test(nearest, foot, side * .215, .045, -.08, .19, .048, .28,
                    width, height, depth, start, end, from, to, center, box);
        }
        return nearest;
    }

    private static Vec3 local(Vec3 point, Vec3 center, double baseY, double c, double s) {
        double dx = point.x - center.x, dz = point.z - center.z;
        return new Vec3(dx * c + dz * s, point.y - baseY, -dx * s + dz * c);
    }

    private static Hit test(Hit nearest, Part part, double x, double y, double z,
                            double halfX, double halfY, double halfZ,
                            double width, double height, double depth,
                            Vec3 worldStart, Vec3 worldEnd, Vec3 from, Vec3 to,
                            Vec3 center, AABB outer) {
        double cx = x * width, cy = y * height, cz = z * depth;
        AABB oriented = new AABB(
                cx - halfX * width, cy - halfY * height, cz - halfZ * depth,
                cx + halfX * width, cy + halfY * height, cz + halfZ * depth);

        Vec3 localHit = oriented.contains(from)
                ? from : oriented.clip(from, to).orElse(null);
        if (localHit == null) return nearest;

        double t = parameter(from, to, localHit);
        if (t < 0d || t > 1d || (nearest != null && t >= nearest.segmentT())) return nearest;
        Vec3 worldHit = worldStart.add(worldEnd.subtract(worldStart).scale(t));
        // A rotated limb can protrude beyond the vanilla collision box.
        // The authoritative entity AABB is an absolute hard limit.
        if (!insideWithTolerance(outer, worldHit)) return nearest;
        return new Hit(part, worldHit, t);
    }

    private static boolean insideWithTolerance(AABB box, Vec3 p) {
        final double epsilon = 1.0e-7;
        return p.x >= box.minX - epsilon && p.x <= box.maxX + epsilon
                && p.y >= box.minY - epsilon && p.y <= box.maxY + epsilon
                && p.z >= box.minZ - epsilon && p.z <= box.maxZ + epsilon;
    }

    private static double parameter(Vec3 from, Vec3 to, Vec3 hit) {
        Vec3 delta = to.subtract(from);
        double magnitude = delta.lengthSqr();
        return magnitude < 1.0e-10 ? 0d : hit.subtract(from).dot(delta) / magnitude;
    }
}
