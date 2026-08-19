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

public class rpg_snar<T extends Entity> extends EntityModel<T> {
	// Привязываем слой к ID нашего мода fly_drone
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation(FlyDroneMod.MOD_ID, "rpg_snar"), "main");

	private final ModelPart rpg;

	public rpg_snar(ModelPart root) {
		this.rpg = root.getChild("rpg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition rpg = partdefinition.addOrReplaceChild("rpg", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.5F, -20.0F, 2.0F, 2.0F, 42.0F, new CubeDeformation(0.0F))
				.texOffs(100, 84).addBox(-1.0F, -3.5F, -17.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(32, 79).addBox(1.0F, -1.5F, -5.0F, 3.0F, 2.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(48, 105).addBox(-1.0F, 0.5F, -17.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(104, 92).addBox(1.0F, -1.5F, -17.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(104, 100).addBox(-3.0F, -1.5F, -17.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(94, 108).addBox(-2.0F, -2.5F, -17.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(48, 113).addBox(1.0F, -2.5F, -17.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(68, 61).addBox(1.0F, -2.5F, -7.0F, 1.0F, 1.0F, 15.0F, new CubeDeformation(0.0F))
				.texOffs(0, 79).addBox(1.0F, 0.5F, -7.0F, 1.0F, 1.0F, 15.0F, new CubeDeformation(0.0F))
				.texOffs(72, 44).addBox(-2.0F, -2.5F, -7.0F, 1.0F, 1.0F, 15.0F, new CubeDeformation(0.0F))
				.texOffs(68, 77).addBox(-2.0F, 0.5F, -7.0F, 1.0F, 1.0F, 15.0F, new CubeDeformation(0.0F))
				.texOffs(56, 93).addBox(2.0F, -2.5F, -5.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(24, 104).addBox(2.0F, 0.5F, -5.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(0, 95).addBox(-3.0F, -2.5F, -5.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(100, 60).addBox(-3.0F, 0.5F, -5.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(32, 92).addBox(1.0F, -3.5F, -5.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(104, 41).addBox(1.0F, 1.5F, -5.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(80, 93).addBox(-2.0F, -3.5F, -5.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(100, 72).addBox(-2.0F, 1.5F, -5.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(108, 108).addBox(1.0F, 0.5F, -17.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(64, 112).addBox(-2.0F, 0.5F, -17.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(60, 79).addBox(-1.0F, -2.5F, -19.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(60, 85).addBox(-1.0F, -3.5F, -7.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(24, 95).addBox(-1.0F, 1.5F, -7.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(78, 113).addBox(2.0F, -1.5F, 6.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(114, 21).addBox(2.0F, -1.5F, 19.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(114, 25).addBox(-3.0F, -1.5F, 19.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(114, 17).addBox(2.0F, -1.5F, -7.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(84, 113).addBox(-3.0F, -1.5F, 6.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(114, 13).addBox(-3.0F, -1.5F, -7.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(88, 41).addBox(-1.0F, -3.5F, 6.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(96, 41).addBox(-1.0F, -3.5F, 19.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(60, 88).addBox(-1.0F, 1.5F, 6.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(24, 98).addBox(-1.0F, 1.5F, 19.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(104, 53).addBox(-1.0F, -2.5F, -11.0F, 2.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(0, 44).addBox(-1.0F, -2.5F, 6.0F, 2.0F, 1.0F, 16.0F, new CubeDeformation(0.0F))
				.texOffs(60, 82).addBox(-1.0F, 0.5F, -19.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(64, 105).addBox(-1.0F, 0.5F, -11.0F, 2.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(36, 44).addBox(-1.0F, 0.5F, 6.0F, 2.0F, 1.0F, 16.0F, new CubeDeformation(0.0F))
				.texOffs(14, 107).addBox(-2.0F, -1.5F, -19.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 107).addBox(-2.0F, -1.5F, -11.0F, 1.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(0, 61).addBox(-2.0F, -1.5F, 6.0F, 1.0F, 2.0F, 16.0F, new CubeDeformation(0.0F))
				.texOffs(14, 111).addBox(1.0F, -1.5F, -19.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(80, 105).addBox(1.0F, -1.5F, -11.0F, 1.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(34, 61).addBox(1.0F, -1.5F, 6.0F, 1.0F, 2.0F, 16.0F, new CubeDeformation(0.0F))
				.texOffs(60, 91).addBox(-0.5F, -1.0F, -20.3F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 23.5F, 5.0F));

		rpg.addOrReplaceChild("rpg_snar_r1", CubeListBuilder.create().texOffs(32, 79).addBox(-3.0F, -1.0F, -5.5F, 3.0F, 2.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.5F, 0.5F, 0.0F, 0.0F, -1.5708F));
		rpg.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(32, 79).addBox(-1.0F, -2.0F, -1.0F, 3.0F, 2.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -1.5F, -4.0F, 0.0F, 0.0F, 3.1416F));
		rpg.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(32, 79).addBox(-1.0F, -2.0F, -1.0F, 3.0F, 2.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 1.5F, -4.0F, 0.0F, 0.0F, 1.5708F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		// Здесь будет анимация снаряда в полёте, пока оставляем пустой
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		rpg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
