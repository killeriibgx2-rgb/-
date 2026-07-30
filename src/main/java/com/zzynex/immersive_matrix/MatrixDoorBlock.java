package com.zzynex.immersive_matrix;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class MatrixDoorBlock extends DoorBlock implements EntityBlock {

    public MatrixDoorBlock() {
        super(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOD)
                        .strength(3.0F)
                        .sound(SoundType.WOOD)
                        .noOcclusion(),
                BlockSetType.OAK);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return new MatrixDoorBlockEntity(pos, state);
        }
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide && placer instanceof Player player) {
            BlockEntity be = level.getBlockEntity(state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below());
            if (be instanceof MatrixDoorBlockEntity doorEntity) {
                doorEntity.setOwner(player.getUUID(), player.getGameProfile().getName());
                player.displayClientMessage(Component.literal("Дверь успешно привязана к матрице игрока: " + player.getGameProfile().getName()), true);
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockPos basePos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
            BlockEntity be = level.getBlockEntity(basePos);

            if (be instanceof MatrixDoorBlockEntity doorEntity) {
                String ownerName = doorEntity.getOwnerName();
                if (ownerName != null && !ownerName.isEmpty()) {
                    serverPlayer.displayClientMessage(Component.literal("Вы входите в матрицу игрока: " + ownerName), true);

                    // ЗАПУСКАЕМ ТЕЛЕПОРТАЦИЮ
                    MatrixTeleporter.teleportToMatrix(serverPlayer);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
