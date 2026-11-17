package com.sheridan.gcr.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sheridan.gcr.client.MagLoadingHandler;
import com.sheridan.gcr.items.AmmoItem;
import com.sheridan.gcr.items.MagItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MagLoadingRenderer {

    public static void render(PoseStack poseStack, MultiBufferSource buffer, int light, float partialTicks, LocalPlayer player) {
        if (!MagLoadingHandler.isLoading() || player == null) {
            return;
        }

        ItemStack magStack = MagLoadingHandler.getMagStack();
        if (magStack.isEmpty() || !(magStack.getItem() instanceof MagItem magItem)) {
            return;
        }

        ItemStack ammoStack = MagLoadingHandler.getAmmoStack();
        if (ammoStack.isEmpty()) {
            AmmoItem ai = AmmoItem.getByCaliber(magItem.getCaliber());
            if (ai != null) {
                ammoStack = new ItemStack(ai);
            }
        }

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        PlayerRenderer playerRenderer = (PlayerRenderer) dispatcher.getRenderer(player);

        int tick = MagLoadingHandler.getCurrentTick();
        int cycleTicks = MagLoadingHandler.CYCLE_TICKS;
        float progress = ((tick % cycleTicks) + partialTicks) / (float) cycleTicks;

        // 1. RIGHT ARM & MAGAZINE (Holding the magazine firmly in front of the chest)
        poseStack.pushPose();

        float rx = 0.20f;
        float ry = -0.34f;
        float rz = -0.46f;

        // Tactile downward recoil pulse when cartridge snaps into feed lips (progress 0.55 - 0.75)
        float bounce = 0.0f;
        if (progress >= 0.55f && progress <= 0.75f) {
            float bT = (progress - 0.55f) / 0.20f;
            bounce = (float) (Math.sin(bT * Math.PI) * Math.exp(-bT * 1.5)) * 0.032f;
        }

        poseStack.translate(rx, ry - bounce, rz + bounce * 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(8.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(-36.0f - bounce * 150.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(12.0f));

        // Render right arm
        playerRenderer.renderRightHand(poseStack, buffer, light, player);

        // Render magazine gripped in right hand with feed lips angled toward left hand
        poseStack.pushPose();
        poseStack.translate(-0.06f, 0.44f, -0.11f);
        poseStack.mulPose(Axis.XP.rotationDegrees(92.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(-18.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(12.0f));
        poseStack.scale(0.85f, 0.85f, 0.85f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                player,
                magStack,
                ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                false,
                poseStack,
                buffer,
                player.level(),
                light,
                OverlayTexture.NO_OVERLAY,
                0
        );
        poseStack.popPose();
        poseStack.popPose();

        // 2. LEFT ARM & AMMO CARTRIDGE (Picking up round, pushing into feed lips, snapping in, and resetting)
        poseStack.pushPose();

        float lx, ly, lz;
        float lRotX, lRotY, lRotZ;

        if (progress < 0.45f) {
            // Stage 1: Rising from pouch towards magazine feed lips
            float u = progress / 0.45f;
            float ease = 0.5f - 0.5f * (float) Math.cos(u * Math.PI);
            lx = Mth.lerp(ease, -0.42f, -0.05f);
            ly = Mth.lerp(ease, -0.72f, -0.28f);
            lz = Mth.lerp(ease, -0.38f, -0.46f);
            lRotX = Mth.lerp(ease, 15.0f, -42.0f);
            lRotY = Mth.lerp(ease, -25.0f, 22.0f);
            lRotZ = Mth.lerp(ease, 20.0f, -16.0f);
        } else if (progress < 0.60f) {
            // Stage 2: Thumb pressing downward and rearward against spring tension
            float pT = (progress - 0.45f) / 0.15f;
            float pushY = (float) Math.sin(pT * Math.PI * 0.5) * 0.038f;
            float pushZ = pT * 0.015f;
            lx = -0.05f + (pT * 0.01f);
            ly = -0.28f - pushY;
            lz = -0.46f + pushZ;
            lRotX = -42.0f - pushY * 80.0f;
            lRotY = 22.0f;
            lRotZ = -16.0f;
        } else {
            // Stage 3 & 4: Cartridge snapped in, left hand pulling back downward for next round
            float rT = (progress - 0.60f) / 0.40f;
            float rEase = 0.5f - 0.5f * (float) Math.cos(rT * Math.PI);
            float arcOut = (float) Math.sin(rT * Math.PI) * 0.04f;
            lx = Mth.lerp(rEase, -0.04f, -0.42f) - arcOut;
            ly = Mth.lerp(rEase, -0.31f, -0.72f);
            lz = Mth.lerp(rEase, -0.445f, -0.38f);
            lRotX = Mth.lerp(rEase, -45.0f, 15.0f);
            lRotY = Mth.lerp(rEase, 22.0f, -25.0f);
            lRotZ = Mth.lerp(rEase, -16.0f, 20.0f);
        }

        poseStack.translate(lx, ly, lz);
        poseStack.mulPose(Axis.YP.rotationDegrees(lRotY));
        poseStack.mulPose(Axis.XP.rotationDegrees(lRotX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(lRotZ));

        // Render left arm
        playerRenderer.renderLeftHand(poseStack, buffer, light, player);

        // Render 3D cartridge held between thumb and index finger until fully seated at tick 9 (progress ~0.58)
        if (!ammoStack.isEmpty() && progress < 0.58f) {
            poseStack.pushPose();
            poseStack.translate(0.05f, 0.45f, -0.08f);
            poseStack.mulPose(Axis.XP.rotationDegrees(82.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(-10.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-24.0f));
            poseStack.scale(0.55f, 0.55f, 0.55f);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    player,
                    ammoStack,
                    ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    true,
                    poseStack,
                    buffer,
                    player.level(),
                    light,
                    OverlayTexture.NO_OVERLAY,
                    0
            );
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
