package com.eternalsnow.block;

import com.eternalsnow.registry.ModRegistry;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.thermal.ThermalEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class FireplaceBlockEntity extends BlockEntity {
    private int fuelMilli = 0;
    private int damperPercent = 100;
    private boolean chimneyValid = false;

    public FireplaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.FIREPLACE_BE.get(), pos, state);
    }

    public int getFuelMilli() {
        return this.fuelMilli;
    }

    public int getDamperPercent() {
        return this.damperPercent;
    }

    public boolean hasFuel() {
        return this.fuelMilli > 0;
    }

    public boolean isLit() {
        BlockState st = this.getBlockState();
        return st.hasProperty(FireplaceBlock.LIT) && st.getValue(FireplaceBlock.LIT);
    }

    public boolean isChimneyValid() {
        return this.chimneyValid;
    }

    public int getCalculatedRemainingTicks() {
        return FireplaceFuelHelper.computeRemainingRealTicks(this.fuelMilli, Math.max(1, this.damperPercent));
    }

    public float getFillRatio() {
        return FireplaceFuelHelper.getFillRatio(this.fuelMilli);
    }

    public int addFuelMilli(int addMilli) {
        if (addMilli <= 0 || this.fuelMilli >= FireplaceFuelHelper.MAX_FUEL_MILLI) {
            return 0;
        }
        int before = this.fuelMilli;
        this.fuelMilli = Math.min(FireplaceFuelHelper.MAX_FUEL_MILLI, this.fuelMilli + addMilli);
        this.setChanged();
        syncToClients();
        return this.fuelMilli - before;
    }

    public static int computePowerStage(boolean lit, int damperPercent) {
        if (!lit || damperPercent <= 0) {
            return 0;
        }
        if (damperPercent <= 25) {
            return 1; // Low smolder (dim warm embers)
        }
        if (damperPercent <= 75) {
            return 2; // Steady hearth flame
        }
        return 3; // Roaring maximum draft!
    }

    public void setDamperPercent(int newPercent, @Nullable ServerPlayer actor) {
        int clamped = Mth.clamp(newPercent, 0, 100);
        if (this.damperPercent == clamped) {
            return;
        }
        this.damperPercent = clamped;

        if (this.level instanceof ServerLevel sl) {
            if (clamped == 0 && isLit()) {
                // Closing the chimney damper to 0% cuts off all draft -> fireplace immediately suffocates!
                setLitState(false);
                sl.playSound(null, this.worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.85F, 0.95F);
                sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                        this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.55D, this.worldPosition.getZ() + 0.5D,
                        14, 0.25D, 0.2D, 0.25D, 0.02D);
                if (actor != null) {
                    actor.displayClientMessage(Component.translatable("eternalsnow.msg.fireplace.damper_extinguished"), true);
                }
                return;
            }

            if (isLit()) {
                updateBlockPowerStage();
                ThermalEngine.onHeatSourceChanged(sl, this.worldPosition, true);
            }
        }
        this.setChanged();
        syncToClients();
    }

    private void updateBlockPowerStage() {
        if (this.level == null) {
            return;
        }
        BlockState st = this.getBlockState();
        if (!(st.getBlock() instanceof FireplaceBlock)) {
            return;
        }
        boolean lit = st.getValue(FireplaceBlock.LIT);
        int desiredStage = computePowerStage(lit, this.damperPercent);
        if (st.getValue(FireplaceBlock.POWER_STAGE) != desiredStage) {
            this.level.setBlock(this.worldPosition, st.setValue(FireplaceBlock.POWER_STAGE, desiredStage), Block.UPDATE_ALL);
        }
    }

    public void setLitState(boolean lit) {
        if (this.level == null) {
            return;
        }
        if (lit && this.damperPercent <= 0) {
            return; // Cannot burn with closed damper!
        }
        BlockState st = this.getBlockState();
        if (st.getBlock() instanceof FireplaceBlock) {
            int stage = computePowerStage(lit, this.damperPercent);
            if (st.getValue(FireplaceBlock.LIT) != lit || st.getValue(FireplaceBlock.POWER_STAGE) != stage) {
                this.level.setBlock(
                        this.worldPosition,
                        st.setValue(FireplaceBlock.LIT, lit).setValue(FireplaceBlock.POWER_STAGE, stage),
                        Block.UPDATE_ALL
                );
                if (this.level instanceof ServerLevel sl) {
                    ThermalEngine.onHeatSourceChanged(sl, this.worldPosition, lit);
                }
            }
        }
        this.setChanged();
        syncToClients();
    }

    /**
     * Given ANY ChimneyBlock position in a chimney column, walks straight down to find the connected
     * 3D Fireplace master position (attached on the 'connect' socket: back=1, dy=+2).
     */
    @Nullable
    public static BlockPos findConnectedFireplaceMaster(Level level, BlockPos anyChimneyPos) {
        if (!(level.getBlockState(anyChimneyPos).getBlock() instanceof ChimneyBlock)) {
            return null;
        }
        int minY = Math.max(level.getMinBuildHeight() + 2, anyChimneyPos.getY() - 52);
        BlockPos bottomChimney = anyChimneyPos;
        for (int y = anyChimneyPos.getY() - 1; y >= minY; y--) {
            BlockPos below = new BlockPos(anyChimneyPos.getX(), y, anyChimneyPos.getZ());
            if (level.getBlockState(below).getBlock() instanceof ChimneyBlock) {
                bottomChimney = below;
            } else {
                break;
            }
        }
        // Directly below the bottom ChimneyBlock on 'connect' (dy=2, back=1) sits FireplacePartBlock (dy=1, back=1)
        BlockPos partPos = bottomChimney.below();
        BlockState partState = level.getBlockState(partPos);
        BlockPos masterPos = null;
        if (partState.getBlock() instanceof FireplacePartBlock) {
            masterPos = FireplacePartBlock.getMasterPos(partPos, partState);
        } else if (partState.getBlock() instanceof FireplaceBlock) {
            masterPos = partPos;
        }
        if (masterPos == null) {
            return null;
        }
        BlockState ms = level.getBlockState(masterPos);
        if (!(ms.getBlock() instanceof FireplaceBlock)) {
            return null;
        }
        Direction facing = ms.getValue(FireplaceBlock.FACING);
        BlockPos expectedStart = masterPos.relative(facing.getOpposite(), 1).above(2);
        return bottomChimney.equals(expectedStart) ? masterPos : null;
    }

    /**
     * Checks the Chimney column starting directly on the 3D 'connect' socket:
     * back 1 block from masterPos, 2 blocks up (masterPos.relative(facing.getOpposite(), 1).above(2)).
     * Must consist of >= 1 ChimneyBlock leading up to open sky!
     */
    @Nullable
    public static BlockPos findValidChimneyFlueExit(Level level, BlockPos masterPos, Direction facing) {
        BlockPos start = masterPos.relative(facing.getOpposite(), 1).above(2);
        if (!(level.getBlockState(start).getBlock() instanceof ChimneyBlock)) {
            return null;
        }
        int maxY = Math.min(level.getMaxBuildHeight() - 2, start.getY() + 48);
        BlockPos topChimney = start;
        for (int y = start.getY() + 1; y <= maxY; y++) {
            BlockPos next = new BlockPos(start.getX(), y, start.getZ());
            if (level.getBlockState(next).getBlock() instanceof ChimneyBlock) {
                topChimney = next;
            } else {
                break;
            }
        }
        BlockPos exitPos = topChimney.above();
        BlockState exitState = level.getBlockState(exitPos);
        boolean clearExit = exitState.isAir() || exitState.is(Blocks.SNOW);
        if (!clearExit) {
            return null;
        }
        if (!level.canSeeSky(exitPos)) {
            return null;
        }
        return exitPos;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, FireplaceBlockEntity be) {
        boolean lit = state.getValue(FireplaceBlock.LIT);
        int burnRate = FireplaceFuelHelper.getBurnMilliPerTick(be.damperPercent);
        if (lit && be.fuelMilli > 0 && burnRate > 0) {
            be.fuelMilli = Math.max(0, be.fuelMilli - burnRate);
        }
        Direction facing = state.getValue(FireplaceBlock.FACING);

        if (level.getGameTime() % 20L == 0L) {
            be.chimneyValid = (findValidChimneyFlueExit(level, pos, facing) != null);
        }

        if (!lit || be.damperPercent <= 0) {
            return;
        }

        float draftRatio = be.damperPercent / 100.0F;

        // Crackle frequency & volume scale naturally with chimney draft!
        int crackleInterval = Math.max(14, Math.round(52 - draftRatio * 34));
        if (level.random.nextInt(crackleInterval) == 0) {
            level.playLocalSound(
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
                    0.30F + draftRatio * 0.65F,
                    0.75F + draftRatio * 0.35F,
                    false
            );
        }

        // Rooftop smoke density scales with damper opening (sparse wisp when choked, thick plume at 100%!)
        int smokeEveryTicks = (be.damperPercent <= 20) ? 14 : ((be.damperPercent <= 60) ? 7 : 3);
        if (level.getGameTime() % smokeEveryTicks == 0L) {
            BlockPos exitPos = findValidChimneyFlueExit(level, pos, facing);
            if (exitPos != null) {
                float wx = EternalSnowState.getWindDirX() * EternalSnowState.windSpeed;
                float wz = EternalSnowState.getWindDirZ() * EternalSnowState.windSpeed;
                double sx = exitPos.getX() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.22D;
                double sy = exitPos.getY() + 0.08D;
                double sz = exitPos.getZ() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.22D;

                double riseSpeed = 0.045D + draftRatio * 0.055D;
                level.addAlwaysVisibleParticle(
                        draftRatio >= 0.75F ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        true,
                        sx, sy, sz,
                        wx * 0.14D, riseSpeed, wz * 0.14D
                );
                if (draftRatio >= 0.55F && level.random.nextInt(3) == 0) {
                    level.addParticle(
                            ParticleTypes.LARGE_SMOKE,
                            sx, sy, sz,
                            wx * 0.18D, 0.055D, wz * 0.18D
                    );
                }
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FireplaceBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        Direction facing = state.getValue(FireplaceBlock.FACING);
        boolean lit = state.getValue(FireplaceBlock.LIT);

        if (lit && be.damperPercent <= 0) {
            be.setLitState(false);
            sl.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.95F);
            return;
        }

        if (level.getGameTime() % 20L == 0L) {
            BlockPos exitPos = findValidChimneyFlueExit(level, pos, facing);
            boolean validNow = (exitPos != null);
            if (be.chimneyValid != validNow) {
                be.chimneyValid = validNow;
                be.syncToClients();
            }
            if (lit) {
                if (!validNow) {
                    be.setLitState(false);
                    sl.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.0F);
                    return;
                }
                if (sl.getBlockState(exitPos).is(Blocks.SNOW)) {
                    sl.setBlock(exitPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }

        if (!lit) {
            return;
        }

        int burnRate = FireplaceFuelHelper.getBurnMilliPerTick(be.damperPercent);
        if (be.fuelMilli > 0 && burnRate > 0) {
            be.fuelMilli = Math.max(0, be.fuelMilli - burnRate);
            if (be.fuelMilli == 0) {
                be.setLitState(false);
                sl.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 1.1F);
                return;
            }
            if (level.getGameTime() % 80L == 0L) {
                be.setChanged();
                be.syncToClients();
            }
        } else {
            be.setLitState(false);
            return;
        }

        // Every 30 ticks: dry wet players & grant 1 minute of «Домашний уют» (scaled by draft!)
        if (level.getGameTime() % 30L == 0L) {
            double radius = 3.5D + (be.damperPercent / 100.0D) * 4.5D;
            AABB cozyBox = new AABB(pos).inflate(radius, 4.0D, radius);
            List<ServerPlayer> nearby = sl.getEntitiesOfClass(ServerPlayer.class, cozyBox);
            int dryAmount = Math.round(70 + (be.damperPercent / 100.0F) * 140);
            for (ServerPlayer sp : nearby) {
                if (!sl.canSeeSky(sp.blockPosition().above())) {
                    if (ModRegistry.WET_EFFECT.isPresent() && sp.hasEffect(ModRegistry.WET_EFFECT.get())) {
                        MobEffectInstance cur = sp.getEffect(ModRegistry.WET_EFFECT.get());
                        sp.removeEffect(ModRegistry.WET_EFFECT.get());
                        if (cur != null && cur.getDuration() > dryAmount) {
                            sp.addEffect(new MobEffectInstance(ModRegistry.WET_EFFECT.get(), cur.getDuration() - dryAmount, 0, false, true, true));
                        }
                        sl.sendParticles(ParticleTypes.CLOUD, sp.getX(), sp.getY() + 1.0D, sp.getZ(), 4, 0.25D, 0.3D, 0.25D, 0.01D);
                    }
                    if (be.damperPercent >= 15 && ModRegistry.COZY_HEARTH.isPresent()) {
                        sp.addEffect(new MobEffectInstance(ModRegistry.COZY_HEARTH.get(), 1200, 0, false, false, true));
                    }
                }
            }
        }
    }

    public void syncToClients() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(2.5D, 2.8D, 2.5D);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("FuelMilli", Mth.clamp(this.fuelMilli, 0, FireplaceFuelHelper.MAX_FUEL_MILLI));
        tag.putInt("DamperPercent", Mth.clamp(this.damperPercent, 0, 100));
        tag.putBoolean("ChimneyValid", this.chimneyValid);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("FuelMilli")) {
            this.fuelMilli = Mth.clamp(tag.getInt("FuelMilli"), 0, FireplaceFuelHelper.MAX_FUEL_MILLI);
        } else if (tag.contains("FuelTicks")) {
            this.fuelMilli = Mth.clamp(tag.getInt("FuelTicks") * FireplaceFuelHelper.MILLI_PER_TICK, 0, FireplaceFuelHelper.MAX_FUEL_MILLI);
        }
        this.damperPercent = tag.contains("DamperPercent") ? Mth.clamp(tag.getInt("DamperPercent"), 0, 100) : 100;
        this.chimneyValid = tag.getBoolean("ChimneyValid");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
