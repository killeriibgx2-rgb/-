package com.zzynes.flydrone.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class FpvDroneRenderer extends GeoEntityRenderer<FpvDroneEntity> {

    public FpvDroneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new FpvDroneModel());
        this.shadowRadius = 0.0f; // Без тени, как в документации
    }

    @Override
    public ResourceLocation getTextureLocation(FpvDroneEntity entity) {
        return new ResourceLocation("fly_drone", "textures/entity/fpv_drone.png");
    }
}