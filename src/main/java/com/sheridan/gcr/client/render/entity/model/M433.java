package com.sheridan.gcr.client.render.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.gcr.GCR;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class M433<T extends Entity> extends EntityModel<T> {
    public static final ResourceLocation TEXTURE = GCR.RL("textures/entity/m433.png");
    public static final M433<?> INSTANCE = new M433<>(M433.createBodyLayer().bakeRoot());
    private final ModelPart root;

    public M433(ModelPart root) {
        this.root = root.getChild("root");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create().texOffs(16, 8).addBox(-1.2428F, -3.0F, -2.6F, 2.4855F, 6.0F, 6.01F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-3.0003F, -1.2425F, -2.6F, 6.0F, 2.4855F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(16, 20).addBox(-1.2452F, -1.2425F, -6.6351F, 2.4855F, 2.4855F, 4.4F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0f, 0.0F));

        PartDefinition cube_r1 = root.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(16, 26).addBox(-2.4855F, -2.4855F, -4.4F, 2.4855F, 2.4855F, 4.4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.9997F, 1.243F, -2.6F, 0.0F, 0.4102F, 0.0F));

        PartDefinition cube_r2 = root.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(28, 20).addBox(0.0F, -2.4855F, -4.4F, 2.4855F, 2.4855F, 4.4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.9997F, 1.243F, -2.6F, 0.0F, -0.4102F, 0.0F));

        PartDefinition cube_r3 = root.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(28, 26).addBox(-1.6425F, 0.0F, -1.0F, 2.4855F, 2.4855F, 4.4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.3972F, -0.6381F, -4.7269F, -0.4102F, 0.0F, 0.0F));

        PartDefinition cube_r4 = root.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(24, 0).addBox(-1.6425F, -2.4855F, -1.0F, 2.4855F, 2.4855F, 4.4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.3972F, 0.6381F, -4.7269F, 0.4102F, 0.0F, 0.0F));

        PartDefinition cube_r5 = root.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 20).addBox(-1.4855F, -6.0F, -1.0F, 2.4855F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.2928F, 1.9497F, -1.6F, 0.0F, 0.0F, -0.7854F));

        PartDefinition cube_r6 = root.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 8).addBox(-1.4855F, -6.0F, -1.0F, 2.4855F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.9499F, 2.2934F, -1.6F, 0.0F, 0.0F, 0.7854F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(@NotNull Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer,  int packedLight, int packedOverlay, int color) {
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay);
    }
}