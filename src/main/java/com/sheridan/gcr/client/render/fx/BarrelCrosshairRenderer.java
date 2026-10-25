package com.sheridan.gcr.client.render.fx;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.BarrelAim;
import com.sheridan.gcr.client.WeaponStatus;
import com.sheridan.gcr.client.render.delayed.Stage;
import com.sheridan.gcr.client.render.delayed.Task;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Camera dot at screen center, spread bars around it, and a hollow ring projected from the barrel.
 * The ring is where the shot will go. Aiming down sights hides it, including iron sights.
 */
@OnlyIn(Dist.CLIENT)
public final class BarrelCrosshairRenderer {
    /** Radius in GUI pixels, matching the earlier ring. The stroke stays 1 screen pixel. */
    private static final int RING_RADIUS = 3;
    private static final int MARK = 0xFFFFFFFF;
    private static final int MARK_BLOCKED = 0xFFFFCC66;

    static {
        Stage.HIGH.addTask(new Task(event -> renderWorld()).forever());
    }

    private BarrelCrosshairRenderer() {}

    /** Loads the class so the world-render task in the static block is registered. */
    public static void init() {}

    private static void renderWorld() {
        BarrelAim.solve();
        if (!BarrelAim.aimValid || !shouldDraw()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        BarrelAim.project(LaserEffectRenderer.modelViewMat, LaserEffectRenderer.projectionMat, camera.getPosition());
    }

    public static void drawHud(GuiGraphics graphics) {
        if (!shouldDraw()) {
            return;
        }
        var window = Minecraft.getInstance().getWindow();
        float guiScale = (float) window.getGuiScale();
        int screenW = window.getWidth();
        int screenH = window.getHeight();
        graphics.pose().pushPose();
        graphics.pose().scale(1f / guiScale, 1f / guiScale, 1f);

        int px = Math.max(1, Math.round(guiScale));
        int cx = screenW / 2;
        int cy = screenH / 2;
        drawCenter(graphics, cx, cy, px);
        if (BarrelAim.projectOk) {
            float sx = (BarrelAim.ndcX * 0.5f + 0.5f) * screenW;
            float sy = (1f - (BarrelAim.ndcY * 0.5f + 0.5f)) * screenH;
            int radius = RING_RADIUS * px;
            int margin = radius + 2;
            int x = Mth.clamp(Math.round(sx), margin, screenW - margin);
            int y = Mth.clamp(Math.round(sy), margin, screenH - margin);
            drawRing(graphics, x, y, radius, BarrelAim.blocked ? MARK_BLOCKED : MARK);
        }
        graphics.pose().popPose();
    }

    private static void drawCenter(GuiGraphics graphics, int cx, int cy, int px) {
        float spread = 0f;
        WeaponStatus status = Client.WEAPON_STATUS;
        IGun gun = status.getGun();
        ItemStack stack = status.getItemStack();
        if (gun != null && stack != null && !stack.isEmpty()) {
            spread = Math.max(0f, gun.getSpread(stack));
        }
        int gap = 2 * px + Math.round(spread * 6f * px);
        int arm = 2 * px;
        int y0 = cy - px / 2;
        int y1 = y0 + px;
        graphics.fill(cx - gap - arm, y0, cx - gap, y1, MARK);
        graphics.fill(cx + gap, y0, cx + gap + arm, y1, MARK);
        int d0 = cx - px / 2;
        int d1 = cy - px / 2;
        graphics.fill(d0, d1, d0 + px, d1 + px, MARK);
    }

    private static void drawRing(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (Math.abs(Mth.sqrt(dx * dx + dy * dy) - radius) <= 0.5f) {
                    graphics.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
                }
            }
        }
    }

    private static boolean shouldDraw() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.options.hideGui) {
            return false;
        }
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            return false;
        }
        WeaponStatus status = Client.WEAPON_STATUS;
        if (!status.isHoldingGun()) {
            return false;
        }
        return status.getAimingProgress() <= 0.45f;
    }
}
