package com.sheridan.gcr.client.model.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.gcr.GCR;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class LaserModel<T extends Entity> extends EntityModel<T> {
	public static final LaserModel<?> INSTANCE = new LaserModel<>(createBodyLayer().bakeRoot());
	public static final ResourceLocation LASER_TEXTURE = GCR.RL("gcr", "textures/fx/laser.png");
	private final ModelPart first_person;
	private final ModelPart third_person;

	public LaserModel(ModelPart root) {
		this.first_person = root.getChild("first_person");
		this.third_person = root.getChild("third_person");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition first_person = partdefinition.addOrReplaceChild("first_person", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r1 = first_person.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -0.5F, -16.0F, 1.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7854F));

		PartDefinition third_person = partdefinition.addOrReplaceChild("third_person", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r2 = third_person.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 17).addBox(-0.5F, -0.5F, -16.0F, 1.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7854F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}


	@Override
	public void setupAnim(@NotNull Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	public void renderFirstPerson(PoseStack poseStack, VertexConsumer vertexConsumer, int color, float length) {
		poseStack.scale(0.2f, 0.2f, length);
		first_person.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
	}

	public void renderThirdPerson(PoseStack poseStack, VertexConsumer vertexConsumer, int color) {
		poseStack.scale(0.2f, 0.2f, 8);
		first_person.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
	}

	@Override
	public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int i, int i1, int i2) {

	}
}