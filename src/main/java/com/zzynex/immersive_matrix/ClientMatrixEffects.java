package com.zzynex.immersive_matrix;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ImmersiveMatrix.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientMatrixEffects {

    private static final ResourceLocation SCREAMER_TEXTURE =
            new ResourceLocation(ImmersiveMatrix.MOD_ID, "textures/gui/screamer.png");

    // 1. НАРАСТАЮЩАЯ ТРЯСКА КАМЕРЫ (Следит за твоим новым честным измерением)
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            net.minecraft.resources.ResourceKey<Level> currentDim = mc.player.level().dimension();

            if (currentDim.equals(ImmersiveMatrix.MATRIX_DIM_KEY)) {
                if (MatrixCollapseManager.isWorldCollapsing()) {
                    int remainingTicks = MatrixCollapseManager.getRemainingTicks();
                    if (remainingTicks <= 220 && remainingTicks > 0) {
                        float progress = (220f - remainingTicks) / 220f;
                        float shakeIntensity = progress * 4.5f;

                        float shakePitch = (float) (Math.sin(mc.player.tickCount * 1.5) * shakeIntensity * 0.5);
                        float shakeYaw = (float) (Math.cos(mc.player.tickCount * 1.8) * shakeIntensity);

                        event.setPitch(event.getPitch() + shakePitch);
                        event.setYaw(event.getYaw() + shakeYaw);
                    }
                }
            }
        }
    }

    // 2. ВЫВОД СКРИМЕРА НА 18-Й СЕКУНДЕ (Следит за твоим новым честным измерением)
    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().getPath().equals("hotbar")) {
            Minecraft mc = Minecraft.getInstance();

            if (mc.player != null) {
                net.minecraft.resources.ResourceKey<Level> currentDim = mc.player.level().dimension();

                if (currentDim.equals(ImmersiveMatrix.MATRIX_DIM_KEY)) {
                    if (MatrixCollapseManager.isWorldCollapsing()) {
                        int remainingTicks = MatrixCollapseManager.getRemainingTicks();

                        if (remainingTicks <= 60 && remainingTicks > 0) {
                            GuiGraphics guiGraphics = event.getGuiGraphics();
                            int width = event.getWindow().getGuiScaledWidth();
                            int height = event.getWindow().getGuiScaledHeight();

                            RenderSystem.disableDepthTest();
                            RenderSystem.depthMask(false);
                            RenderSystem.enableBlend();
                            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                            guiGraphics.blit(SCREAMER_TEXTURE, 0, 0, 0, 0, width, height, width, height);

                            RenderSystem.depthMask(true);
                            RenderSystem.enableDepthTest();
                            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        }
                    }
                }
            }
        }
    }
}
