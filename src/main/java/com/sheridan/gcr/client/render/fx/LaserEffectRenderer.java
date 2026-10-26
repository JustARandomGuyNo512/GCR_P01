package com.sheridan.gcr.client.render.fx;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.events.RenderEvents;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.compat.IrisCompat;
import com.sheridan.gcr.compat.OwnMultipartHitFilter;
import com.sheridan.gcr.items.GunItem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * TaCZ Additions-style block-face laser dots. The gun model supplies the animated
 * emitter orientation; world raycasts and world RenderType draw the actual dot.
 */
@OnlyIn(Dist.CLIENT)
public final class LaserEffectRenderer {
    private static final RenderType DOT_TYPE = LaserDotType.make();
    private static final Map<String, NodeState> NODES = new HashMap<>();
    private static String lastIdentity = "";
    private static int lastModifyId = -1;
    private static final long EXPIRE_NANOS = 2_000_000_000L;

    private LaserEffectRenderer() {}

    public static float getHitLength(String id) {
        NodeState node = NODES.get(id);
        return node == null ? Float.NaN : node.hitLength;
    }

    public static void recordEffectCall(int color, String id, PoseStack.Pose emitterPose, ModuleRenderContext context) {
        if (IrisCompat.isRenderingShadowPass()) return;
        int modifyId = context.gun.getModifyID(context.itemStack);
        String identity = context.gun.getIdentityID(context.itemStack);
        if (!Objects.equals(lastIdentity, identity) || lastModifyId != modifyId) {
            NODES.clear();
            lastIdentity = identity;
            lastModifyId = modifyId;
        }

        Matrix4f pose = emitterPose.pose();
        float fov = (float) RenderEvents.currentFov;
        float fovScale = (float)(Math.tan(Math.toRadians(fov * 0.5)) / Math.tan(Math.toRadians(35.0)));
        Vector3f position = pose.getTranslation(new Vector3f())
                .mul(0.25f * fovScale, 0.25f * fovScale, 0.25f);
        Vector3f direction = pose.transformDirection(new Vector3f(0, 0, -1));
        direction.mul(fovScale, fovScale, 1f);
        if (!valid(direction) || direction.lengthSquared() < 0.0001f) return;

        NodeState node = NODES.computeIfAbsent(id, ignored -> new NodeState());
        node.localOrigin.set(position);
        node.localDirection.set(direction.normalize());
        node.color = color;
        node.lastRecorded = System.nanoTime();
    }

    @SubscribeEvent
    public static void renderEffect(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null
                || !mc.options.getCameraType().isFirstPerson()
                || !(mc.player.getMainHandItem().getItem() instanceof GunItem)
                || IrisCompat.isRenderingShadowPass()) {
            NODES.clear();
            return;
        }

        long now = System.nanoTime();
        NODES.values().removeIf(node -> now - node.lastRecorded > EXPIRE_NANOS);
        if (NODES.isEmpty()) return;

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        Quaternionf rotation = camera.rotation();
        Vector3f cameraForward = rotation.transform(new Vector3f(0, 0, -1));
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();
        VertexConsumer consumer = buffers.getBuffer(DOT_TYPE);
        boolean hasDots = false;

