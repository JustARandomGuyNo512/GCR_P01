package com.sheridan.gcr.client.model.playerArm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.client.model.modular.IArmHandlerModel;
import com.sheridan.gcr.client.model.playerArm.builder.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@Deprecated
@OnlyIn(Dist.CLIENT)
public class PlayerArmModel{
    public static final PlayerArmModel INSTANCE;

    static {
        INSTANCE = new PlayerArmModel(createBodyLayer().bakeRoot());
    }

    private final GModelPart RSS;
    private final GModelPart RAS;
    private final GModelPart RS;
    private final GModelPart RA;
    private final GModelPart LSS;
    private final GModelPart LAS;
    private final GModelPart LS;
    private final GModelPart LA;

    public PlayerArmModel(GModelPart root) {
        GModelPart root1 = root.getChild("root");
        this.RSS = root1.getChild("RSS");
        this.RAS = root1.getChild("RAS");
        this.RS = root1.getChild("RS");
        this.RA = root1.getChild("RA");
        this.LSS = root1.getChild("LSS");
        this.LAS = root1.getChild("LAS");
        this.LS = root1.getChild("LS");
        this.LA = root1.getChild("LA");
    }

    public static GLayerDefinition createBodyLayer() {
        GMeshDefinition meshDefinition = new GMeshDefinition();
        GPartDefinition partDefinition = meshDefinition.getRoot();

        GPartDefinition root = partDefinition.addOrReplaceChild("root", GCubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("RSS", GCubeListBuilder.create().texOffs(192, 192).addBox(-6.0F, 0.0F, -8.0F, 12.0F, 48.0F, 16.0F, new GCubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 48.0F, 0.0F, 3.1416F, 0.0F, 0.0F));
        root.addOrReplaceChild("RAS", GCubeListBuilder.create().texOffs(160, 64).addBox(-6.0F, 0.0F, -8.0F, 12.0F, 48.0F, 16.0F, new GCubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 48.0F, 0.0F, 3.1416F, 0.0F, 0.0F));
        root.addOrReplaceChild("RS", GCubeListBuilder.create().texOffs(192, 192).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 48.0F, 16.0F, new GCubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 48.0F, 0.0F, 3.1416F, 0.0F, 0.0F));
        root.addOrReplaceChild("RA", GCubeListBuilder.create().texOffs(160, 64).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 48.0F, 16.0F, new GCubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 48.0F, 0.0F, 3.1416F, 0.0F, 0.0F));
        root.addOrReplaceChild("LSS", GCubeListBuilder.create().texOffs(160, 128).addBox(-6.0F, 0.0F, -8.0F, 12.0F, 48.0F, 16.0F, new GCubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 48.0F, 0.0F, 3.1416F, 0.0F, 0.0F));
        root.addOrReplaceChild("LAS", GCubeListBuilder.create().texOffs(128, 192).addBox(-6.0F, 0.0F, -8.0F, 12.0F, 48.0F, 16.0F, new GCubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 48.0F, 0.0F, 3.1416F, 0.0F, 0.0F));
        root.addOrReplaceChild("LS", GCubeListBuilder.create().texOffs(160, 128).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 48.0F, 16.0F, new GCubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 48.0F, 0.0F, 3.1416F, 0.0F, 0.0F));
        root.addOrReplaceChild("LA", GCubeListBuilder.create().texOffs(128, 192).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 48.0F, 16.0F, new GCubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 48.0F, 0.0F, 3.1416F, 0.0F, 0.0F));
        return GLayerDefinition.create(meshDefinition, 256, 256);
    }

    public static boolean isPlayerModelSlim() {
        AbstractClientPlayer abstractClientPlayer = Minecraft.getInstance().player;
        if (abstractClientPlayer != null) {
            PlayerSkin skin = abstractClientPlayer.getSkin();
            return skin.model() == PlayerSkin.Model.SLIM;
        }
        return false;
    }

    public static PlayerSkin getPlayerSkin() {
        AbstractClientPlayer abstractClientPlayer = Minecraft.getInstance().player;
        if (abstractClientPlayer != null) {
            return abstractClientPlayer.getSkin();
        }
        return null;
    }

    private static final PoseStack POSE_LEFT = new PoseStack();
    private static final PoseStack POSE_RIGHT = new PoseStack();
    public void renderByPose(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, boolean rightHand, boolean slim) {
        if (rightHand) {
            Utils.overridePose(POSE_LEFT, pose);
            if (slim) {
                RAS.render(POSE_LEFT, buffer, packedLight, OverlayTexture.NO_OVERLAY);
                POSE_LEFT.scale(1.2f, 1f, 1.2f);
                RSS.render(POSE_LEFT, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            } else {
                RA.render(POSE_LEFT, buffer, packedLight, OverlayTexture.NO_OVERLAY);
                POSE_LEFT.scale(1.2f, 1f, 1.2f);
                RS.render(POSE_LEFT, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            }
        } else {
            Utils.overridePose(POSE_RIGHT, pose);
            if (slim) {
                LAS.render(POSE_RIGHT, buffer, packedLight, OverlayTexture.NO_OVERLAY);
                POSE_RIGHT.scale(1.2f, 1f, 1.2f);
                LSS.render(POSE_RIGHT, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            } else {
                LA.render(POSE_RIGHT, buffer, packedLight, OverlayTexture.NO_OVERLAY);
                POSE_RIGHT.scale(1.2f, 1f, 1.2f);
                LS.render(POSE_RIGHT, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            }
        }
    }

    public void render(IArmHandlerModel model, VertexConsumer buffer, int packedLight, boolean rightHand, boolean isPlayerModelSlim) {
        PoseStack.Pose pose = model.getPose(rightHand, isPlayerModelSlim);
        if (rightHand) {
            PoseStack stack = new PoseStack();
            Utils.overridePose(stack, pose);
            if (isPlayerModelSlim) {
                RAS.render(stack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
                stack.scale(1.2f, 1f, 1.2f);
                RSS.render(stack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            } else {
                RA.render(stack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
                stack.scale(1.2f, 1f, 1.2f);
                RS.render(stack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            }
        } else {
            PoseStack stack = new PoseStack();
            Utils.overridePose(stack, pose);
            if (isPlayerModelSlim) {
                LAS.render(stack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
                stack.scale(1.2f, 1f, 1.2f);
                LSS.render(stack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            } else {
                LA.render(stack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
                stack.scale(1.2f, 1f, 1.2f);
                LS.render(stack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            }
        }
    }

}
