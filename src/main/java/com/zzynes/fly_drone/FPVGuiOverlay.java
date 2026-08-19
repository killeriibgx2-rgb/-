package com.zzynes.fly_drone;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(modid = FlyDroneMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FPVGuiOverlay {

    // Пути к твоим PNG файлам в ресурсах мода
    private static final ResourceLocation OVERLAY_TEXTURE =
            new ResourceLocation(FlyDroneMod.MOD_ID, "textures/gui/overlay.png");
    private static final ResourceLocation CONNECTION_LOST_TEXTURE =
            new ResourceLocation(FlyDroneMod.MOD_ID, "textures/gui/connection_lost.png");

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        // Отрисовываем наш интерфейс поверх стандартного игрового оверлея
        if (event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;

            if (player != null && mc.getCameraEntity() instanceof FPVDroneEntity drone) {
                int width = event.getWindow().getGuiScaledWidth();
                int height = event.getWindow().getGuiScaledHeight();

                // Считаем расстояние от тела игрока на земле до летящего дрона
                double distance = player.distanceTo(drone);

                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                if (distance > 150.0D) {
                    // ЕСЛИ ДРОН УЛЕТЕЛ СЛИШКОМ ДАЛЕКО (~150+ блоков): выводим экран помех "Нет сигнала"
                    mc.getTextureManager().bindForSetup(CONNECTION_LOST_TEXTURE);
                    event.getGuiGraphics().blit(CONNECTION_LOST_TEXTURE, 0, 0, 0, 0, width, height, width, height);
                } else {
                    // В НОРМАЛЬНОМ ПОЛЕТЕ: выводим полупрозрачную сетку FPV-очков (HUD)
                    mc.getTextureManager().bindForSetup(OVERLAY_TEXTURE);
                    event.getGuiGraphics().blit(OVERLAY_TEXTURE, 0, 0, 0, 0, width, height, width, height);

                    // Дополнительно пишем техническую информацию текстом прямо на экране
                    String telemetry = String.format("АКБ: %d%% | ДИСТАНЦИЯ: %.1fм", (drone.batteryCharge / 40), distance);
                    event.getGuiGraphics().drawString(mc.font, telemetry, 10, 10, 0xFF00FF00, true);
                }

                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }
}
