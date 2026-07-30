package com.zzynex.immersive_matrix;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.Random;

public class MatrixTeleporter {

    public static void teleportToMatrix(ServerPlayer player) {
        // Запрашиваем сервер выдать уровень нашего честного, отдельного измерения Матрицы
        ServerLevel matrixLevel = player.server.getLevel(ImmersiveMatrix.MATRIX_DIM_KEY);

        if (matrixLevel != null) {
            BlockPos centerPos = new BlockPos(0, 64, 0);

            // Очищаем куб воздуха в радиусе 15 блоков вокруг центра острова,
            // чтобы стереть любые случайные блоки шума генератора
            for (int x = -15; x <= 15; x++) {
                for (int y = -15; y <= 15; y++) {
                    for (int z = -15; z <= 15; z++) {
                        BlockPos clearPos = centerPos.offset(x, y, z);
                        if (matrixLevel.getBlockState(clearPos).getBlock() != ModBlocks.BLACK_BLOCK.get()) {
                            matrixLevel.setBlockAndUpdate(clearPos, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
            }

            // Генерируем парящий неоновый остров на Y=64, если его ещё нет
            if (matrixLevel.isEmptyBlock(centerPos)) {
                generateFloatingIsland(matrixLevel, centerPos);
            }

            // Безопасно спавним игрока перед дверью на высоте 65
            double targetX = 0.5;
            double targetY = 65.0;
            double targetZ = 2.5;

            player.teleportTo(matrixLevel, targetX, targetY, targetZ, 180.0F, player.getXRot());
        }
    }

    private static void generateFloatingIsland(ServerLevel level, BlockPos center) {
        BlockState blackState = ModBlocks.BLACK_BLOCK.get().defaultBlockState();
        int radius = 5;
        Random random = new Random();

        // 1. Плоская черная поверхность на Y=64
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    level.setBlockAndUpdate(center.offset(x, 0, z), blackState);
                }
            }
        }

        // 2. Конус утеса вниз до высоты Y=58
        for (int yOffset = -1; yOffset >= -6; yOffset--) {
            int currentY = center.getY() + yOffset;
            int currentRadius = radius + yOffset;
            if (currentRadius < 1) currentRadius = 1;

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= currentRadius * currentRadius) {
                        if (x * x + z * z < (currentRadius - 1) * (currentRadius - 1) || random.nextFloat() > 0.4f) {
                            level.setBlockAndUpdate(new BlockPos(x, currentY, z), blackState);
                        }
                    }
                }
            }
        }

        // 3. Ставим ответную дверь строго НА черную поверхность острова (высота Y=65 и Y=66)
        BlockPos doorBottomPos = center.above(); // Y = 65
        BlockPos doorTopPos = doorBottomPos.above(); // Y = 66

        BlockState doorBottomState = ModBlocks.MATRIX_DOOR_BLOCK.get().defaultBlockState()
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(DoorBlock.FACING, Direction.SOUTH);

        BlockState doorTopState = ModBlocks.MATRIX_DOOR_BLOCK.get().defaultBlockState()
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)
                .setValue(DoorBlock.FACING, Direction.SOUTH);

        level.setBlockAndUpdate(doorBottomPos, doorBottomState);
        level.setBlockAndUpdate(doorTopPos, doorTopState);

        if (level.getBlockEntity(doorBottomPos) instanceof MatrixDoorBlockEntity doorEntity) {
            doorEntity.setOwner(java.util.UUID.randomUUID(), "MATRIX");
        }
    }
}
