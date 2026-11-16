package com.eternalsnow.client;

import com.eternalsnow.EternalSnowMod;
import com.eternalsnow.entity.FestiveCopperGolemEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public final class FestiveCopperGolemModel extends HierarchicalModel<FestiveCopperGolemEntity> implements ArmedModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(EternalSnowMod.MODID, "festive_copper_golem"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart upperBody;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public FestiveCopperGolemModel(ModelPart root) {
        this.root = root;
        this.upperBody = root.getChild("upper_body");
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();

        partRoot.addOrReplaceChild(
                "lower_body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -10.0F, -6.0F, 12.0F, 10.0F, 12.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F)
        );

        partRoot.addOrReplaceChild(
                "upper_body",
                CubeListBuilder.create().texOffs(0, 22).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 9.0F, 10.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F)
        );

        PartDefinition head = partRoot.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(0, 41).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 5.0F, 0.0F)
        );

        head.addOrReplaceChild(
                "hat_brim",
                CubeListBuilder.create().texOffs(32, 41).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 2.0F, 9.0F),
                PartPose.ZERO
        );
        head.addOrReplaceChild(
                "hat_top",
                CubeListBuilder.create().texOffs(32, 53).addBox(-3.5F, -13.0F, -3.5F, 7.0F, 4.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.10F, 0.0F, 0.05F)
        );
        head.addOrReplaceChild(
                "pom_pom",
                CubeListBuilder.create().texOffs(48, 22).addBox(-1.5F, -15.5F, -2.5F, 3.0F, 3.0F, 3.0F),
                PartPose.ZERO
        );
        head.addOrReplaceChild(
                "copper_rod",
                CubeListBuilder.create().texOffs(56, 30).addBox(-1.0F, -17.0F, 1.5F, 2.0F, 4.0F, 2.0F),
                PartPose.ZERO
        );

        partRoot.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create().texOffs(40, 22).addBox(-3.0F, -1.0F, -1.5F, 3.0F, 10.0F, 3.0F),
                PartPose.offset(-5.0F, 6.5F, 0.0F)
        );

        partRoot.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(40, 22).mirror().addBox(0.0F, -1.0F, -1.5F, 3.0F, 10.0F, 3.0F),
                PartPose.offset(5.0F, 6.5F, 0.0F)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(
            FestiveCopperGolemEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);
        this.upperBody.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.22F;

        float walkSwing = Mth.cos(limbSwing * 0.6662F) * 0.85F * limbSwingAmount;
        boolean holdingItem = !entity.getMainHandItem().isEmpty();

        this.leftArm.xRot = -walkSwing * 0.7F;
        this.leftArm.zRot = -0.08F - Mth.cos(ageInTicks * 0.07F) * 0.04F;

        if (holdingItem) {
            this.rightArm.xRot = -0.65F + walkSwing * 0.35F;
            this.rightArm.zRot = 0.08F;
        } else {
            this.rightArm.xRot = walkSwing * 0.7F;
            this.rightArm.zRot = 0.08F + Mth.cos(ageInTicks * 0.07F) * 0.04F;
        }

        int swingTicks = entity.getSwingAnimTicks();
        if (swingTicks > 0) {
            float progress = (10.0F - swingTicks) / 10.0F;
            float scoopArc = Mth.sin(progress * (float) Math.PI);
            this.rightArm.xRot = -0.55F - scoopArc * 1.25F;
            this.leftArm.xRot = -0.25F - scoopArc * 0.65F;
            this.upperBody.xRot = scoopArc * 0.18F;
        } else {
            this.upperBody.xRot = 0.0F;
        }
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        ModelPart armPart = (arm == HumanoidArm.LEFT) ? this.leftArm : this.rightArm;
        armPart.translateAndRotate(poseStack);
        poseStack.translate(arm == HumanoidArm.LEFT ? 0.08F : -0.08F, 0.52F, -0.06F);
    }
}
