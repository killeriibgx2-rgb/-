package com.zzynes.fly_drone;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FlyDroneMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DroneInputHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.getCameraEntity() instanceof FPVDroneEntity) {
                Options options = mc.options;

                float thrust = 0.0F;
                float side = 0.0F;
                float pitchChange = 0.0F;
                float yawChange = 0.0F;

                if (options.keyUp.isDown()) thrust = 1.0F;
                if (options.keyDown.isDown()) thrust = 0.0F;
                if (options.keyLeft.isDown()) side = -1.0F;
                if (options.keyRight.isDown()) side = 1.0F;

                if (options.keyJump.isDown()) pitchChange = -2.0F;
                if (options.keyShift.isDown()) pitchChange = 2.0F;

                yawChange = mc.player.yRotO - mc.player.getYRot();

                ModMessages.sendToServer(new DroneControlPacket(thrust, side, pitchChange, yawChange));
            }
        }
    }

    // ПЕРЕХВАТ КЛИКОВ МЫШИ ДЛЯ СТРЕЛЬБЫ И ДЕТОНАЦИИ
    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton event) {
        Minecraft mc = Minecraft.getInstance();
        // Если игрок пилотирует дрон и нажимает кнопку (действие клика = 1, то есть нажатие)
        if (mc.player != null && mc.getCameraEntity() instanceof FPVDroneEntity && event.getAction() == 1) {
            // Если нажат ЛКМ (0) — отправляем пакет на пуск РПГ
            if (event.getButton() == 0) {
                ModMessages.sendToServer(new DroneAttackPacket(1));
                event.setCanceled(true); // Отменяем стандартный удар рукой в Майнкрафте
            }
            // Если нажат ПКМ (1) — отправляем пакет на подрыв C4
            if (event.getButton() == 1) {
                ModMessages.sendToServer(new DroneAttackPacket(2));
                event.setCanceled(true); // Отменяем стандартный блок/клик блоком
            }
        }
    }
}
