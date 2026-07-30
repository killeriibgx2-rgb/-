package com.zzynex.immersive_matrix;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = ImmersiveMatrix.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MatrixCollapseManager {

    private static int collapseTicks = -1;
    private static boolean isCollapsing = false;
    private static final Random RANDOM = new Random();

    // Переменная для запоминания точки спавна игрока в Обычном мире при эвакуации
    private static BlockPos fallbackPos = new BlockPos(0, 64, 0);

    // ПОДРОБНЫЙ ТРИГГЕР: Срабатывает, когда кто-то ломает Матричную Дверь в Обычном мире
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        BlockState state = event.getState();
        BlockPos pos = event.getPos();

        // Проверяем, что сломали именно нашу дверь и именно в Overworld
        if (!level.isClientSide && state.getBlock() == ModBlocks.MATRIX_DOOR_BLOCK.get() && level.dimension() == Level.OVERWORLD) {
            ServerLevel serverLevel = (ServerLevel) level;

            // Находим серверную копию нашего нового кастомного измерения
            ServerLevel matrixLevel = serverLevel.getServer().getLevel(ImmersiveMatrix.MATRIX_DIM_KEY);

            if (matrixLevel != null && !isCollapsing) {
                isCollapsing = true;
                collapseTicks = 420; // 21 секунда до полного уничтожения мира (420 тиков)

                // Запоминаем, где стояла дверь в Обычном мире, чтобы вернуть игрока ровно туда
                fallbackPos = pos;

                // Разрушаем ответную дверь строго на новой высоте острова (нижний блок на 65, верхний на 66)
                BlockPos islandDoorBottom = new BlockPos(0, 65, 0);
                matrixLevel.destroyBlock(islandDoorBottom, false);
                matrixLevel.destroyBlock(islandDoorBottom.above(), false);

                // Включаем эмбиент катастрофы destroy.ogg на максимум для всех, кто заперт в Матрице
                for (ServerPlayer player : matrixLevel.players()) {
                    matrixLevel.playSound(
                            null,
                            player.getX(), player.getY(), player.getZ(),
                            ModSounds.DIMENSION_COLLAPSE.get(),
                            SoundSource.AMBIENT,
                            1.0F,
                            1.0F
                    );
                }

                ImmersiveMatrix.LOGGER.warn("[Immersive Matrix] МАТРИЦА ПОВРЕЖДЕНА! Апокалипсис на высоте 64 запущен.");
            }
        }
    }

    // Каждый игровой тик обрабатываем таймер обрушения реальности
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && isCollapsing && collapseTicks > 0) {
            collapseTicks--;

            ServerLevel matrixLevel = event.getServer().getLevel(ImmersiveMatrix.MATRIX_DIM_KEY);
            if (matrixLevel != null) {

                BlockState glitchState = ModBlocks.GLITCH_BLOCK.get().defaultBlockState();
                int radius = 8;

                // ПОДРОБНЫЙ АЛГОРИТМ ВСЕЯДНОГО ГЛИТЧ-ВИРУСА:
                // Каждый тик выбираем 5 случайных точек по ВСЕМ осям вокруг острова (от Y=58 до Y=76)
                for (int i = 0; i < 5; i++) {
                    int randX = RANDOM.nextInt(radius * 2 + 1) - radius;
                    int randY = RANDOM.nextInt(12) - 6;
                    int randZ = RANDOM.nextInt(radius * 2 + 1) - radius;

                    BlockPos targetPos = new BlockPos(randX, 64 + randY, randZ);
                    BlockState currentBlock = matrixLevel.getBlockState(targetPos);

                    // УСЛОВИЕ ОБХОДА: Глитч пожирает только твердые блоки постройки и острова, воздух оставляем чистым!
                    if (!currentBlock.isAir() && currentBlock.getBlock() != ModBlocks.GLITCH_BLOCK.get()) {
                        // Защищаем пятачок спавна (X:0, Z:2), чтобы игрока не замуровало в текстурах
                        if (randX != 0 || randZ != 2) {
                            matrixLevel.setBlockAndUpdate(targetPos, glitchState);
                        }
                    }
                }

                // ФИНАЛ ТАЙМЕРА: 21 секунда истекла, выкидываем всех обратно с тошнотой
                if (collapseTicks <= 0) {
                    isCollapsing = false;
                    collapseTicks = -1;

                    ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);
                    if (overworld != null) {
                        // Создаем временную копию списка, чтобы избежать краша из-за одновременного изменения циклов
                        List<ServerPlayer> playersToKick = new ArrayList<>(matrixLevel.players());

                        for (ServerPlayer player : playersToKick) {
                            // Накладываем эффект тошноты на 5 секунд (100 тиков)
                            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false));

                            // Выбрасываем игрока в Overworld на место сломанной двери
                            player.teleportTo(
                                    overworld,
                                    fallbackPos.getX() + 0.5,
                                    fallbackPos.getY(),
                                    fallbackPos.getZ() + 0.5,
                                    player.getYRot(),
                                    player.getXRot()
                            );
                        }
                    }
                    ImmersiveMatrix.LOGGER.info("[Immersive Matrix] Измерение полностью схлопнулось. Все игроки эвакуированы.");
                }
            }
        }
    }

    public static boolean isWorldCollapsing() {
        return isCollapsing;
    }

    public static int getRemainingTicks() {
        return collapseTicks;
    }
}
