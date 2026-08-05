package com.zzynex.immersive_matrix;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ImmersiveMatrix.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VoidLoopEvent {

    // ==========================================
    // ТРИГГЕР 1: МГНОВЕННЫЙ ПЕРЕХВАТ В БЕЗДНЕ НА Y: -55
    // ==========================================
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            Level level = player.level();

            if (level.dimension().equals(ImmersiveMatrix.MATRIX_DIM_KEY)) {
                if (player.getY() < -55.0D) {
                    player.fallDistance = 0.0F;
                    player.setDeltaMovement(0, 0, 0);

                    double targetX = 0.5;
                    double targetY = 65.0;
                    double targetZ = 2.5;

                    player.teleportTo((ServerLevel) level, targetX, targetY, targetZ, player.getYRot(), player.getXRot());
                    player.displayClientMessage(Component.literal("Матрица перехватила твое падение у самого дна бездны..."), true);
                }
            }
        }
    }

    // ==========================================
    // ТРИГГЕР 2: ПОЛНОЕ ВЫКЛЮЧЕНИЕ УРОНА ОТ ПАДЕНИЯ В МАТРИЦЕ
    // ==========================================
    @SubscribeEvent
    public static void onPlayerFallDamage(LivingHurtEvent event) {
        // Проверяем, что урон получает именно игрок на сервере
        if (event.getEntity() instanceof ServerPlayer player) {
            Level level = player.level();

            // Если игрок находится внутри нашего измерения Матрицы
            if (level.dimension().equals(ImmersiveMatrix.MATRIX_DIM_KEY)) {
                // И если тип входящего урона — это ПАДЕНИЕ (FALL)
                if (event.getSource().is(DamageTypes.FALL)) {
                    // Намертво отменяем урон, выставляя его значение на 0.0F!
                    event.setAmount(0.0F);
                    // На всякий случай сбрасываем счетчик высоты падения в движке игры
                    player.fallDistance = 0.0F;
                }
            }
        }
    }
}
