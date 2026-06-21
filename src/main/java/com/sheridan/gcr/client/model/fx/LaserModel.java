package com.sheridan.gcr.client.model.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LaserModel<T extends Entity> extends EntityModel<T> {

	private final ModelPart ray;
	private final ModelPart third_person;

	public LaserModel(ModelPart root) {
		this.ray = root.getChild("ray");
		this.third_person = root.getChild("third_person");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition ray = partdefinition.addOrReplaceChild("ray", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition cube_r1 = ray.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 2).addBox(-0.1F, -0.1F, -16.0F, 0.2F, 0.2F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7854F));

		PartDefinition third_person = partdefinition.addOrReplaceChild("third_person", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition cube_r2 = third_person.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(2, 4).addBox(-0.1F, -0.1F, -16.0F, 0.2F, 0.2F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7854F));

		return LayerDefinition.create(meshdefinition, 32, 32);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}


	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int i1, int i2) {

	}
}