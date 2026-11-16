package com.eternalsnow.block;

import com.eternalsnow.item.WoodenFirestarterItem;
import com.eternalsnow.registry.ModRegistry;
import com.eternalsnow.thermal.ThermalEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 3D Multi-Block Brick Fireplace (3 wide x 2 deep x 2 tall):
 * - Renders fireplace.geo.json + fireplace_128.png + looping fireplace.animation.json.
 * - Requires Chimney column on 'connect' socket (back-center, dy=+2) leading out to open sky.
 */
public final class FireplaceBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final IntegerProperty POWER_STAGE = IntegerProperty.create("power_stage", 0, 3);

    private static boolean dismantlingMultiblock = false;

    public FireplaceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false)
                .setValue(POWER_STAGE, 0));
    }

    public static int getLightEmissionForState(BlockState state) {
        if (!state.getValue(LIT)) {
            return 0;
        }
        return switch (state.getValue(POWER_STAGE)) {
            case 1 -> 8;  // Low damper smolder
            case 2 -> 12; // Medium damper
            default -> 15; // Wide-open roaring hearth
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, POWER_STAGE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        BlockPos masterPos = ctx.getClickedPos();
        Level level = ctx.getLevel();

        // Check that all 12 blocks of the 3x2x2 fireplace footprint are replaceable & inside build height!
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();
        for (int side = -1; side <= 1; side++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int b = 0; b <= 1; b++) {
                    BlockPos p = masterPos.relative(right, side).above(dy).relative(back, b);
                    if (p.getY() >= level.getMaxBuildHeight() - 1) {
                        return null;
                    }
                    if (!level.getBlockState(p).canBeReplaced(ctx)) {
                        return null;
                    }
                }
            }
        }
        return this.defaultBlockState().setValue(FACING, facing).setValue(LIT, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos masterPos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, masterPos, state, placer, stack);
        Direction facing = state.getValue(FACING);
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();

        Block partBlock = ModRegistry.FIREPLACE_PART.get();
        for (int side = -1; side <= 1; side++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int b = 0; b <= 1; b++) {
                    if (side == 0 && dy == 0 && b == 0) {
                        continue;
                    }
                    BlockPos p = masterPos.relative(right, side).above(dy).relative(back, b);
                    BlockState partState = partBlock.defaultBlockState()
                            .setValue(FireplacePartBlock.FACING, facing)
                            .setValue(FireplacePartBlock.SIDE, side + 1)
                            .setValue(FireplacePartBlock.UP_PART, dy)
                            .setValue(FireplacePartBlock.BACK_PART, b);
                    level.setBlock(p, partState, Block.UPDATE_ALL);
                }
            }
        }
    }

    public static void destroyMultiblock(Level level, BlockPos masterPos, Direction facing, boolean dropFireplaceItem) {
        if (dismantlingMultiblock) {
            return;
        }
        dismantlingMultiblock = true;
        try {
            Direction right = facing.getClockWise();
            Direction back = facing.getOpposite();
            for (int side = -1; side <= 1; side++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int b = 0; b <= 1; b++) {
                        BlockPos p = masterPos.relative(right, side).above(dy).relative(back, b);
                        BlockState cur = level.getBlockState(p);
                        if (cur.getBlock() instanceof FireplaceBlock || cur.getBlock() instanceof FireplacePartBlock) {
                            if (p.equals(masterPos) && dropFireplaceItem) {
                                Block.dropResources(cur, level, masterPos);
                            }
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                }
            }
            if (level instanceof ServerLevel sl) {
                ThermalEngine.onHeatSourceChanged(sl, masterPos, false);
            }
        } finally {
            dismantlingMultiblock = false;
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
            destroyMultiblock(level, pos, state.getValue(FACING), false);
        }
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
        return interactWithMaster(level, pos, state, player, hand);
    }

    public static InteractionResult interactWithMaster(
            Level level,
            BlockPos masterPos,
            BlockState masterState,
            Player player,
            InteractionHand hand
    ) {
        if (!(level.getBlockEntity(masterPos) instanceof FireplaceBlockEntity be)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        boolean lit = masterState.getValue(LIT);
        Direction facing = masterState.getValue(FACING);

        // 1. Shift + Right-Click with empty hand -> Extinguish fireplace!
        if (player.isShiftKeyDown() && held.isEmpty()) {
            if (lit) {
                if (!level.isClientSide) {
                    be.setLitState(false);
                    level.playSound(null, masterPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.85F, 1.05F);
                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SMOKE,
                                masterPos.getX() + 0.5D, masterPos.getY() + 0.5D, masterPos.getZ() + 0.5D,
                                12, 0.25D, 0.2D, 0.25D, 0.02D);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }

        // 2. Right-Click with Instant Lighter (Flint & Steel / Fire Charge) -> Ignite!
        // Note: WoodenFirestarterItem returns PASS here so its 10-second hold-to-rub mechanic starts cleanly!
        if (held.getItem() instanceof WoodenFirestarterItem) {
            return InteractionResult.PASS;
        }

        if (WoodenFirestarterItem.isInstantIgniter(held)) {
            if (lit) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                if (!be.hasFuel()) {
                    player.displayClientMessage(Component.translatable("eternalsnow.msg.fireplace_no_fuel"), true);
                    return InteractionResult.CONSUME;
                }
                BlockPos flueExit = FireplaceBlockEntity.findValidChimneyFlueExit(level, masterPos, facing);
                if (flueExit == null) {
                    player.displayClientMessage(Component.translatable("eternalsnow.msg.fireplace_no_chimney"), true);
                    level.playSound(null, masterPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.3F);
                    return InteractionResult.CONSUME;
                }
                if (be.getDamperPercent() <= 0) {
                    player.displayClientMessage(Component.translatable("eternalsnow.msg.fireplace.damper_closed"), true);
                    level.playSound(null, masterPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.2F);
                    return InteractionResult.CONSUME;
                }
                if (WoodenFirestarterItem.tryConsumeInstantIgniter(level, masterPos, player, hand, held)) {
                    be.setLitState(true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 3. Right-Click / Shift+Right-Click with any burnable fuel or junk -> Feed fixed 0..100% firebox!
        int milliPerItem = FireplaceFuelHelper.getFuelMilliUnits(held);
        if (milliPerItem > 0) {
            if (be.getFuelMilli() >= FireplaceFuelHelper.MAX_FUEL_MILLI) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("eternalsnow.msg.fireplace_full"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                boolean isLavaBucket = held.is(Items.LAVA_BUCKET);
                int maxToConsume = (player.isShiftKeyDown() && !isLavaBucket) ? held.getCount() : 1;
                int consumed = 0;
                for (int i = 0; i < maxToConsume; i++) {
                    if (be.getFuelMilli() >= FireplaceFuelHelper.MAX_FUEL_MILLI) {
                        break;
                    }
                    be.addFuelMilli(milliPerItem);
                    consumed++;
                }
                if (consumed > 0) {
                    if (!player.getAbilities().instabuild) {
                        if (isLavaBucket) {
                            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                        } else {
                            held.shrink(consumed);
                        }
                    }
                    level.playSound(null, masterPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.85F, 0.9F + level.random.nextFloat() * 0.2F);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FireplaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
                type,
                ModRegistry.FIREPLACE_BE.get(),
                level.isClientSide ? FireplaceBlockEntity::clientTick : FireplaceBlockEntity::serverTick
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("eternalsnow.tooltip.fireplace.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("eternalsnow.tooltip.fireplace.chimney").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("eternalsnow.tooltip.fireplace.fuel").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("eternalsnow.tooltip.fireplace.craft").withStyle(ChatFormatting.DARK_GRAY));
    }
}
