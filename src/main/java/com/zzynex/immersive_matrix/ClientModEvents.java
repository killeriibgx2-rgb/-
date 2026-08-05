package com.zzynex.immersive_matrix;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = ImmersiveMatrix.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        DimensionSpecialEffects matrixEffects = new DimensionSpecialEffects(
                15.0F,
                false, // НАМЕРТВО ВЫКЛЮЧАЕМ РЕНДЕР ВАНИЛЬНЫХ ОБЛАКОВ!
                DimensionSpecialEffects.SkyType.NONE, // Стирает солнце, луну и звезды
                false,
                false
        ) {
            @Override
            public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
                return new Vec3(0.0D, 0.0D, 0.0D); // Чистый черный туман горизонта
            }

            @Override
            public boolean isFoggyAt(int x, int z) {
                return false;
            }

            @Nullable
            @Override
            public float[] getSunriseColor(float timeOfDay, float partialTicks) {
                // ИСТРЕБЛЯЕМ БАГ ЗАКАТА: Возвращаем null, чтобы небо никогда не окрашивалось в розовые тона!
                return null;
            }
        };

        event.register(new net.minecraft.resources.ResourceLocation(ImmersiveMatrix.MOD_ID, "matrix_effects"), matrixEffects);
    }
}
