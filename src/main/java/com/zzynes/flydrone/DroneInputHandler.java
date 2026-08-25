package com.zzynes.flydrone;

import com.zzynes.flydrone.entity.FpvDroneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FlyDroneMod.MOD_ID, value = Dist.CLIENT)
public class DroneInputHandler {

    private static float currentThrottle = 0f;
    private static final float THROTTLE_STEP = 0.05f;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (!(player.getVehicle() instanceof FpvDroneEntity)) {
            currentThrottle = 0f;
            return;
        }

        // Читаем ванильные клавиши управления
        boolean forward = mc.options.keyUp.isDown();
        boolean back = mc.options.keyDown.isDown();
        boolean left = mc.options.keyLeft.isDown();
        boolean right = mc.options.keyRight.isDown();
        boolean jump = mc.options.keyJump.isDown();
        boolean sneak = mc.options.keyShift.isDown();

        // Газ
        if (jump) {
            currentThrottle = Math.min(1.0f, currentThrottle + THROTTLE_STEP);
        } else if (sneak) {
            currentThrottle = Math.max(0f, currentThrottle - THROTTLE_STEP);
        }

        // Угловые скорости
        float pitchRate = 0f;
        float rollRate = 0f;
        float yawRate = 0f;

        if (forward) pitchRate = -1.5f;
        if (back) pitchRate = 1.5f;
        if (left) rollRate = -1.5f;
        if (right) rollRate = 1.5f;

        // Отправляем пакет на сервер каждый тик
        ModNetwork.CHANNEL.sendToServer(
                new DroneControlPacket(currentThrottle, pitchRate, rollRate, yawRate)
        );
    }
}