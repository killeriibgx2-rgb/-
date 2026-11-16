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
    private float localTempC = -15.0F;
    private boolean initialized = false;

    public WallThermometerBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.WALL_THERMOMETER_BE.get(), pos, state);
    }

    public float getLocalTempC() {
        return this.localTempC;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WallThermometerBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (be.initialized && (level.getGameTime() + pos.asLong()) % 30L != 0L) {
            return;
        }
        float measured = ThermalEngine.getLocalTemperatureC(serverLevel, pos);
        if (!be.initialized || Math.abs(measured - be.localTempC) >= 0.20F) {
            be.localTempC = measured;
            be.initialized = true;
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("LocalTempC", this.localTempC);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("LocalTempC")) {
            this.localTempC = tag.getFloat("LocalTempC");
            this.initialized = true;
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putFloat("LocalTempC", this.localTempC);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
