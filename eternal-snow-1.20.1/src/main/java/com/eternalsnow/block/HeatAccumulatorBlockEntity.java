package com.eternalsnow.block;

import com.eternalsnow.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class HeatAccumulatorBlockEntity extends BlockEntity {
    public static final float MAX_HEAT = 90.0F;
    private static final float CHARGE_PER_TICK = 1.8F;
    /** 0.9983 per 10 server ticks -> ~5 minute discharge tail. */
    private static final float DISCHARGE = 0.9983F;

    private float heatC = 0.0F;

    public HeatAccumulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.HEAT_ACCUMULATOR_BE.get(), pos, state);
    }

    public float getHeatC() {
        return this.heatC;
    }

    /** Emitted as ordinary heat-source power by ThermalEngine.getHeatSourcePowerC(). */
    public float getEmitPowerC() {
        return this.heatC * 0.62F;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HeatAccumulatorBlockEntity be) {
        if (level.isClientSide || (level.getGameTime() + pos.asLong()) % 10L != 0L) {
            return;
        }
        BlockState below = level.getBlockState(pos.below());
        boolean heating = (below.getBlock() instanceof CampfireBlock
                    && below.hasProperty(CampfireBlock.LIT) && below.getValue(CampfireBlock.LIT))
                || below.is(Blocks.FIRE) || below.is(Blocks.SOUL_FIRE)
                || below.is(Blocks.LAVA) || below.is(Blocks.MAGMA_BLOCK)
                || (below.getBlock() instanceof com.eternalsnow.block.FireplaceBlock
                    && below.hasProperty(com.eternalsnow.block.FireplaceBlock.LIT)
                    && below.getValue(com.eternalsnow.block.FireplaceBlock.LIT));

        float next = heating
                ? Math.min(MAX_HEAT, be.heatC + CHARGE_PER_TICK)
                : be.heatC * DISCHARGE;
        if (next < 0.05F) {
            next = 0.0F;
        }
        if (Math.abs(next - be.heatC) > 0.005F) {
            be.heatC = next;
            be.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("HeatC", this.heatC);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.heatC = tag.contains("HeatC") ? tag.getFloat("HeatC") : 0.0F;
    }
}
