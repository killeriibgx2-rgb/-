package com.eternalsnow.block;

import com.eternalsnow.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Zero-BlockEntity invisible structure part for the 3x2x2 3D Fireplace:
 * Forwards all interactions, breaking, pick-block, and look-at telemetry to the master FireplaceBlock in O(1)!
 */
public final class FireplacePartBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty SIDE = IntegerProperty.create("side", 0, 2);   // 0 = -1, 1 = 0, 2 = +1
    public static final IntegerProperty UP_PART = IntegerProperty.create("up_part", 0, 1);
    public static final IntegerProperty BACK_PART = IntegerProperty.create("back_part", 0, 1);

    public FireplacePartBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SIDE, 1)
                .setValue(UP_PART, 0)
                .setValue(BACK_PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SIDE, UP_PART, BACK_PART);
    }

    public static BlockPos getMasterPos(BlockPos partPos, BlockState partState) {
        Direction facing = partState.getValue(FACING);
        int side = partState.getValue(SIDE) - 1;
        int dy = partState.getValue(UP_PART);
        int back = partState.getValue(BACK_PART);

        Direction right = facing.getClockWise();
        Direction backDir = facing.getOpposite();
        return partPos.relative(right, -side).below(dy).relative(backDir, -back);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        BlockPos masterPos = getMasterPos(pos, state);
        BlockState masterState = level.getBlockState(masterPos);
        if (masterState.getBlock() instanceof FireplaceBlock) {
            return FireplaceBlock.interactWithMaster(level, masterPos, masterState, player, hand);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos masterPos = getMasterPos(pos, state);
        BlockState masterState = level.getBlockState(masterPos);
        if (masterState.getBlock() instanceof FireplaceBlock) {
            FireplaceBlock.destroyMultiblock(level, masterPos, masterState.getValue(FireplaceBlock.FACING), !player.isCreative());
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockPos masterPos = getMasterPos(pos, state);
            BlockState masterState = level.getBlockState(masterPos);
            if (masterState.getBlock() instanceof FireplaceBlock) {
                FireplaceBlock.destroyMultiblock(level, masterPos, masterState.getValue(FireplaceBlock.FACING), false);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(ModRegistry.FIREPLACE_ITEM.get());
    }
}
