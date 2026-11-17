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

        // 1. RIGHT HAND & MAGAZINE
        poseStack.pushPose();

        float xOffset = 0.22f;
        float yOffset = -0.36f;
        float zOffset = -0.48f;

        // Subtle tactile bounce on insert (around progress 0.45 - 0.65)
        float bounce = 0.0f;
        if (progress >= 0.45f && progress <= 0.65f) {
            float bT = (progress - 0.45f) / 0.20f;
            bounce = (float) Math.sin(bT * Math.PI) * 0.025f;
        }

        poseStack.translate(xOffset, yOffset - bounce, zOffset);
        poseStack.mulPose(Axis.YP.rotationDegrees(12.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(-35.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(10.0f));

        // Render right player arm
        playerRenderer.renderRightHand(poseStack, buffer, light, player);

        // Attached magazine in right hand
        poseStack.pushPose();
        poseStack.translate(-0.06f, 0.46f, -0.10f);
        poseStack.mulPose(Axis.XP.rotationDegrees(95.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(-15.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(10.0f));
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

        // 2. LEFT HAND & AMMO CARTRIDGE
        poseStack.pushPose();

        float lx, ly, lz;
        float lRotX, lRotY, lRotZ;

        if (progress < 0.5f) {
            // Rising towards magazine feed lips
            float u = progress / 0.5f;
            float ease = (float) (0.5 - 0.5 * Math.cos(u * Math.PI));
            lx = Mth.lerp(ease, -0.38f, -0.04f);
            ly = Mth.lerp(ease, -0.68f, -0.30f);
            lz = Mth.lerp(ease, -0.40f, -0.46f);
            lRotX = Mth.lerp(ease, 10.0f, -38.0f);
            lRotY = Mth.lerp(ease, -20.0f, 25.0f);
            lRotZ = Mth.lerp(ease, 15.0f, -18.0f);
        } else if (progress < 0.65f) {
            // Pushing cartridge into magazine feed lips
            float pushT = (progress - 0.5f) / 0.15f;
            float push = (float) Math.sin(pushT * Math.PI) * 0.035f;
            lx = -0.04f;
            ly = -0.30f - push;
            lz = -0.46f;
            lRotX = -38.0f - push * 60.0f;
            lRotY = 25.0f;
            lRotZ = -18.0f;
        } else {
            // Returning to fetch next round
            float u = (progress - 0.65f) / 0.35f;
            float ease = (float) (0.5 - 0.5 * Math.cos(u * Math.PI));
            lx = Mth.lerp(ease, -0.04f, -0.38f);
            ly = Mth.lerp(ease, -0.30f, -0.68f);
            lz = Mth.lerp(ease, -0.46f, -0.40f);
            lRotX = Mth.lerp(ease, -38.0f, 10.0f);
            lRotY = Mth.lerp(ease, 25.0f, -20.0f);
            lRotZ = Mth.lerp(ease, -18.0f, 15.0f);
        }

        poseStack.translate(lx, ly, lz);
        poseStack.mulPose(Axis.YP.rotationDegrees(lRotY));
        poseStack.mulPose(Axis.XP.rotationDegrees(lRotX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(lRotZ));

        // Render left player arm
        playerRenderer.renderLeftHand(poseStack, buffer, light, player);

        // Render ammo cartridge in left fingers while not fully inserted
        if (!ammoStack.isEmpty() && progress < 0.60f) {
            poseStack.pushPose();
            poseStack.translate(0.04f, 0.48f, -0.08f);
            poseStack.mulPose(Axis.XP.rotationDegrees(85.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-25.0f));
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
