package com.eternalsnow.block;

import com.eternalsnow.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * v2.6.0: a copper tank of water. Put a lit campfire / fireplace / lava under it and it slowly
 * charges; afterwards it keeps releasing that stored heat for minutes. No recipe for a "warm
 * floor" and no snow-melting pot — this is pure thermal mass.
 */
public final class HeatAccumulatorBlock extends BaseEntityBlock {
    public HeatAccumulatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeatAccumulatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModRegistry.HEAT_ACCUMULATOR_BE.get(), HeatAccumulatorBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HeatAccumulatorBlockEntity be) {
            int pct = Math.round(be.getHeatC() / HeatAccumulatorBlockEntity.MAX_HEAT * 100.0F);
            player.displayClientMessage(Component.translatable("eternalsnow.heat_accumulator.readout",
                    String.format(Locale.ROOT, "%d%%", pct)), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
