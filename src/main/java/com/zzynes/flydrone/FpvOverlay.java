package com.zzynes.flydrone;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zzynes.flydrone.entity.FpvDroneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FlyDroneMod.MOD_ID, value = Dist.CLIENT)
public class FpvOverlay {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (!(player.getVehicle() instanceof FpvDroneEntity drone)) return;

        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Батарея
        int batteryTicks = drone.getBatteryTicks();
        int maxTicks = 4800;
        int batteryPercent = (batteryTicks * 100) / maxTicks;
        String batteryText = "BAT: " + batteryPercent + "%";

        // Температура мотора
        float motorTemp = drone.getMotorTemp();
        String motorText = "MOT: " + String.format("%.0f", motorTemp) + "C";

        // Температура батареи
        float battTemp = drone.getBatteryTemp();
        String battTempText = "BATT: " + String.format("%.0f", battTemp) + "C";

        // Газ
        float throttle = drone.getThrottle();
        int throttlePercent = (int)(throttle * 100);
        String throttleText = "THR: " + throttlePercent + "%";

        // Рисуем текст
        RenderSystem.enableBlend();
        gui.drawString(mc.font, batteryText, 10, screenHeight - 60, getColor(batteryPercent), true);
        gui.drawString(mc.font, motorText, 10, screenHeight - 48, getTempColor(motorTemp, 120f), true);
        gui.drawString(mc.font, battTempText, 10, screenHeight - 36, getTempColor(battTemp, 80f), true);
        gui.drawString(mc.font, throttleText, 10, screenHeight - 24, 0xFFFFFFFF, true);
        RenderSystem.disableBlend();
    }

    private static int getColor(int percent) {
        if (percent > 50) return 0xFF00FF00; // Зелёный
        if (percent > 20) return 0xFFFFFF00; // Жёлтый
        return 0xFFFF0000; // Красный
    }

    private static int getTempColor(float temp, float max) {
        float ratio = temp / max;
        if (ratio < 0.5f) return 0xFF00FF00;
        if (ratio < 0.8f) return 0xFFFFFF00;
        return 0xFFFF0000;
    }
}