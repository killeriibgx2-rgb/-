package com.zzynex.immersive_matrix;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ImmersiveMatrix.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        // Создаем рендер неба без солнца, луны и звезд для эффекта цифровой бездны
        DimensionSpecialEffects matrixEffects = new DimensionSpecialEffects(
                15.0F,
                true,
                DimensionSpecialEffects.SkyType.NONE, // Убирает все небесные светила
                false,
                false
        ) {
            @Override
            public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
                // Окрашиваем горизонт в кристально глубокий черный цвет
                return new Vec3(0.0D, 0.0D, 0.0D);
            }

            @Override
            public boolean isFoggyAt(int x, int z) {
                return false;
            }
        };

        event.register(new net.minecraft.resources.ResourceLocation(ImmersiveMatrix.MOD_ID, "matrix_effects"), matrixEffects);
    }
}