        for (NodeState node : NODES.values()) {
            Vector3f dir = new Vector3f(node.localDirection).rotate(rotation).normalize();
            if (!valid(dir) || dir.dot(cameraForward) < 0.25f) {
                // An invalid muzzle transform must not create backwards rays.
                dir.set(cameraForward);
            }
            Vector3f offset = new Vector3f(node.localOrigin).rotate(rotation);
            Vec3 rayStart = cameraPos.add(offset.x, offset.y, offset.z);
            Vec3 rayEnd = rayStart.add(dir.x * 64d, dir.y * 64d, dir.z * 64d);
            BlockHitResult hit = mc.level.clip(
                    new ClipContext(rayStart, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            Vec3 surface = hit.getLocation();
            Vec3 normal = hit.getType() == HitResult.Type.BLOCK
                    ? Vec3.atLowerCornerOf(hit.getDirection().getNormal()) : null;
            Vec3 entityEnd = hit.getType() == HitResult.Type.MISS ? rayEnd : surface;
            double closest = rayStart.distanceToSqr(entityEnd);
            AABB query = new AABB(rayStart, entityEnd).inflate(0.5);
            for (Entity entity : mc.level.getEntities(mc.player, query,
                    e -> !OwnMultipartHitFilter.isOwnerOrOwnedPart(e, mc.player)
                            && !e.isSpectator() && e.isPickable())) {
                Optional<Vec3> entityHit = entity.getBoundingBox().inflate(entity.getPickRadius())
                        .clip(rayStart, entityEnd);
                if (entityHit.isPresent()) {
                    double d2 = rayStart.distanceToSqr(entityHit.get());
                    if (d2 < closest) {
                        closest = d2;
                        surface = entityHit.get();
                        normal = new Vec3(-dir.x, -dir.y, -dir.z);
                    }
                }
            }
            if (normal == null) {
                node.hitLength = 64f;
                continue;
            }
            float distance = (float) rayStart.distanceTo(surface);
            node.hitLength = distance;
            if (distance < 0.25f) continue;
            drawDot(consumer, poseStack.last().pose(), surface, normal, cameraPos, node.color, distance);
            hasDots = true;
        }
        if (hasDots) buffers.endBatch(DOT_TYPE);
    }

    private static boolean valid(Vector3f direction) {
        return Float.isFinite(direction.x) && Float.isFinite(direction.y) && Float.isFinite(direction.z);
    }

    private static void drawDot(VertexConsumer consumer, Matrix4f pose, Vec3 hit, Vec3 normal,
                                Vec3 cameraPos, int color, float distance) {
        Vector3f n = normal.toVector3f().normalize();
        Vector3f axis = Math.abs(n.y) > 0.95f
                ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f tangent = axis.cross(n, new Vector3f()).normalize();
        Vector3f bitangent = n.cross(tangent, new Vector3f()).normalize();
        // Compact core, modest halo expansion over distance.
        float radius = Math.min(0.075f, 0.040f + distance * 0.0003f);
        tangent.mul(radius);
        bitangent.mul(radius);
        Vec3 renderPos = hit.add(normal.scale(0.006)).subtract(cameraPos);
        int red = (color >>> 16) & 255;
        int green = (color >>> 8) & 255;
        int blue = color & 255;
        // Colored laser halo with a brighter central pixel from the alpha texture.
        vertex(consumer, pose, renderPos, tangent, bitangent, -1,-1, 0,1, red,green,blue);
        vertex(consumer, pose, renderPos, tangent, bitangent, -1, 1, 0,0, red,green,blue);
        vertex(consumer, pose, renderPos, tangent, bitangent,  1, 1, 1,0, red,green,blue);
        vertex(consumer, pose, renderPos, tangent, bitangent,  1,-1, 1,1, red,green,blue);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Vec3 at, Vector3f x, Vector3f y,
                               int xDir, int yDir, float u, float v, int r, int g, int b) {
        consumer.vertex(pose,
                        (float)at.x + x.x*xDir + y.x*yDir,
                        (float)at.y + x.y*xDir + y.y*yDir,
                        (float)at.z + x.z*xDir + y.z*yDir)
                .uv(u, v)
                .color(Math.min(255, (int)(r * 1.8f)), Math.min(255, (int)(g * 1.8f)),
                       Math.min(255, (int)(b * 1.8f)), 255)
                .endVertex();
    }

    private static final class NodeState {
        final Vector3f localOrigin = new Vector3f();
        final Vector3f localDirection = new Vector3f(0, 0, -1);
        int color;
        float hitLength = Float.NaN;
        long lastRecorded;
    }

    /** Mirrors the custom world-pass RenderType used by TaCZ Additions. */
    private static final class LaserDotType extends RenderType {
        private LaserDotType() {
            super("gcr_laser_dot", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS,
                    256, false, true, () -> {}, () -> {});
        }

        static RenderType make() {
            return create("gcr_laser_dot", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS,
                    256, false, true, CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
                            .setTextureState(new TextureStateShard(GCR.RL("textures/particle/laser_dot.png"), false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                            .setWriteMaskState(COLOR_WRITE)
                            .setCullState(NO_CULL)
                            .createCompositeState(false));
        }
    }
}
