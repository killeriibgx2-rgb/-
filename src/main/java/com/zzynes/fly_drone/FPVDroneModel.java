package com.zzynes.fly_drone;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class FPVDroneModel extends EntityModel<FPVDroneEntity> {
    private final ModelPart root;
    private ModelPart propellerLf = null;
    private ModelPart propellerRf = null;
    private ModelPart propellerLb = null;
    private ModelPart propellerRb = null;

    public FPVDroneModel(ModelPart root) {
        this.root = root;

        // БЕЗОПАСНОСТЬ: Ищем кости пропеллеров в твоей модели Blockbench.
        // Если имена не совпадают, игра больше НЕ будет крашиться!
        try { this.propellerLf = root.getChild("propeller_lf"); } catch (Exception e) {}
        try { this.propellerRf = root.getChild("propeller_rf"); } catch (Exception e) {}
        try { this.propellerLb = root.getChild("propeller_lb"); } catch (Exception e) {}
        try { this.propellerRb = root.getChild("propeller_rb"); } catch (Exception e) {}
    }

    @Override
    public void setupAnim(FPVDroneEntity drone, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Если моторы заведены, вращаем только те пропеллеры, которые успешно нашлись в модели
        if (drone.isMotorsActive) {
            float speed = ageInTicks * 0.8F;
            if (this.propellerLf != null) this.propellerLf.yRot = speed;
            if (this.propellerRf != null) this.propellerRf.yRot = -speed;
            if (this.propellerLb != null) this.propellerLb.yRot = -speed;
            if (this.propellerRb != null) this.propellerRb.yRot = speed;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
