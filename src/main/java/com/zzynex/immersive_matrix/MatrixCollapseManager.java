package com.zzynex.immersive_matrix;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Mod.EventBusSubscriber(modid = ImmersiveMatrix.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MatrixCollapseManager {

    private static int collapseTicks = -1;
    private static boolean isCollapsing = false;
    private static final Random RANDOM = new Random();
    private static BlockPos fallbackPos = new BlockPos(0, 64, 0);

    private static final Map<BlockPos, BlockState> savedStates = new HashMap<>();
    private static final Map<BlockPos, CompoundTag> savedNbtData = new HashMap<>();

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        BlockState state = event.getState();
        BlockPos pos = event.getPos();

        if (!level.isClientSide && state.getBlock() == ModBlocks.MATRIX_DOOR_BLOCK.get() && level.dimension() == Level.OVERWORLD) {
            ServerLevel serverLevel = (ServerLevel) level;
            ServerLevel matrixLevel = serverLevel.getServer().getLevel(ImmersiveMatrix.MATRIX_DIM_KEY);

            if (matrixLevel != null && !isCollapsing) {
                isCollapsing = true;
                collapseTicks = 420;
                fallbackPos = pos;

                savedStates.clear();
                savedNbtData.clear();

                int minX = -100, maxX = 100;
                int minZ = -100, maxZ = 100;
                int minY = 0, maxY = 128;

                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            BlockPos loopPos = new BlockPos(x, y, z);
                            BlockState blockState = matrixLevel.getBlockState(loopPos);

                            if (!blockState.isAir()) {
                                savedStates.put(loopPos, blockState);
                                BlockEntity be = matrixLevel.getBlockEntity(loopPos);
                                if (be != null) {
                                    savedNbtData.put(loopPos, be.saveWithFullMetadata());
                                }
                            }
                        }
                    }
                }

                BlockPos islandDoorBottom = new BlockPos(0, 65, 0);
                matrixLevel.destroyBlock(islandDoorBottom, false);
                matrixLevel.destroyBlock(islandDoorBottom.above(), false);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && isCollapsing && collapseTicks > 0) {
            collapseTicks--;

            ServerLevel matrixLevel = event.getServer().getLevel(ImmersiveMatrix.MATRIX_DIM_KEY);
            if (matrixLevel != null) {

                if (collapseTicks % 20 == 0) {
                    for (ServerPlayer player : matrixLevel.players()) {
                        matrixLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                ModSounds.DIMENSION_COLLAPSE.get(), SoundSource.AMBIENT, 5.0F, 1.0F);
                    }
                }

                BlockState glitchState = ModBlocks.GLITCH_BLOCK.get().defaultBlockState();

                // МАКСИМАЛЬНАЯ ГЛИЧНОСТЬ: Теперь спавним СТРОГО 100 блоков помех за один тик!
                for (int i = 0; i < 100; i++) {
                    int randX = RANDOM.nextInt(61) - 30;
                    int randY = RANDOM.nextInt(20) - 10;
                    int randZ = RANDOM.nextInt(61) - 30;

                    BlockPos targetPos = new BlockPos(randX, 64 + randY, randZ);
                    BlockState currentBlock = matrixLevel.getBlockState(targetPos);

                    if (!currentBlock.isAir() && currentBlock.getBlock() != ModBlocks.GLITCH_BLOCK.get()) {
                        if (randX != 0 || randZ != 2) {
                            BlockEntity be = matrixLevel.getBlockEntity(targetPos);
                            if (be instanceof Container container) {
                                container.clearContent();
                            }
                            matrixLevel.setBlockAndUpdate(targetPos, glitchState);
                        }
                    }
                }

                if (collapseTicks <= 0) {
                    isCollapsing = false;
                    collapseTicks = -1;

                    ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);
                    if (overworld != null) {
                        List<ServerPlayer> playersToKick = new ArrayList<>(matrixLevel.players());
                        for (ServerPlayer player : playersToKick) {
                            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false));
                            player.teleportTo(overworld, fallbackPos.getX() + 0.5, fallbackPos.getY(), fallbackPos.getZ() + 0.5, player.getYRot(), player.getXRot());
                        }
                    }

                    for (BlockPos loopPos : savedStates.keySet()) {
                        if (matrixLevel.getBlockState(loopPos).getBlock() == ModBlocks.GLITCH_BLOCK.get()) {
                            matrixLevel.setBlockAndUpdate(loopPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                        }
                    }

                    for (Map.Entry<BlockPos, BlockState> entry : savedStates.entrySet()) {
                        BlockPos restorePos = entry.getKey();
                        BlockState restoreState = entry.getValue();
                        matrixLevel.setBlockAndUpdate(restorePos, restoreState);

                        if (savedNbtData.containsKey(restorePos)) {
                            BlockEntity newBe = matrixLevel.getBlockEntity(restorePos);
                            if (newBe != null) {
                                newBe.load(savedNbtData.get(restorePos));
                                newBe.setChanged();
                            }
                        }
                    }

                    savedStates.clear();
                    savedNbtData.clear();
                    ImmersiveMatrix.LOGGER.info("[Immersive Matrix] Супер-разогнанный апокалипсис завершен. База восстановлена!");
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
