package com.zzynex.immersive_matrix;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ImmersiveMatrix.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VoidLoopEvent {

    // Каждый игровой тик подробно проверяем всех игроков на стороне сервера
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Выполняем проверку строго в конце тика и только на сервере, чтобы избежать десинхронизации
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.player;
            ResourceKey<Level> currentDim = player.level().dimension();

            // Железно проверяем, что игрок находится внутри нашего нового измерения Матрицы
            if (currentDim.equals(ImmersiveMatrix.MATRIX_DIM_KEY)) {

                // ПОДРОБНЫЙ ТРИГГЕР БЕЗДНЫ: Ловим игрока на самом дне строительного мира (Y <= -64)
                if (player.getY() <= -64.0) {

                    // Точные координаты возврата на черную поверхность острова перед дверью
                    double returnX = 0.5;
                    double returnY = 65.0; // Высота 65, чтобы не проваливаться в блоки
                    double returnZ = 2.5;

                    // Мгновенная и безопасная телепортация с сохранением направления взгляда игрока
                    player.teleportTo(
                            player.serverLevel(),
                            returnX,
                            returnY,
                            returnZ,
                            player.getYRot(),
                            player.getXRot()
                    );
                }
            }
        }
    }
}
