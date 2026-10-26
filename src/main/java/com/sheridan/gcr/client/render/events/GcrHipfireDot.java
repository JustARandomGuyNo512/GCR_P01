package com.sheridan.gcr.client.render.events;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.events.RenderEvents;
import com.sheridan.gcr.client.render.AftermatchWeaponMotion;
import com.sheridan.gcr.client.recoil.IRecoilUpdater;
import com.sheridan.gcr.client.recoil.RecoilHandler;
import com.sheridan.gcr.compat.OwnMultipartHitFilter;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.fire.AftermatchWallTuck;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Project the GCR bullet's nominal no-spread path onto the HUD. */
public final class GcrHipfireDot {
    // Same 128x128 dot_1.png texture as TaCZ 1.1.8 (exact original bytes).
    // The 10px round dot lives in the center of a transparent 128px canvas.
    // TaCZ draws the WHOLE texture inside a 16x16 HUD quad; the actual
    // visible dot therefore occupies only ~1.25 GUI pixels, with soft edges.
    private static final ResourceLocation DOT_TEXTURE = GCR.RL("textures/gui/hipfire_dot.png");
    private static final int DOT_QUAD_SIZE = 28;

    private GcrHipfireDot() {}

    public static boolean draw(GuiGraphics g, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null || mc.options.hideGui
                || !(player.getMainHandItem().getItem() instanceof GunItem)) return false;
        // Stop the reticle immediately on RMB, even before the next ADS tick.
        // Keep it hidden until sight-lowering animation has substantially ended.
        if (Client.RIGHT_BUTTON_PRESSED.get() || Client.WEAPON_STATUS.isAiming()
                || Client.WEAPON_STATUS.getAimingProgress(tickDelta) > 0.025f) return false;
        // Near a wall, GCR may suppress firing, but while the weapon is held
        // outside ADS the dot still represents its current nominal direction.
        AftermatchWallTuck.State wall = AftermatchWallTuck.check(player, tickDelta);

        float yaw = player.getViewYRot(tickDelta);
        float pitch = player.getViewXRot(tickDelta) - wall.raiseDegrees();
        IRecoilUpdater recoil = RecoilHandler.INSTANCE.getRecoilUpdater();
        if (recoil != null) {
            yaw += recoil.getGunKickYaw();
            pitch += recoil.getGunKickPitch();
        }
        AftermatchWeaponMotion.ShotOffset inertia = AftermatchWeaponMotion.INSTANCE.shotOffset();
        yaw += inertia.yawDegrees();
        pitch += inertia.pitchDegrees();

        // Same eye position and angular sum as Gun.serverShoot, minus spread.
        Vec3 start = player.getEyePosition(tickDelta);
        Vec3 target = start.add(Vec3.directionFromRotation(pitch, yaw).scale(256d));
        BlockHitResult block = mc.level.clip(new ClipContext(start, target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = block.getType() == HitResult.Type.MISS ? target : block.getLocation();
        double closest = start.distanceToSqr(end);
        for (Entity e : mc.level.getEntities(player, new AABB(start, end).inflate(1d),
                entity -> entity.isAlive() && entity.isPickable()
                        && !OwnMultipartHitFilter.isOwnerOrOwnedPart(entity, player))) {
            AABB actual = e.getBoundingBox();
            Vec3 at = actual.contains(start) ? start : actual.clip(start, end).orElse(null);
            if (at == null) continue;
            double distance = start.distanceToSqr(at);
            if (distance < closest) {
                closest = distance;
                end = at;
            }
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 relative = end.subtract(camera.getPosition());
        Vector3f view = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z)
                .rotate(new Quaternionf(camera.rotation()).conjugate());
        // Minecraft 1.20.1 camera space is +Z-forward, unlike modern
        // -Z-forward OpenGL conventions (1.21+). Using -view.z here rejected
        // every valid hipfire hit as if it were behind the camera.
        final float forward = view.z;
        if (!Float.isFinite(forward) || forward <= 0.01f
                || !Float.isFinite(view.x) || !Float.isFinite(view.y)) {
            return false;
        }

        // Looking south (+Z), positive view-space X points screen LEFT.
        // Reverse X for the 1.20.1 camera's +Z-forward handedness.
        double fov = Math.max(15d, Math.min(155d, RenderEvents.currentFov));
        double focal = g.guiHeight() * 0.5d / Math.tan(Math.toRadians(fov * 0.5d));
        double screenX = g.guiWidth() * 0.5d - view.x * focal / forward;
        double screenY = g.guiHeight() * 0.5d - view.y * focal / forward;
        if (!Double.isFinite(screenX) || !Double.isFinite(screenY)) return false;
        int x = (int) Math.round(screenX);
        int y = (int) Math.round(screenY);
        if (x < 2 || y < 2 || x >= g.guiWidth() - 2 || y >= g.guiHeight() - 2) {
            return false;
        }

        // Match TaCZ's DOT_1: preserve the entire alpha-padded sprite,
        // normal alpha blending and 90% opacity. A 16x16 quad does NOT
        // mean a 16px dot; the visible 10/128 fraction is ~1.25 GUI px.
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1f, 1f, 1f, 0.9f);
        try {
            g.blit(DOT_TEXTURE, x - DOT_QUAD_SIZE / 2, y - DOT_QUAD_SIZE / 2,
                    0f, 0f, DOT_QUAD_SIZE, DOT_QUAD_SIZE,
                    DOT_QUAD_SIZE, DOT_QUAD_SIZE);
            // GuiGraphics batches vertices; flush BEFORE restoring alpha so
            // the queued dot is actually shaded with TaCZ's 0.9 opacity.
            g.flush();
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
        return true;
    }
}
