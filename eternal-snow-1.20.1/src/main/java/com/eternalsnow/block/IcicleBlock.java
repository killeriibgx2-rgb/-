package com.eternalsnow.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Hanging Winter Icicle block: forms naturally under warm snow-covered roof eaves and can be hung as cozy decor.
 */
public final class IcicleBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(4.0D, 2.0D, 4.0D, 12.0D, 16.0D, 12.0D);

    public IcicleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState sup = level.getBlockState(above);
        return Block.canSupportCenter(level, above, Direction.DOWN)
                || sup.isFaceSturdy(level, above, Direction.DOWN)
                || sup.is(BlockTags.SLABS)
                || sup.is(BlockTags.STAIRS)
                || sup.is(BlockTags.LEAVES);
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction facing,
            BlockState facingState,
            LevelAccessor level,
            BlockPos currentPos,
            BlockPos facingPos
    ) {
        if (facing == Direction.UP && !state.canSurvive(level, currentPos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("eternalsnow.tooltip.icicle.desc").withStyle(ChatFormatting.AQUA));
    }
}
