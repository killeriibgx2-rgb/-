package com.zzynes.fly_drone;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

public class FPVDroneRenderer extends LivingEntityRenderer<FPVDroneEntity, FPVDroneModel> {

    // Путь к PNG текстуре твоего дрона в ресурсах
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(FlyDroneMod.MOD_ID, "textures/entity/fpv_drone.png");

    public FPVDroneRenderer(EntityRendererProvider.Context context) {
        // ИСПРАВЛЕНО: Создаем модель без лишних параметров дженериков, типы теперь бьются идеально!
        super(context, new FPVDroneModel(context.bakeLayer(fpv_drone.LAYER_LOCATION)), 0.25F);
    }

    @Override
    public ResourceLocation getTextureLocation(FPVDroneEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(FPVDroneEntity entity) {
        // Отключаем отображение никнеймов и индикаторов здоровья над летающими дронами
        return false;
    }
}
