package com.zzynes.fly_drone;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class fpv_drone<T extends Entity> extends EntityModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation(FlyDroneMod.MOD_ID, "fpv_drone"), "main");

	private final ModelPart body;
	private final ModelPart propeller_rf;
	private final ModelPart propeller_lf;
	private final ModelPart propeller_rb;
	private final ModelPart propeller_lb;

	public fpv_drone(ModelPart root) {
		this.body = root.getChild("body");
		this.propeller_rf = root.getChild("propeller_rf");
		this.propeller_lf = root.getChild("propeller_lf");
		this.propeller_rb = root.getChild("propeller_rb");
		this.propeller_lb = root.getChild("propeller_lb");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-10.5F, 1.3F, -2.5F, 24.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(34, 40).addBox(3.5F, -1.7F, -2.5F, 10.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(10, 56).addBox(-12.5F, -2.7F, 1.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(6, 56).addBox(-12.5F, -2.7F, -2.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(0, 6).addBox(-10.5F, -3.7F, -2.5F, 24.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(52, 59).addBox(12.5F, -2.7F, 1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(28, 58).addBox(12.5F, -2.7F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(20, 56).addBox(-12.5F, 1.3F, 1.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(28, 46).addBox(-12.5F, 1.3F, -2.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(26, 56).addBox(-12.5F, -3.7F, 1.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(54, 50).addBox(-12.5F, -3.7F, -2.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(36, 54).addBox(-9.5F, 0.3F, 0.5F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(24, 53).addBox(-9.5F, 0.3F, -2.5F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(56, 59).addBox(3.5F, -2.7F, 1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(0, 59).addBox(3.5F, -2.7F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(60, 12).addBox(1.5F, 0.3F, 1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(14, 59).addBox(1.5F, 0.3F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(0, 40).addBox(-9.5F, -0.7F, -2.5F, 12.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(52, 52).addBox(-9.5F, -2.7F, 0.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(58, 9).addBox(1.5F, -2.7F, 1.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(36, 50).addBox(-9.5F, -2.7F, -2.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(52, 56).addBox(1.5F, -2.7F, -2.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(44, 50).addBox(-0.5F, 0.3F, -0.5F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(14, 56).addBox(-0.5F, 0.3F, 0.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 56).addBox(-0.5F, 0.3F, -2.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(58, 6).addBox(12.5F, 2.3F, 1.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(44, 56).addBox(12.5F, 2.3F, -2.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(0, 46).addBox(0.5F, 2.3F, -0.5F, 13.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(44, 59).addBox(12.5F, 2.3F, 0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(20, 58).addBox(12.5F, 2.3F, -1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(48, 56).addBox(-10.5F, 2.3F, -2.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(0, 48).addBox(-10.5F, 2.3F, -0.5F, 11.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(24, 58).addBox(-10.5F, 2.3F, -1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(56, 56).addBox(-10.5F, 2.3F, 1.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(24, 58).addBox(-10.5F, 2.3F, 0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 19.7F, 0.0F, 0.0F, -1.5708F, 0.0F));
		body.addOrReplaceChild("camera_r1", CubeListBuilder.create().texOffs(2, 1).addBox(0.5296F, -0.426F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-14.5F, -0.5F, 0.0F, 0.0F, 0.0F, -0.1309F));
		body.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(58, 0).addBox(0.0F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-14.9F, -1.7F, -10.8F, 0.0F, 0.6109F, 0.0F));
		body.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(40, 57).addBox(0.0F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-15.5F, -1.7F, 11.6F, 0.0F, -0.6109F, 0.0F));
		body.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(36, 57).addBox(0.0F, -2.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.1F, -1.7F, 11.4F, 0.0F, 0.6109F, 0.0F));
		body.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(58, 3).addBox(0.0F, -2.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.1F, -1.7F, -11.4F, 0.0F, 0.9599F, 0.0F));
		body.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(44, 52).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-14.8093F, -0.7F, -11.5107F, 0.0F, 0.6109F, 0.0F));
		body.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(16, 52).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.8F, -0.7F, 11.5F, 0.0F, 0.6109F, 0.0F));
		body.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(8, 52).addBox(-1.0F, 2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-14.8093F, -4.7F, 11.5107F, 0.0F, -0.6109F, 0.0F));
		body.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(0, 52).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.8093F, -0.7F, -11.5107F, 0.0F, 0.9599F, 0.0F));
		body.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(0, 26).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.5F, 0.3F, -2.5F, 3.1416F, -0.6109F, -3.1416F));
		body.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(30, 26).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.5F, 0.3F, 2.5F, 0.0F, -0.6109F, 0.0F));
		body.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(30, 12).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(12.5F, 0.3F, 2.5F, 0.0F, 0.6109F, 0.0F));
		body.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(0, 12).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(12.5F, 0.3F, -2.5F, 0.0F, 2.5307F, 0.0F));
		body.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(24, 48).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.5F, 0.3F, -0.5F, 0.0F, 0.0F, -0.1309F));

		PartDefinition propeller_rf = partdefinition.addOrReplaceChild("propeller_rf", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		propeller_rf.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(48, 48).addBox(-2.5F, -1.0F, -0.5F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.6F, -8.0F, 18.7F, 0.0F, 0.9599F, 0.0F));

		PartDefinition propeller_lf = partdefinition.addOrReplaceChild("propeller_lf", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		propeller_lf.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(36, 48).addBox(-2.5F, -1.0F, -0.5F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.6F, -8.0F, 18.7F, 0.0F, 2.1817F, 0.0F));

		PartDefinition propeller_rb = partdefinition.addOrReplaceChild("propeller_rb", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		propeller_rb.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(12, 50).addBox(-2.5F, -1.0F, -0.5F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.4F, -8.0F, -14.7F, 0.0F, -2.1729F, 0.0F));

		PartDefinition propeller_lb = partdefinition.addOrReplaceChild("propeller_lb", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		propeller_lb.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(0, 50).addBox(-2.5F, -1.0F, -0.5F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.5F, -8.0F, -14.8F, 0.0F, 2.1817F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		float speed = ageInTicks * 1.5F;
		this.propeller_rf.yRot = speed;
		this.propeller_lf.yRot = -speed;
		this.propeller_rb.yRot = -speed;
		this.propeller_lb.yRot = speed;
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		propeller_rf.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		propeller_lf.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		propeller_rb.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		propeller_lb.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
