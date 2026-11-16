package com.eternalsnow.block;

import com.eternalsnow.item.CopperThermosItem;
import com.eternalsnow.item.EmptyCupItem;
import com.eternalsnow.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CocoaPotBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape POT_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 15.0D, 15.0D);

    public CocoaPotBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return POT_SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Rendered via custom 3D CocoaPotRenderer using exact pot - Converted.geo.json + pot.png!
        return RenderShape.ENTITYBLOCK_ANIMATED;
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
        ItemStack held = player.getItemInHand(hand);
        if (!(level.getBlockEntity(pos) instanceof CocoaPotBlockEntity potBe)) {
            return InteractionResult.PASS;
        }

        // Clicking with a Copper Thermos fills up to 6 hot servings directly from the pot!
        if (held.getItem() instanceof CopperThermosItem && potBe.getServings() > 0) {
            int curSips = CopperThermosItem.getSips(held);
            int curDrink = CopperThermosItem.getDrinkType(held);
            int potDrink = potBe.getDrinkType();
            if (curSips < CopperThermosItem.MAX_SIPS && (curSips == 0 || curDrink == potDrink)) {
                if (!level.isClientSide) {
                    int taken = 0;
                    while (curSips + taken < CopperThermosItem.MAX_SIPS && potBe.getServings() > 0) {
                        int d = potBe.takeOneServing();
                        if (d == CocoaPotBlockEntity.DRINK_NONE) break;
                        potDrink = d;
                        taken++;
                    }
                    if (taken > 0) {
                        CopperThermosItem.setContents(held, potDrink, curSips + taken);
                        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.95F, 0.95F);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        // Clicking with an Empty Cup when the pot holds cooked cocoa or caramel drink fills 1 mug!
        if (held.getItem() instanceof EmptyCupItem && potBe.getServings() > 0) {
            if (!level.isClientSide) {
                int drink = potBe.takeOneServing();
                if (drink != CocoaPotBlockEntity.DRINK_NONE) {
                    Item filledItem = (drink == CocoaPotBlockEntity.DRINK_CARAMEL)
                            ? ModRegistry.CARAMEL_CUP.get()
                            : ModRegistry.COCOA_CUP.get();
                    ItemStack filledStack = new ItemStack(filledItem);

                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                    }
                    if (held.isEmpty()) {
                        player.setItemInHand(hand, filledStack);
                    } else if (!player.getInventory().add(filledStack)) {
                        player.drop(filledStack, false);
                    }
                    level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.9F, 1.05F);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Otherwise open the Cocoa Pot cooking menu!
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            NetworkHooks.openScreen(sp, potBe, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof CocoaPotBlockEntity potBe) {
                Containers.dropContents(level, pos, potBe);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CocoaPotBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModRegistry.COCOA_POT_BE.get(), CocoaPotBlockEntity::serverTick);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("eternalsnow.tooltip.cocoa_pot.place").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("eternalsnow.tooltip.cocoa_pot.recipe_cocoa").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("eternalsnow.tooltip.cocoa_pot.recipe_caramel").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("eternalsnow.tooltip.cocoa_pot.scoop").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("eternalsnow.tooltip.cocoa_pot.craft").withStyle(ChatFormatting.DARK_GRAY));
    }
}
