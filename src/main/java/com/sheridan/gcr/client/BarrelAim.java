package com.sheridan.gcr.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.events.RenderEvents;
import com.sheridan.gcr.network.c2s.GunFirePacket;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * First-person shot solution taken from the {@code MUZZLE_FLASH} bone.
 *
 * <p>The camera and the barrel are separate. The center dot is where the camera looks. The
 * tethered ring is the barrel ray: shots leave the muzzle along that direction, and the ring
 * sits on the first surface that ray hits. The viewmodel already carries the gun's lag, sway,
 * and recoil, so the ring is a readout of that muzzle rather than a second aim spring.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class BarrelAim {
    private static final double BLOCK_RANGE = 192.0;
    private static final double ENTITY_RANGE = 64.0;
    /** Used when the barrel ray hits nothing, so the ring still shows barrel direction. */
    private static final double SKY_RANGE = 48.0;
    private static final long STALE_NS = 250_000_000L;

    private static final Vector3f localPos = new Vector3f();
    private static final Vector3f localDir = new Vector3f();
    private static boolean captured;
    private static long capturedAt;
    /** Most negative view-space Z wins, so a suppressor tip beats the barrel behind it. */
    private static float bestForwardZ = Float.POSITIVE_INFINITY;

    public static boolean aimValid;
    public static Vec3 origin = Vec3.ZERO;
    public static Vec3 direction = new Vec3(0, 0, 1);
    /** Barrel ray hit a block or entity, so {@link #ringPoint} is on that surface. */
    public static boolean hasSurface;
    /**
     * The barrel hit something the camera ray does not — a wall edge, a teammate, anything
     * between the muzzle and the camera's target.
     */
    public static boolean blocked;
    public static Vec3 ringPoint = Vec3.ZERO;
    public static boolean projectOk;
    public static float ndcX;
    public static float ndcY;

    private BarrelAim() {}

    /** Opens a new pass so each first-person draw picks the furthest muzzle bone again. */
    public static void beginCaptureFrame() {
        bestForwardZ = Float.POSITIVE_INFINITY;
    }

    /**
     * Stores the muzzle bone in the same camera-local space the laser uses.
     * Called while the first-person gun is being drawn, once the bone pose is current.
     */
    public static void capture(PoseStack.Pose bonePose) {
        float fov = (float) RenderEvents.currentFov;
        float fovScale = (float) (Math.tan(Math.toRadians(fov * 0.5)) / Math.tan(Math.toRadians(35.0)));
        if (!Float.isFinite(fovScale) || fovScale < 1.0e-3f) {
            fovScale = 1f;
        }
        Vector3f pos = bonePose.pose().getTranslation(new Vector3f());
        Vector3f dir = bonePose.pose().transformDirection(new Vector3f(0f, 0f, -1f));
        if (dir.lengthSquared() < 1.0e-8f) {
            return;
        }
        dir.normalize();
        dir.mul(fovScale, fovScale, 1f);
        // Match LaserEffectRenderer: viewmodel units become camera-space blocks via 0.25 * fov.
        // Iris already scales the pose stack by 0.25, so that factor is not applied twice.
        float unit = Client.isUsingIrisShader ? fovScale : 0.25f * fovScale;
        pos.mul(unit, unit, unit);
        if (pos.z > bestForwardZ) {
            return;
        }
        bestForwardZ = pos.z;
        localPos.set(pos);
        localDir.set(dir);
        captured = true;
        capturedAt = System.nanoTime();
    }

    public static void solve() {
        aimValid = false;
        hasSurface = false;
        blocked = false;
        projectOk = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameRenderer == null) {
            return;
        }
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }
        if (!Client.WEAPON_STATUS.isHoldingGun()) {
            return;
        }
        if (!captured || System.nanoTime() - capturedAt > STALE_NS) {
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vector3f worldOff = new Vector3f(localPos).rotate(camera.rotation());
        Vector3f worldFwd = new Vector3f(localDir).rotate(camera.rotation());
        if (worldFwd.lengthSquared() < 1.0e-6f) {
            return;
        }
        worldFwd.normalize();

        Vec3 camPos = camera.getPosition();
        Vec3 muzzle = camPos.add(worldOff.x, worldOff.y, worldOff.z);
        if (!Double.isFinite(muzzle.x) || !Double.isFinite(muzzle.y) || !Double.isFinite(muzzle.z)) {
            return;
        }
        Vec3 forward = new Vec3(worldFwd.x, worldFwd.y, worldFwd.z);
        if (forward.lengthSqr() < 1.0e-6 || !Double.isFinite(forward.x) || !Double.isFinite(forward.y) || !Double.isFinite(forward.z)) {
            return;
        }
        forward = forward.normalize();

        Vec3 barrelHit = firstHit(mc.level, mc.player, muzzle, muzzle.add(forward.scale(BLOCK_RANGE)), ENTITY_RANGE);
        if (barrelHit != null) {
            hasSurface = true;
            ringPoint = barrelHit;
            Vec3 look = Vec3.directionFromRotation(camera.getXRot(), camera.getYRot());
            Vec3 lookHit = firstHit(mc.level, mc.player, camPos, camPos.add(look.scale(BLOCK_RANGE)), ENTITY_RANGE);
            double barrelDist = muzzle.distanceTo(barrelHit);
            double lookDist = lookHit == null ? BLOCK_RANGE : camPos.distanceTo(lookHit);
            blocked = barrelDist + 0.4 < lookDist;
        } else {
            ringPoint = muzzle.add(forward.scale(SKY_RANGE));
        }

        origin = muzzle;
        direction = forward;
        aimValid = true;
    }

    /** Projects {@link #ringPoint} to NDC. Hidden when the point is behind the camera. */
    public static void project(Matrix4f modelView, Matrix4f projection, Vec3 camPos) {
        projectOk = false;
        if (!aimValid) {
            return;
        }
        Vector4f p = new Vector4f(
                (float) (ringPoint.x - camPos.x),
                (float) (ringPoint.y - camPos.y),
                (float) (ringPoint.z - camPos.z),
                1f);
        modelView.transform(p);
        projection.transform(p);
        if (p.w() <= 0.05f) {
            return;
        }
        float x = p.x() / p.w();
        float y = p.y() / p.w();
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            return;
        }
        ndcX = x;
        ndcY = y;
        projectOk = true;
    }

    public static void apply(GunFirePacket packet) {
        solve();
        if (aimValid) {
            packet.setBarrelAim(origin, direction);
        }
    }

    @Nullable
    private static Vec3 firstHit(Level level, Entity shooter, Vec3 start, Vec3 end, double entityRange) {
        Vec3 delta = end.subtract(start);
        double segLen = delta.length();
        if (segLen < 1.0e-4) {
            return null;
        }
        BlockHitResult blockHit = level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter) {
            @Override
            public VoxelShape getBlockShape(BlockState state, BlockGetter blockGetter, BlockPos pos) {
                if (state.is(BlockTags.LEAVES)) {
                    return Shapes.empty();
                }
                return super.getBlockShape(state, blockGetter, pos);
            }
        });
        Vec3 blockPos = blockHit.getType() == HitResult.Type.MISS ? null : blockHit.getLocation();
        double bestDist2 = blockPos != null ? start.distanceToSqr(blockPos) : Double.POSITIVE_INFINITY;

        double entityLimit = Math.min(Math.max(entityRange, 0.0), segLen);
        Vec3 entityEnd = start.add(delta.scale(entityLimit / segLen));
        if (blockPos != null && bestDist2 <= entityLimit * entityLimit) {
            entityEnd = blockPos;
        }
        Vec3 best = blockPos;
        AABB box = new AABB(start, entityEnd).inflate(1.0);
        for (Entity entity : level.getEntities(shooter, box, e -> !e.isSpectator() && e.isPickable() && e.isAlive())) {
            var hit = entity.getBoundingBox().inflate(entity.getPickRadius()).clip(start, entityEnd);
            if (hit.isPresent()) {
                double dist2 = start.distanceToSqr(hit.get());
                if (dist2 < bestDist2) {
                    bestDist2 = dist2;
                    best = hit.get();
                }
            }
        }
        return best;
    }
}
