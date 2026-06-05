// Made with Blockbench 4.12.5
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class arms_java_render - Converted<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "arms_java_render_- converted"), "main");
	private final ModelPart root;
	private final ModelPart RSS;
	private final ModelPart RAS;
	private final ModelPart RS;
	private final ModelPart RA;
	private final ModelPart LSS;
	private final ModelPart LAS;
	private final ModelPart LS;
	private final ModelPart LA;

	public arms_java_render - Converted(ModelPart root) {
		this.root = root.getChild("root");
		this.RSS = this.root.getChild("RSS");
		this.RAS = this.root.getChild("RAS");
		this.RS = this.root.getChild("RS");
		this.RA = this.root.getChild("RA");
		this.LSS = this.root.getChild("LSS");
		this.LAS = this.root.getChild("LAS");
		this.LS = this.root.getChild("LS");
		this.LA = this.root.getChild("LA");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition RSS = root.addOrReplaceChild("RSS", CubeListBuilder.create().texOffs(192, 192).addBox(-6.0F, 0.0F, -8.0F, 12.0F, 48.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 48.0F, 0.0F, 3.1416F, 0.0F, 0.0F));

		PartDefinition RAS = root.addOrReplaceChild("RAS", CubeListBuilder.create().texOffs(160, 64).addBox(-6.0F, 0.0F, -8.0F, 12.0F, 48.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 48.0F, 0.0F, 3.1416F, 0.0F, 0.0F));

		PartDefinition RS = root.addOrReplaceChild("RS", CubeListBuilder.create().texOffs(192, 192).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 48.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 48.0F, 0.0F, 3.1416F, 0.0F, 0.0F));

		PartDefinition RA = root.addOrReplaceChild("RA", CubeListBuilder.create().texOffs(160, 64).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 48.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 48.0F, 0.0F, 3.1416F, 0.0F, 0.0F));

		PartDefinition LSS = root.addOrReplaceChild("LSS", CubeListBuilder.create().texOffs(160, 128).addBox(-6.0F, 0.0F, -8.0F, 12.0F, 48.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 48.0F, 0.0F, 3.1416F, 0.0F, 0.0F));

		PartDefinition LAS = root.addOrReplaceChild("LAS", CubeListBuilder.create().texOffs(128, 192).addBox(-6.0F, 0.0F, -8.0F, 12.0F, 48.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 48.0F, 0.0F, 3.1416F, 0.0F, 0.0F));

		PartDefinition LS = root.addOrReplaceChild("LS", CubeListBuilder.create().texOffs(160, 128).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 48.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 48.0F, 0.0F, 3.1416F, 0.0F, 0.0F));

		PartDefinition LA = root.addOrReplaceChild("LA", CubeListBuilder.create().texOffs(128, 192).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 48.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 48.0F, 0.0F, 3.1416F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}