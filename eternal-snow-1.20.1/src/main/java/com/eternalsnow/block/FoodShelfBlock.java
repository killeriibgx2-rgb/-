package com.eternalsnow.block;

import com.eternalsnow.food.FoodShelfManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * v2.7 — a thin wall rack with FIVE hooks, one block long.
 *
 * <p>No GUI on purpose. Which hook you touch is decided by where your crosshair actually lands
 * (see {@link FoodShelfManager#hookFromHit}), then rounded to the nearest hook — a hook is two
 * pixels wide, so "the exact hook you hit" would be unhittable. Left-clicking a hook takes the item
 * back, right-clicking with food (or anything edible-looking) hangs it.
 *
 * <p>The shelf is non-colliding: it must never block movement or building, only be targetable.
 */
public final class FoodShelfBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** Five hooks along the block, flush with the wall. */
    public static final int HOOKS = 5;

    private static final VoxelShape SHAPE_NORTH = Block.box(0.0D, 10.0D, 14.5D, 16.0D, 15.0D, 16.0D);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0.0D, 10.0D, 0.0D, 16.0D, 15.0D, 1.5D);
    private static final VoxelShape SHAPE_WEST = Block.box(14.5D, 10.0D, 0.0D, 16.0D, 15.0D, 16.0D);
    private static final VoxelShape SHAPE_EAST = Block.box(0.0D, 10.0D, 0.0D, 1.5D, 15.0D, 16.0D);

    public FoodShelfBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }

    /** Non-colliding by design: it hangs on the wall and must never push the player around. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Needs a solid wall behind it, exactly like a torch, but the player may be looking anywhere.
        BlockPos pos = context.getClickedPos();
        for (Direction dir : context.getNearestLookingDirections()) {
            if (dir.getAxis().isHorizontal()) {
                BlockState candidate = this.defaultBlockState().setValue(FACING, dir.getOpposite());
                if (candidate.canSurvive(context.getLevel(), pos)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos wall = pos.relative(facing.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
                                  LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction.getOpposite() == state.getValue(FACING) && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FoodShelfBlockEntity(pos, state);
    }

    /**
     * Right click hangs what is in your hand (and, with an empty hand, takes it back).
     *
     * <p>A dedicated left-click hook is handled in {@link com.eternalsnow.world.ModWorldEvents#onLeftClickBlock}
     * because vanilla still routes both buttons through {@code use()} — without that, left-clicking
     * would also try to hang an item before the break starts.
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FoodShelfBlockEntity shelf)) {
            return InteractionResult.PASS;
        }
        int hook = FoodShelfManager.hookFromHit(state, pos, hit);
        ItemStack held = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack onHook = shelf.getHook(hook);
        if (!onHook.isEmpty()) {
            // Occupied hook: an EMPTY hand takes the item back, anything else is ignored so the
            // player's own item is never silently swapped onto the hook.
            if (held.isEmpty()) {
                player.setItemInHand(hand, onHook);
                shelf.setHook(hook, ItemStack.EMPTY);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.7F, 1.0F);
            }
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty() || !FoodShelfManager.isHangable(held)) {
            return InteractionResult.CONSUME;
        }

        ItemStack one = held.copy();
        one.setCount(1);
        shelf.setHook(hook, one);
        held.shrink(1);
        player.setItemInHand(hand, held);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.7F, 1.0F);
        return InteractionResult.CONSUME;
    }

    /** Breaking the block drops everything that was hanging on it — food must never vanish. */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FoodShelfBlockEntity shelf) {
            shelf.dropContents();
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
