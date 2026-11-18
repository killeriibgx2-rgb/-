package com.eternalsnow.thermal;

import com.eternalsnow.block.FireplaceBlock;
import com.eternalsnow.block.FireplaceBlockEntity;
import com.eternalsnow.block.FireplaceFuelHelper;
import com.eternalsnow.block.HeatAccumulatorBlock;
import com.eternalsnow.block.HeatAccumulatorBlockEntity;
import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.state.EternalSnowState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;


/** Public compatibility facade. All temperatures now come from the persisted cell grid. */
public final class ThermalEngine {
    private ThermalEngine() {}
    public static void clearAll() { com.eternalsnow.thermal.cells.WorldCellThermal.clearAll(); }
    public static void forgetChunk(ServerLevel level, ChunkPos pos) {
        com.eternalsnow.thermal.cells.WorldCellThermal.unload(level, pos.toLong());
    }
    public static void onBlockPlaced(ServerLevel level, BlockPos pos) {
        com.eternalsnow.thermal.cells.WorldCellThermal.replaced(level, pos);
    }
    public static void onBlockBroken(ServerLevel level, BlockPos pos) {
        com.eternalsnow.thermal.cells.WorldCellThermal.replaced(level, pos);
    }
    public static void onHeatSourceChanged(ServerLevel level, BlockPos pos, boolean active) {
        com.eternalsnow.thermal.cells.WorldCellThermal.sourceChanged(level, pos);
    }
    public static float getLocalTemperatureC(ServerLevel level, BlockPos pos) {
        if (!EternalSnowState.isOverworldLike(level) || !EternalSnowState.getServerSettings().enabled)
            return computeBaseAmbientC(level, pos);
        return (float) com.eternalsnow.thermal.cells.WorldCellThermal.temperature(level, pos);
    }
    public static float getDeltaHeatAt(ServerLevel level, BlockPos pos) {
        return getLocalTemperatureC(level, pos) - computeBaseAmbientC(level, pos);
    }
    public static void tickLevelThermal(ServerLevel level) {
        com.eternalsnow.thermal.cells.WorldCellThermal.tick(level);
    }
    public static boolean isPotentialHeatEmitter(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        Block b = state.getBlock();
        return b instanceof FireplaceBlock
                || b instanceof HeatAccumulatorBlock
                || b instanceof CampfireBlock
                || b instanceof AbstractFurnaceBlock
                || b instanceof LanternBlock
                || b instanceof TorchBlock
                || b instanceof WallTorchBlock
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE);
    }

    public static float computeBaseAmbientC(ServerLevel level, BlockPos pos) {
        if (!EternalSnowState.isOverworldLike(level)) {
            return level.dimension() == Level.NETHER ? 42.0F : 10.0F;
        }
        float surfaceTemp = EternalSnowState.worldAmbientTempC;
        int y = pos.getY();
        if (y > 85) {
            surfaceTemp -= (y - 85) * 0.12F;
        }
        if (y < 52 && !level.canSeeSky(pos)) {
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
            int depth = surfaceY - y;
            if (depth > 8) {
                float geothermalBlend = Mth.clamp((depth - 8) / 36.0F, 0.0F, 0.85F);
                surfaceTemp = Mth.lerp(geothermalBlend, surfaceTemp, 6.0F);
            }
        }
        return surfaceTemp;
    }

    public static boolean isAirOrOpenPassage(BlockState state) {
        if (state.isAir()) {
            return true;
        }
        if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.OPEN)) {
            return true;
        }
        if (state.getBlock() instanceof TrapDoorBlock && state.getValue(TrapDoorBlock.OPEN)) {
            return true;
        }
        return state.getCollisionShape(null, BlockPos.ZERO).isEmpty()
                && !state.is(Blocks.SNOW)
                && !state.is(Blocks.LAVA)
                && !state.is(Blocks.WATER);
    }

    public static float getHeatSourcePowerC(ServerLevel level, BlockPos pos, BlockState state, ServerSnowSettings s) {
        float basePower = 0.0F;
        Block block = state.getBlock();
        if (block instanceof FireplaceBlock) {
            if (state.hasProperty(FireplaceBlock.LIT) && state.getValue(FireplaceBlock.LIT)) {
                int damper = 100;
                if (level.getBlockEntity(pos) instanceof FireplaceBlockEntity fbe) {
                    damper = fbe.getDamperPercent();
                }
                basePower = s.heatFireplace * FireplaceFuelHelper.getHeatPowerMultiplier(damper);
            }
        } else if (state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            basePower = s.heatLava;
        } else if (state.is(Blocks.MAGMA_BLOCK)) {
            basePower = s.heatMagma;
        } else if (block instanceof HeatAccumulatorBlock) {
            // v2.6.0: hot water releases its stored heat slowly through the normal emitter path.
            if (level.getBlockEntity(pos) instanceof HeatAccumulatorBlockEntity acc) {
                basePower = 0; // stored cell energy, never an independent heat source
            }
        } else if (block instanceof CampfireBlock) {
            if (state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT)) {
                if (state.is(Blocks.SOUL_CAMPFIRE)) {
                    basePower = s.heatSoulCampfire;
                } else {
                    boolean hayBoost = level.getBlockState(pos.below()).is(Blocks.HAY_BLOCK);
                    basePower = hayBoost ? s.heatCampfire * 1.25F : s.heatCampfire;
                }
            }
        } else if (block instanceof BlastFurnaceBlock || block instanceof SmokerBlock) {
            if (state.hasProperty(AbstractFurnaceBlock.LIT) && state.getValue(AbstractFurnaceBlock.LIT)) {
                basePower = s.heatBlastSmoker;
            }
        } else if (block instanceof AbstractFurnaceBlock) {
            if (state.hasProperty(AbstractFurnaceBlock.LIT) && state.getValue(AbstractFurnaceBlock.LIT)) {
                basePower = s.heatFurnace;
            }
        } else if (block instanceof LanternBlock) {
            basePower = s.heatLantern;
        } else if (block instanceof TorchBlock || block instanceof WallTorchBlock) {
            basePower = s.heatTorch;
        }

        if (basePower <= 0.05F) {
            return 0.0F;
        }

        float withBonus = Math.max(0.0F, basePower + s.globalHeatBonusC);
        return withBonus * (0.75F + 0.25F * s.globalHeatingSpeedMultiplier);
    }

    public static void meltOneSnowLayerAt(ServerLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (st.is(Blocks.SNOW)) {
            int layers = st.getValue(SnowLayerBlock.LAYERS);
            if (layers <= 1) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            } else {
                level.setBlock(pos, st.setValue(SnowLayerBlock.LAYERS, layers - 1), Block.UPDATE_CLIENTS);
            }
            if (level.random.nextInt(5) == 0) {
                level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D, 1, 0.1D, 0.05D, 0.1D, 0.01D);
            }
        } else if (st.is(Blocks.SNOW_BLOCK)) {
            level.setBlock(pos, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 7), Block.UPDATE_ALL);
        }
    }
    /** Keep roof melt and eave icicles on the new temperature path. Never load neighbours. */
    public static void processWarmSurface(ServerLevel level, BlockPos pos) {
        meltOneSnowLayerAt(level, pos);
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null)
            meltOneSnowLayerAt(level, pos.above());
        if (computeBaseAmbientC(level, pos) >= -2 || level.random.nextInt(32) != 0
                || !com.eternalsnow.registry.ModRegistry.ICICLE.isPresent()) return;
        Direction[] sides = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        BlockPos edge = pos.relative(sides[level.random.nextInt(4)]);
        if (level.getChunkSource().getChunkNow(edge.getX() >> 4, edge.getZ() >> 4) == null) return;
        BlockPos under = edge.below();
        if (!level.getBlockState(under).isAir() || !level.canSeeSky(edge.above())) return;
        BlockState icicle = com.eternalsnow.registry.ModRegistry.ICICLE.get().defaultBlockState();
        if (icicle.canSurvive(level, under)) level.setBlock(under, icicle, Block.UPDATE_CLIENTS);
    }
}
