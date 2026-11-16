package com.eternalsnow.block;

import com.eternalsnow.registry.ModRegistry;
import com.eternalsnow.thermal.ThermalEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class WallThermometerBlockEntity extends BlockEntity {
    /** Rolling min/max window: 5 minutes of server ticks. */
    private static final long WINDOW_TICKS = 6000L;

    private float localTempC = -15.0F;
    private boolean initialized = false;
    // v2.6.0: trend + min/max over the window, shown ONLY on crosshair hover.
    private float minTempC = -15.0F;
    private float maxTempC = -15.0F;
    private float trendC = 0.0F;
    private float windowStartTempC = -15.0F;
    private long windowStartTick = -1L;
    private float lastSyncedTrendC = 0.0F;

    public WallThermometerBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.WALL_THERMOMETER_BE.get(), pos, state);
    }

    public float getLocalTempC() {
        return this.localTempC;
    }

    public float getMinTempC() {
        return this.minTempC;
    }

    public float getMaxTempC() {
        return this.maxTempC;
    }

    /** Positive = warming, negative = cooling since the start of the window. */
    public float getTrendC() {
        return this.trendC;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WallThermometerBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (be.initialized && (level.getGameTime() + pos.asLong()) % 30L != 0L) {
            return;
        }
        float measured = ThermalEngine.getLocalTemperatureC(serverLevel, pos);
        long now = level.getGameTime();
        if (!be.initialized || be.windowStartTick < 0L || now - be.windowStartTick > WINDOW_TICKS) {
            be.windowStartTick = now;
            be.windowStartTempC = measured;
            be.minTempC = measured;
            be.maxTempC = measured;
            be.lastSyncedTrendC = 0.0F;
        }
        if (measured < be.minTempC) be.minTempC = measured;
        if (measured > be.maxTempC) be.maxTempC = measured;
        be.trendC = measured - be.windowStartTempC;

        if (!be.initialized || Math.abs(measured - be.localTempC) >= 0.20F
                || Math.abs(be.trendC - be.lastSyncedTrendC) >= 0.25F) {
            be.localTempC = measured;
            be.initialized = true;
            be.lastSyncedTrendC = be.trendC;
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeReadout(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("LocalTempC")) {
            this.localTempC = tag.getFloat("LocalTempC");
            this.initialized = true;
        }
        if (tag.contains("MinTempC")) this.minTempC = tag.getFloat("MinTempC");
        if (tag.contains("MaxTempC")) this.maxTempC = tag.getFloat("MaxTempC");
        if (tag.contains("TrendC")) this.trendC = tag.getFloat("TrendC");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        writeReadout(tag);
        return tag;
    }

    private void writeReadout(CompoundTag tag) {
        tag.putFloat("LocalTempC", this.localTempC);
        tag.putFloat("MinTempC", this.minTempC);
        tag.putFloat("MaxTempC", this.maxTempC);
        tag.putFloat("TrendC", this.trendC);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
