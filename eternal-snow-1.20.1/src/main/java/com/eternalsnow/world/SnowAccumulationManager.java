package com.eternalsnow.world;

import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.thermal.ThermalEngine;
import com.eternalsnow.weather.WeatherEventType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ServerLevelData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SnowAccumulationManager {
    private static final Direction[] HORIZONTAL_DIRS = new Direction[]{
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private static int remainingBudgetThisTick = 140;
    private static boolean lastKnownEnabled = true;
    private static final Map<UUID, Long> LAST_PLAYER_COLUMN = new HashMap<>();

    private SnowAccumulationManager() {
    }

    public static void resetTickBudget() {
        ServerSnowSettings settings = EternalSnowState.getServerSettings();
        WeatherEventType ev = EternalSnowState.getActiveWeatherEvent();
        float precip = settings.computeCurrentPrecipitationSpeed(ev, EternalSnowState.snowIntensity);
        // Е: Smart automatic per-tick block update budget scaled dynamically from live precipitation!
        remainingBudgetThisTick = Mth.clamp(Math.round(36.0F + precip * 10.5F), 28, 600);
    }

    /**
     * Б-3: Ice-Fishing Hole Protection — checks in O(playerCount) if an active FishingHook bobber floats here!
     */
    public static boolean isProtectedByFishingBobber(ServerLevel level, BlockPos waterPos) {
        double wx = waterPos.getX() + 0.5D;
        double wy = waterPos.getY() + 0.5D;
        double wz = waterPos.getZ() + 0.5D;
        for (ServerPlayer sp : level.players()) {
            if (sp.fishing != null && sp.fishing.isAlive()) {
                if (sp.fishing.distanceToSqr(wx, wy, wz) <= 5.2D) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void enforceWeatherOnLevel(ServerLevel level) {
        if (!EternalSnowState.isOverworldLike(level)) {
            return;
        }
        ServerSnowSettings settings = EternalSnowState.getServerSettings();
        if (settings.enabled) {
            WeatherEventType ev = EternalSnowState.getActiveWeatherEvent();
            float precipSpeed = settings.computeCurrentPrecipitationSpeed(ev, EternalSnowState.snowIntensity);
            boolean shouldPrecipitate = (ev != WeatherEventType.CLEAR_FROST) && (precipSpeed > 0.01F);

            if (level.getLevelData() instanceof ServerLevelData serverData) {
                if (serverData.getClearWeatherTime() > 0) {
                    serverData.setClearWeatherTime(0);
                }
                if (serverData.isThundering()) {
                    serverData.setThundering(false);
                    serverData.setThunderTime(0);
                }
                if (shouldPrecipitate) {
                    if (!serverData.isRaining() || serverData.getRainTime() < 20000) {
                        serverData.setRaining(true);
                        serverData.setRainTime(Integer.MAX_VALUE);
                    }
                } else if (serverData.isRaining()) {
                    serverData.setRaining(false);
                    serverData.setRainTime(0);
                }
            }
            if (shouldPrecipitate) {
                if (level.getRainLevel(1.0F) < 1.0F) {
                    level.setRainLevel(1.0F);
                    level.setThunderLevel(0.0F);
                }
            } else if (level.getRainLevel(1.0F) > 0.0F) {
                level.setRainLevel(0.0F);
                level.setThunderLevel(0.0F);
            }
            lastKnownEnabled = true;
        } else if (lastKnownEnabled) {
            lastKnownEnabled = false;
            level.setWeatherParameters(24000, 0, false, false);
            level.setRainLevel(0.0F);
            level.setThunderLevel(0.0F);
        }
    }

    public static void tickLevelSnow(ServerLevel level) {
        enforceWeatherOnLevel(level);
        if (!EternalSnowState.isOverworldLike(level)) {
            return;
        }

        ServerSnowSettings settings = EternalSnowState.getServerSettings();
        if (!settings.enabled) {
            return;
        }

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }

        // 1. Footstep snow trampling (1 layer compression under walking players)
        if (settings.trampleChancePercent > 0) {
            tickPlayerFootstepTrample(level, players, settings.trampleChancePercent);
        }

        if (remainingBudgetThisTick <= 0) {
            return;
        }

        RandomSource random = level.random;
        int playerCount = players.size();
        int radiusBlocks = 104;

        float intensity = EternalSnowState.snowIntensity;
        WeatherEventType ev = EternalSnowState.getActiveWeatherEvent();
        float currentPrecipSpeed = settings.computeCurrentPrecipitationSpeed(ev, intensity);

        // On Clear Frost days (or when precipitation speed is 0), NO snow falls! Only water freezes in the cold.
        if (currentPrecipSpeed <= 0.01F) {
            if (settings.freezeWater) {
                int freezeChecks = Math.min(4 * playerCount, remainingBudgetThisTick);
                for (int i = 0; i < freezeChecks && remainingBudgetThisTick > 0; i++) {
                    ServerPlayer player = players.get(i % playerCount);
                    if (player.isRemoved()) continue;
                    int x = player.getBlockX() + random.nextInt(radiusBlocks * 2 + 1) - radiusBlocks;
                    int z = player.getBlockZ() + random.nextInt(radiusBlocks * 2 + 1) - radiusBlocks;
                    LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                    if (chunk != null && tryFreezeWaterOnly(level, chunk, x, z, settings, random)) {
                        remainingBudgetThisTick--;
                    }
                }
            }
            return;
        }

        // Self-throttling automatic budget if server MSPT is heavily loaded (> 38 ms)
        if (level.getServer().getAverageTickTime() > 38.0F) {
            remainingBudgetThisTick = Math.max(8, remainingBudgetThisTick / 3);
        }

        float exactAttempts = currentPrecipSpeed * 4.0F * playerCount;
        int totalAttempts = (int) exactAttempts;
        if (random.nextFloat() < (exactAttempts - totalAttempts)) {
            totalAttempts++;
        }
        totalAttempts = Math.min(totalAttempts, Math.max(remainingBudgetThisTick * 4, 64));

        for (int i = 0; i < totalAttempts && remainingBudgetThisTick > 0; i++) {
            ServerPlayer player = players.get(i % playerCount);
            if (player.isRemoved()) {
                continue;
            }

            int x = player.getBlockX() + random.nextInt(radiusBlocks * 2 + 1) - radiusBlocks;
            int z = player.getBlockZ() + random.nextInt(radiusBlocks * 2 + 1) - radiusBlocks;

            LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            if (chunk == null) {
                continue;
            }

            if (processColumn(level, chunk, x, z, settings, random)) {
                remainingBudgetThisTick--;
            }
        }
    }

    private static boolean tryFreezeWaterOnly(
            ServerLevel level,
            LevelChunk chunk,
            int x,
            int z,
            ServerSnowSettings settings,
            RandomSource random
    ) {
        int motionY = getEffectiveSurfaceY(level, x, z, settings);
        if (motionY <= level.getMinBuildHeight() || motionY >= level.getMaxBuildHeight() - 1) {
            return false;
        }
        BlockPos belowMotion = new BlockPos(x, motionY - 1, z);
        BlockState belowState = chunk.getBlockState(belowMotion);
        if (belowState.is(Blocks.WATER) && belowState.getFluidState().isSource()) {
            if (!isProtectedByFishingBobber(level, belowMotion) && ThermalEngine.getLocalTemperatureC(level, belowMotion) < 0.0F) {
                Biome biome = level.getBiome(belowMotion).value();
                boolean allowOpenWaterFreeze = random.nextInt(4) == 0;
                if (biome.shouldFreeze(level, belowMotion, !allowOpenWaterFreeze)) {
                    level.setBlock(belowMotion, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
                    return true;
                }
            }
        }
        return false;
    }

    private static void tickPlayerFootstepTrample(ServerLevel level, List<ServerPlayer> players, int chancePercent) {
        for (ServerPlayer player : players) {
            if (player.isSpectator() || !player.onGround()) {
                continue;
            }
            int bx = player.getBlockX();
            int bz = player.getBlockZ();
            long packed = (((long) bx) << 32) ^ (bz & 0xFFFFFFFFL);
            Long prev = LAST_PLAYER_COLUMN.put(player.getUUID(), packed);
            if (prev == null || prev == packed) {
                continue;
            }
            BlockPos footPos = player.blockPosition();
            BlockState atFeet = level.getBlockState(footPos);
            if (atFeet.is(Blocks.SNOW) && atFeet.getValue(SnowLayerBlock.LAYERS) >= 3 && level.random.nextInt(2) == 0) {
                level.playSound(null, footPos, SoundEvents.POWDER_SNOW_STEP, SoundSource.PLAYERS, 0.45F, 0.85F + level.random.nextFloat() * 0.25F);
            }
            if (level.random.nextInt(100) >= chancePercent) {
                continue;
            }
            if (atFeet.is(Blocks.SNOW)) {
                int layers = atFeet.getValue(SnowLayerBlock.LAYERS);
                if (layers > 1) {
                    level.setBlock(footPos, atFeet.setValue(SnowLayerBlock.LAYERS, layers - 1), Block.UPDATE_CLIENTS);
                }
            } else {
                BlockPos below = footPos.below();
                BlockState belowState = level.getBlockState(below);
                if (belowState.is(Blocks.SNOW_BLOCK)) {
                    level.setBlock(below, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 7), Block.UPDATE_ALL);
                } else if (belowState.is(Blocks.SNOW)) {
                    int layers = belowState.getValue(SnowLayerBlock.LAYERS);
                    if (layers > 1) {
                        level.setBlock(below, belowState.setValue(SnowLayerBlock.LAYERS, layers - 1), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private static int getEffectiveSurfaceY(ServerLevel level, int x, int z, ServerSnowSettings settings) {
        Heightmap.Types hmType = (settings.leavesMode == 2)
                ? Heightmap.Types.MOTION_BLOCKING_NO_LEAVES
                : Heightmap.Types.MOTION_BLOCKING;
        return level.getHeight(hmType, x, z);
    }

    private static boolean canSnowReachFromSky(ServerLevel level, BlockPos pos, ServerSnowSettings settings) {
        if (level.canSeeSky(pos)) {
            return true;
        }
        if (settings.leavesMode != 2) {
            return false;
        }
        // In Pass-Through-Leaves mode (2), check if only air/leaves/snow lie between pos and open sky
        BlockPos.MutableBlockPos check = pos.mutable();
        int maxY = Math.min(level.getMaxBuildHeight() - 1, pos.getY() + 36);
        while (check.getY() < maxY) {
            check.move(Direction.UP);
            if (level.canSeeSky(check)) {
                return true;
            }
            BlockState st = level.getBlockState(check);
            if (!st.isAir() && !st.is(BlockTags.LEAVES) && !st.is(Blocks.SNOW)) {
                return false;
            }
        }
        return false;
    }

    private static boolean processColumn(
            ServerLevel level,
            LevelChunk chunk,
            int x,
            int z,
            ServerSnowSettings settings,
            RandomSource random
    ) {
        int motionY = getEffectiveSurfaceY(level, x, z, settings);
        if (motionY <= level.getMinBuildHeight() || motionY >= level.getMaxBuildHeight() - 1) {
            return false;
        }

        // 1. Water Freezing (controlled independently by settings.freezeWater!)
        if (settings.freezeWater) {
            BlockPos belowMotion = new BlockPos(x, motionY - 1, z);
            BlockState belowState = chunk.getBlockState(belowMotion);
            if (belowState.is(Blocks.WATER) && belowState.getFluidState().isSource()) {
                if (!isProtectedByFishingBobber(level, belowMotion) && ThermalEngine.getLocalTemperatureC(level, belowMotion) < 0.0F) {
                    Biome biome = level.getBiome(belowMotion).value();
                    boolean allowOpenWaterFreeze = random.nextInt(4) == 0;
                    if (biome.shouldFreeze(level, belowMotion, !allowOpenWaterFreeze)) {
                        level.setBlock(belowMotion, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
                        return true;
                    }
                }
                return false;
            }
        }

        // 2. Resolve snow column target
        ColumnTarget target = resolveColumnTarget(level, chunk, x, z, motionY, settings);
        if (target == null) {
            return false;
        }

        // 3. Aerodynamic Windward Drift Piling & Leeward Wind Shadow!
        float windSpeed = EternalSnowState.windSpeed;
        float driftScale = settings.autoWindDrift
                ? (0.65F + windSpeed * 0.85F)
                : (settings.windDriftStrengthPercent / 100.0F);
        double windHeightBonusBlocks = 0.0D;
        boolean extraWindwardLayer = false;

        if (windSpeed >= 0.22F && driftScale > 0.01F) {
            float wx = EternalSnowState.getWindDirX();
            float wz = EternalSnowState.getWindDirZ();

            // Check Leeward Shadow (upwind solid building wall shields this spot!)
            boolean inWindShadow = false;
            for (int d = 1; d <= 4; d++) {
                int ux = x - Math.round(wx * d);
                int uz = z - Math.round(wz * d);
                if (level.getChunkSource().getChunkNow(ux >> 4, uz >> 4) == null) {
                    continue;
                }
                int wallY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ux, uz);
                if (wallY - target.y >= Math.max(2, d)) {
                    inWindShadow = true;
                    break;
                }
            }

            if (inWindShadow) {
                float skipProb = Mth.clamp(0.82F * windSpeed * driftScale, 0.0F, 0.92F);
                if (random.nextFloat() < skipProb) {
                    return false; // Wind blows snow over the roof, leaving sheltered side clean!
                }
                windHeightBonusBlocks = -0.65D * driftScale;
            } else {
                // Check Windward Obstacle (downwind solid wall catches drifting snow right against the wall!)
                for (int d = 1; d <= 3; d++) {
                    int dx = x + Math.round(wx * d);
                    int dz = z + Math.round(wz * d);
                    if (level.getChunkSource().getChunkNow(dx >> 4, dz >> 4) == null) {
                        continue;
                    }
                    int wallY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
                    int rise = wallY - target.y;
                    if (rise >= 2) {
                        double proximity = (4 - d) / 3.0D;
                        windHeightBonusBlocks = Math.min(rise - 0.25D, 2.8D * proximity * windSpeed * driftScale);
                        extraWindwardLayer = (windSpeed > 0.55F && random.nextFloat() < 0.45F * driftScale);
                        break;
                    }
                }
            }
        }

        // 4. Check finite height cap & optional auto-thaw
        if (!settings.infiniteHeight) {
            double effectiveAvg = Math.max(0.1D, settings.averagePillarHeight + windHeightBonusBlocks);
            int localTargetLayers = computeLocalTargetLayers(x, z, effectiveAvg);
            int maxScanBlocks = Mth.ceil(effectiveAvg) + 8;
            int currentTotalLayers = measureSnowLayersInColumn(chunk, target.x, target.y, target.z, target.currentLayers, level.getMinBuildHeight(), maxScanBlocks);

            if (currentTotalLayers >= localTargetLayers) {
                if (settings.autoThawExcess && currentTotalLayers > localTargetLayers) {
                    return thawOneLayer(level, chunk, target);
                }
                return false;
            }
        }

        // 5. Smooth drift distribution
        if (settings.smoothDrifts && windHeightBonusBlocks <= 0.1D) {
            double centerSurface = target.y + (target.currentLayers / 8.0D);
            int startIdx = random.nextInt(4);
            ColumnTarget lowestNeighbor = null;
            double lowestSurface = centerSurface;

            for (int d = 0; d < 4; d++) {
                Direction dir = HORIZONTAL_DIRS[(startIdx + d) & 3];
                int nx = x + dir.getStepX();
                int nz = z + dir.getStepZ();
                LevelChunk nChunk = ((nx >> 4) == (x >> 4) && (nz >> 4) == (z >> 4))
                        ? chunk
                        : level.getChunkSource().getChunkNow(nx >> 4, nz >> 4);
                if (nChunk == null) {
                    continue;
                }
                int nMotionY = getEffectiveSurfaceY(level, nx, nz, settings);
                if (nMotionY <= level.getMinBuildHeight() || nMotionY >= level.getMaxBuildHeight() - 1) {
                    continue;
                }
                ColumnTarget nTarget = resolveColumnTarget(level, nChunk, nx, nz, nMotionY, settings);
                if (nTarget == null) {
                    continue;
                }
                double nSurface = nTarget.y + (nTarget.currentLayers / 8.0D);
                double diff = centerSurface - nSurface;
                if (diff >= 0.25D && diff <= 1.75D && nSurface < lowestSurface) {
                    lowestSurface = nSurface;
                    lowestNeighbor = nTarget;
                }
            }

            if (lowestNeighbor != null) {
                target = lowestNeighbor;
                chunk = ((target.x >> 4) == (x >> 4) && (target.z >> 4) == (z >> 4))
                        ? chunk
                        : level.getChunkSource().getChunkNow(target.x >> 4, target.z >> 4);
                if (chunk == null) {
                    return false;
                }
            }
        }

        // 6. Place snow (and optional 2nd windward drift layer during strong gale/blizzard)
        boolean placed = addOneSnowLayer(level, chunk, target, settings);
        if (placed && extraWindwardLayer && remainingBudgetThisTick > 1) {
            int nextMotionY = getEffectiveSurfaceY(level, target.x, target.z, settings);
            ColumnTarget second = resolveColumnTarget(level, chunk, target.x, target.z, nextMotionY, settings);
            if (second != null) {
                addOneSnowLayer(level, chunk, second, settings);
            }
        }
        return placed;
    }

    private static ColumnTarget resolveColumnTarget(
            ServerLevel level,
            LevelChunk chunk,
            int x,
            int z,
            int motionY,
            ServerSnowSettings settings
    ) {
        BlockPos belowPos = new BlockPos(x, motionY - 1, z);
        BlockState belowState = chunk.getBlockState(belowPos);

        if (belowState.is(Blocks.SNOW)) {
            int layers = belowState.getValue(SnowLayerBlock.LAYERS);
            if (layers < 8) {
                return new ColumnTarget(x, motionY - 1, z, layers, false, false);
            }
        }

        if (belowState.is(Blocks.MAGMA_BLOCK)) {
            return null;
        }

        // Farmland / Dirt Path handling
        if (belowState.is(Blocks.DIRT_PATH) || belowState.is(Blocks.FARMLAND)) {
            if (!settings.snowOnPathsAndFarmland) {
                return null;
            }
            BlockPos topPos = new BlockPos(x, motionY, z);
            if (chunk.getBlockState(topPos).isAir() || isBuriableWildPlant(chunk.getBlockState(topPos))) {
                return new ColumnTarget(x, motionY, z, 0, !chunk.getBlockState(topPos).isAir(), true);
            }
            return null;
        }

        BlockPos topPos = new BlockPos(x, motionY, z);
        BlockState topState = chunk.getBlockState(topPos);

        if (topState.is(Blocks.SNOW)) {
            int layers = topState.getValue(SnowLayerBlock.LAYERS);
            if (layers < 8) {
                return new ColumnTarget(x, motionY, z, layers, false, false);
            }
            if (motionY + 1 >= level.getMaxBuildHeight() - 1) {
                return null;
            }
            BlockPos aboveEight = topPos.above();
            if (chunk.getBlockState(aboveEight).isAir()) {
                return new ColumnTarget(x, motionY + 1, z, 0, false, false);
            }
            return null;
        }

        if (topState.isAir() || (settings.leavesMode == 2 && topState.is(BlockTags.LEAVES))) {
            if (topState.isAir() && Blocks.SNOW.defaultBlockState().canSurvive(level, topPos)) {
                return new ColumnTarget(x, motionY, z, 0, false, false);
            }
            return null;
        }

        if (settings.buryPlants && isBuriableWildPlant(topState)) {
            if (Blocks.SNOW.defaultBlockState().canSurvive(level, topPos)) {
                return new ColumnTarget(x, motionY, z, 0, true, false);
            }
        }

        return null;
    }

    private static boolean isBuriableWildPlant(BlockState state) {
        if (state.getBlock() instanceof FlowerPotBlock) {
            return false;
        }
        if (state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS)) {
            return false;
        }
        return state.is(Blocks.GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(BlockTags.FLOWERS);
    }

    private static boolean addOneSnowLayer(
            ServerLevel level,
            LevelChunk chunk,
            ColumnTarget target,
            ServerSnowSettings settings
    ) {
        BlockPos pos = new BlockPos(target.x, target.y, target.z);
        if (target.y >= level.getMaxBuildHeight() - 1) {
            return false;
        }

        // Respect 3D ThermalEngine warmth (torches, furnaces, campfires, warm houses)!
        if (ThermalEngine.getLocalTemperatureC(level, pos) > 0.5F) {
            return false;
        }

        if (!canSnowReachFromSky(level, pos, settings)) {
            return false;
        }

        if (target.convertPathBelow && target.y > level.getMinBuildHeight()) {
            level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        }

        if (target.currentLayers == 0 && target.y > level.getMinBuildHeight()) {
            BlockPos belowPos = pos.below();
            BlockState belowState = chunk.getBlockState(belowPos);
            if (belowState.is(Blocks.SNOW) && belowState.getValue(SnowLayerBlock.LAYERS) >= 8) {
                level.setBlock(belowPos, Blocks.SNOW_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        if (target.currentLayers == 0) {
            if (!Blocks.SNOW.defaultBlockState().canSurvive(level, pos)) {
                return false;
            }
            if (target.replacingPlant) {
                BlockState existing = chunk.getBlockState(pos);
                if (existing.getBlock() instanceof DoublePlantBlock) {
                    BlockPos above = pos.above();
                    BlockState aboveState = chunk.getBlockState(above);
                    if (aboveState.getBlock() instanceof DoublePlantBlock
                            && aboveState.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) {
                        level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            }
            level.setBlock(pos, Blocks.SNOW.defaultBlockState(), Block.UPDATE_ALL);
            return true;
        }

        BlockState oldState = chunk.getBlockState(pos);
        if (!oldState.is(Blocks.SNOW)) {
            return false;
        }

        if (target.currentLayers < 7) {
            BlockState nextState = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, target.currentLayers + 1);
            Block.pushEntitiesUp(oldState, nextState, level, pos);
            level.setBlock(pos, nextState, Block.UPDATE_CLIENTS);
            return true;
        } else {
            BlockState fullSnowBlock = Blocks.SNOW_BLOCK.defaultBlockState();
            Block.pushEntitiesUp(oldState, fullSnowBlock, level, pos);
            level.setBlock(pos, fullSnowBlock, Block.UPDATE_ALL);

            // Check Leaf Crushing under heavy snow (>= 3 full blocks on top of leaves!)
            if (settings.leavesMode == 1 && settings.crushLeavesUnderWeight) {
                checkCrushLeavesBelow(level, chunk, pos);
            }
            return true;
        }
    }

    private static void checkCrushLeavesBelow(ServerLevel level, LevelChunk chunk, BlockPos topFullSnowPos) {
        int snowBlocks = 1;
        BlockPos.MutableBlockPos cursor = topFullSnowPos.mutable().move(Direction.DOWN);
        while (cursor.getY() > level.getMinBuildHeight() && snowBlocks < 5) {
            BlockState st = chunk.getBlockState(cursor);
            if (st.is(Blocks.SNOW_BLOCK)) {
                snowBlocks++;
                cursor.move(Direction.DOWN);
            } else {
                if (snowBlocks >= 3 && st.is(BlockTags.LEAVES)) {
                    // Leaves snap under >= 3 blocks of snow weight without laggy item drops!
                    level.setBlock(cursor, Blocks.SNOW_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                    level.setBlock(topFullSnowPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                break;
            }
        }
    }

    private static boolean thawOneLayer(ServerLevel level, LevelChunk chunk, ColumnTarget target) {
        if (target.currentLayers > 0) {
            BlockPos pos = new BlockPos(target.x, target.y, target.z);
            if (target.currentLayers == 1) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            } else {
                BlockState lower = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, target.currentLayers - 1);
                level.setBlock(pos, lower, Block.UPDATE_CLIENTS);
            }
            return true;
        }

        BlockPos below = new BlockPos(target.x, target.y - 1, target.z);
        if (below.getY() <= level.getMinBuildHeight()) {
            return false;
        }
        BlockState belowState = chunk.getBlockState(below);
        if (belowState.is(Blocks.SNOW_BLOCK) || belowState.is(Blocks.SNOW)) {
            BlockState sevenLayers = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 7);
            level.setBlock(below, sevenLayers, Block.UPDATE_ALL);
            return true;
        }
        return false;
    }

    private static int measureSnowLayersInColumn(
            LevelChunk chunk,
            int x,
            int activeY,
            int z,
            int topPartialLayers,
            int minBuildY,
            int maxScanBlocks
    ) {
        int totalLayers = topPartialLayers;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, activeY - 1, z);
        int scanned = 0;
        while (cursor.getY() >= minBuildY && scanned < maxScanBlocks) {
            BlockState state = chunk.getBlockState(cursor);
            if (state.is(Blocks.SNOW_BLOCK)) {
                totalLayers += 8;
            } else if (state.is(Blocks.SNOW)) {
                totalLayers += state.getValue(SnowLayerBlock.LAYERS);
            } else {
                break;
            }
            cursor.move(Direction.DOWN);
            scanned++;
        }
        return totalLayers;
    }

    private static int computeLocalTargetLayers(int x, int z, double averagePillarHeight) {
        if (averagePillarHeight <= 0.16D) {
            return 1;
        }
        double wave = (Math.sin(x * 0.11D + z * 0.07D) + Math.cos(x * 0.05D - z * 0.13D)) * 0.5D;
        double amplitude = Math.min(averagePillarHeight * 0.15D, 0.75D);
        double localBlocks = Math.max(0.125D, averagePillarHeight + wave * amplitude);
        return Math.max(1, (int) Math.round(localBlocks * 8.0D));
    }

    public static int clearSnowAroundPlayer(ServerPlayer player, int radius) {
        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        int cleared = 0;
        int cx = player.getBlockX();
        int cz = player.getBlockZ();
        int minY = level.getMinBuildHeight();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                if (chunk == null) {
                    continue;
                }
                int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                cursor.set(x, topY, z);
                if (chunk.getBlockState(cursor).is(Blocks.SNOW)) {
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    cleared++;
                }
                cursor.set(x, topY - 1, z);
                int safety = 0;
                while (cursor.getY() > minY && safety < 80) {
                    BlockState st = chunk.getBlockState(cursor);
                    if (st.is(Blocks.SNOW) || st.is(Blocks.SNOW_BLOCK)) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        cleared++;
                        cursor.move(Direction.DOWN);
                        safety++;
                    } else {
                        break;
                    }
                }
            }
        }
        return cleared;
    }

    private record ColumnTarget(
            int x,
            int y,
            int z,
            int currentLayers,
            boolean replacingPlant,
            boolean convertPathBelow
    ) {
    }
}
