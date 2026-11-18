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

    private float heatC = 0.0F;

    public HeatAccumulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.HEAT_ACCUMULATOR_BE.get(), pos, state);
    }

    public float getHeatC() {
        return this.heatC;
    }

    /** Compatibility API: stored energy now leaves via neighbour conduction, not a second source. */
    public float getEmitPowerC() {
        return 0.0F;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HeatAccumulatorBlockEntity be) {
        if (level.isClientSide || (level.getGameTime() + pos.asLong()) % 10L != 0L) {
            return;
        }
        // Compatibility telemetry only. Stored energy belongs exclusively to the cell grid;
        // the old independent charge/discharge emitter would otherwise create heat twice.
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            float actual = com.eternalsnow.thermal.ThermalEngine.getLocalTemperatureC(server, pos);
            float next = Math.max(0, actual - com.eternalsnow.thermal.ThermalEngine.computeBaseAmbientC(server, pos));
            if (Math.abs(next - be.heatC) > .01F) {
                be.heatC = next;
                be.setChanged();
            }
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
