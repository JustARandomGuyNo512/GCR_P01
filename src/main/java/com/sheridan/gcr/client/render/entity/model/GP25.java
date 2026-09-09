package com.sheridan.gcr.client.render.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.gcr.GCR;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class GP25<T extends Entity> extends EntityModel<T> {
    public static final ResourceLocation TEXTURE = GCR.RL("textures/entity/gp25.png");
    public static final GP25<?> INSTANCE = new GP25<>(GP25.createBodyLayer().bakeRoot());
    private final ModelPart root;

    public GP25(ModelPart root) {
        this.root = root.getChild("root2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root2 = partdefinition.addOrReplaceChild("root2", CubeListBuilder.create().texOffs(0, 20).addBox(-1.2452F, -3.0F, -4.8775F, 2.4855F, 6.0F, 6.01F, new CubeDeformation(0.0F))
                .texOffs(0, 12).addBox(-3.0022F, -1.2425F, -4.8775F, 6.0F, 2.4855F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-1.2452F, -1.2425F, -6.6351F, 2.4855F, 2.4855F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0, 0.0F));

        PartDefinition cube_r1 = root2.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(8, 32).addBox(-2.4855F, -2.4855F, -4.4F, 2.4855F, 2.4855F, 2.4855F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.3515F, 1.243F, -3.5238F, 0.0F, 0.7854F, 0.0F));

        PartDefinition cube_r2 = root2.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 32).addBox(0.0F, -2.4855F, -4.4F, 2.4855F, 2.4855F, 2.4855F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.3565F, 1.243F, -3.5238F, 0.0F, -0.7854F, 0.0F));

        PartDefinition cube_r3 = root2.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(24, 16).addBox(-1.6425F, 0.0F, -1.0F, 2.4855F, 2.4855F, 2.4855F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.3972F, 0.1926F, -4.1704F, -0.7854F, 0.0F, 0.0F));

        PartDefinition cube_r4 = root2.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(24, 12).addBox(-1.6425F, -2.4855F, -1.0F, 2.4855F, 2.4855F, 2.4855F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.3972F, -0.1921F, -4.1704F, 0.7854F, 0.0F, 0.0F));

        PartDefinition cube_r5 = root2.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(24, 0).addBox(-1.4855F, -6.0F, -1.0F, 2.4855F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.2907F, 1.9501F, -3.8775F, 0.0F, 0.0F, -0.7854F));

        PartDefinition cube_r6 = root2.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(16, 20).addBox(-1.4855F, -6.0F, -1.0F, 2.4855F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.952F, 2.293F, -3.8775F, 0.0F, 0.0F, 0.7854F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(@NotNull Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }

    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutout(TEXTURE));
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay);
    }


    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer,  int packedLight, int packedOverlay, int color) {
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay);
    }
}