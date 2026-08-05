package com.zzynex.immersive_matrix;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.Random;

public class MatrixTeleporter {

    public static void teleportToMatrix(ServerPlayer player) {
        ServerLevel matrixLevel = player.server.getLevel(ImmersiveMatrix.MATRIX_DIM_KEY);

        if (matrixLevel != null) {
            BlockPos centerPos = new BlockPos(0, 64, 0);

            if (matrixLevel.getBlockState(centerPos).isAir()) {
                generateFloatingIsland(matrixLevel, centerPos);
            }

            double targetX = 0.5;
            double targetY = 65.0;
            double targetZ = 2.5;

            player.teleportTo(matrixLevel, targetX, targetY, targetZ, 180.0F, player.getXRot());
        }
    }

    private static void generateFloatingIsland(ServerLevel level, BlockPos center) {
        BlockState blackState = ModBlocks.BLACK.get().defaultBlockState();
        int radius = 5;
        Random random = new Random();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    level.setBlockAndUpdate(center.offset(x, 0, z), blackState);
                }
            }
        }

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

        BlockPos doorBottomPos = center.above();
        BlockPos doorTopPos = doorBottomPos.above();

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
