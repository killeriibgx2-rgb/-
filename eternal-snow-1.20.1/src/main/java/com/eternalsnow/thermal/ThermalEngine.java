package com.eternalsnow.thermal;

import com.eternalsnow.block.FireplaceBlock;
import com.eternalsnow.block.FireplaceBlockEntity;
import com.eternalsnow.block.FireplaceFuelHelper;
import com.eternalsnow.block.FireplacePartBlock;
import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.registry.ModRegistry;
import com.eternalsnow.state.EternalSnowState;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;

/**
 * Maximum-Performance 3D Thermal Solver (v2.5.0):
 * - O(1) Per-Chunk Heat Emitter Index (replaces 25,000-block 3D player cube scans!).
 * - Zero-GC Ping-Pong maps & reusable candidate sets (0 hash-table allocations per tick!).
 * - Supports 3D Fireplace hearth radiation, open-door winter drafts (З-1), and warm-eave icicles (Д-2).
 */
public final class ThermalEngine {
    private static final Direction[] DIRS_6 = Direction.values();
    private static final Direction[] HORIZ_4 = new Direction[]{
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };
    private static final int MAX_ACTIVE_VOXELS_PER_LEVEL = 16000;

    // --- v2.5.2: lumped-capacitance room heating (З-2 «физика ограждений») -----------------
    // A closed room is flooded once (bounded BFS), its envelope conductance U is summed over
    // exterior faces as a SERIES resistance of up to MAX_WALL_DEPTH blocks: 2 wool layers halve U.
    // Heat is injected as power (not as a temperature clamp) and leaves ONLY through that envelope,
    // so wall material, wall thickness and wall heat capacity all change the measured numbers.
    private static final int MAX_ROOM_CELLS = 2048;
    private static final int MAX_WALL_DEPTH = 4;
    private static final float ROOM_OPEN_FACE_U = 1.20F;   // open door / window / hole in the roof
    private static final float ROOM_WALL_K_BASE = 0.32F;   // compressed conductivity: k = BASE + COND*cond
    private static final float ROOM_WALL_K_COND = 0.68F;   // wool ~0.354, planks ~0.483, stone ~0.810
    private static final float ROOM_POWER_GAIN = 1.51F;    // heat power -> delta °C per room cell per tick
    private static final float ROOM_LEAK_SCALE = 0.12F;    // U/volume -> per-substep heat loss
    private static final float ROOM_LEAK_MIN = 0.004F;     // always a tiny leak (no eternal thermos)
    private static final float ROOM_LEAK_MAX = 0.35F;
    private static final float ROOM_LEAK_FALLBACK = 0.02F; // for enclosed air outside any scanned room
    private static final float ROOM_CAP_INERTIA = 0.18F;   // wall capacity slows BOTH gain and loss (equilibrium unchanged)
    private static final int ROOM_REFRESH_TICKS = 40;      // re-scan geometry every 20 s

    private static final int[] DX26 = new int[26];
    private static final int[] DY26 = new int[26];
    private static final int[] DZ26 = new int[26];
    private static final int[] DIST2_26 = new int[26];
    private static final float[] WEIGHT26 = new float[26];

    static {
        int idx = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    int dist2 = dx * dx + dy * dy + dz * dz;
                    float w = (dist2 == 1) ? 1.00F : (dist2 == 2 ? 0.58F : 0.34F);
                    if (dy == 1) {
                        w *= 1.06F;
                    } else if (dy == -1) {
                        w *= 0.94F;
                    }
                    DX26[idx] = dx;
                    DY26[idx] = dy;
                    DZ26[idx] = dz;
                    DIST2_26[idx] = dist2;
                    WEIGHT26[idx] = w;
                    idx++;
                }
            }
        }
    }

    /** Cached geometry of one enclosed air volume (re-scanned on block changes / settings changes). */
    private static final class RoomData {
        final LongOpenHashSet air = new LongOpenHashSet();
        float conductance = 0.0F;   // Σ 1/Σ(1/kWall) over all exterior faces
        float capacitySum = 0.0F;
        int boundaryFaces = 0;
        float volume = 1.0F;
        float leakPerCell = ROOM_LEAK_FALLBACK;
        float inertia = 1.0F;
        float power = 0.0F;         // transient: summed source power for THIS tick only
        boolean valid = false;
    }

    private static final class LevelThermalWorkspace {
        Long2FloatOpenHashMap activeMap = createHeatMap();
        Long2FloatOpenHashMap backMap = createHeatMap();
        final LongOpenHashSet candidates = new LongOpenHashSet(4096);
        final LongOpenHashSet seenSources = new LongOpenHashSet(64);
        final Long2ObjectOpenHashMap<LongOpenHashSet> chunkEmitters = new Long2ObjectOpenHashMap<>();

        // v2.5.2 room model caches (all re-used, zero allocations in the hot tick loop)
        final Long2ObjectOpenHashMap<RoomData> roomCache = new Long2ObjectOpenHashMap<>();
        final Long2FloatOpenHashMap leakCache = createLeakMap();
        final LongOpenHashSet roomVisited = new LongOpenHashSet(MAX_ROOM_CELLS);
        final long[] roomQueue = new long[MAX_ROOM_CELLS];
        float settingsStamp = Float.NaN;
        int tickCounter = 0;

        private static Long2FloatOpenHashMap createHeatMap() {
            Long2FloatOpenHashMap m = new Long2FloatOpenHashMap(2048);
            m.defaultReturnValue(0.0F);
            return m;
        }

        private static Long2FloatOpenHashMap createLeakMap() {
            Long2FloatOpenHashMap m = new Long2FloatOpenHashMap(2048);
            m.defaultReturnValue(-1.0F);
            return m;
        }

        void invalidateGeometry() {
            roomCache.clear();
            leakCache.clear();
        }
    }

    private static final Map<ResourceKey<Level>, LevelThermalWorkspace> WORKSPACES = new HashMap<>();

    private ThermalEngine() {
    }

    private static LevelThermalWorkspace getWorkspace(ServerLevel level) {
        return WORKSPACES.computeIfAbsent(level.dimension(), k -> new LevelThermalWorkspace());
    }

    public static void clearAll() {
        WORKSPACES.clear();
    }

    public static boolean isPotentialHeatEmitter(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        Block b = state.getBlock();
        return b instanceof FireplaceBlock
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

    private static LongOpenHashSet getOrIndexChunkEmitters(ServerLevel level, LevelThermalWorkspace ws, LevelChunk chunk) {
        long cKey = chunk.getPos().toLong();
        LongOpenHashSet set = ws.chunkEmitters.get(cKey);
        if (set != null) {
            return set;
        }
        set = new LongOpenHashSet();
        LevelChunkSection[] sections = chunk.getSections();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        int minSec = chunk.getMinSection();

        for (int sIdx = 0; sIdx < sections.length; sIdx++) {
            LevelChunkSection sec = sections[sIdx];
            if (sec == null || sec.hasOnlyAir()) {
                continue;
            }
            int baseY = (minSec + sIdx) << 4;
            for (int ly = 0; ly < 16; ly++) {
                for (int lz = 0; lz < 16; lz++) {
                    for (int lx = 0; lx < 16; lx++) {
                        BlockState st = sec.getBlockState(lx, ly, lz);
                        if (isPotentialHeatEmitter(st)) {
                            set.add(BlockPos.asLong(baseX + lx, baseY + ly, baseZ + lz));
                        }
                    }
                }
            }
        }
        ws.chunkEmitters.put(cKey, set);
        return set;
    }

    public static void onBlockPlaced(ServerLevel level, BlockPos pos) {
        LevelThermalWorkspace ws = getWorkspace(level);
        ws.invalidateGeometry(); // a new wall layer must change room envelope resistance immediately
        long key = pos.asLong();
        ws.activeMap.remove(key);
        BlockState st = level.getBlockState(pos);
        long cKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        LongOpenHashSet set = ws.chunkEmitters.get(cKey);
        if (set != null) {
            if (isPotentialHeatEmitter(st)) {
                set.add(key);
            } else {
                set.remove(key);
            }
        }
    }

    public static void onBlockBroken(ServerLevel level, BlockPos pos) {
        LevelThermalWorkspace ws = getWorkspace(level);
        ws.invalidateGeometry();
        long key = pos.asLong();
        ws.activeMap.remove(key);
        long cKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        LongOpenHashSet set = ws.chunkEmitters.get(cKey);
        if (set != null) {
            set.remove(key);
        }
    }

    public static void onHeatSourceChanged(ServerLevel level, BlockPos pos, boolean active) {
        LevelThermalWorkspace ws = getWorkspace(level);
        long key = pos.asLong();
        long cKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        LongOpenHashSet set = ws.chunkEmitters.get(cKey);
        if (set != null) {
            BlockState st = level.getBlockState(pos);
            if (isPotentialHeatEmitter(st)) {
                set.add(key);
            }
        }
        if (!active) {
            ws.activeMap.remove(key);
        }
    }

    public static float getLocalTemperatureC(ServerLevel level, BlockPos pos) {
        float base = computeBaseAmbientC(level, pos);
        float delta = getWorkspace(level).activeMap.get(pos.asLong());
        return base + delta;
    }

    public static float getDeltaHeatAt(ServerLevel level, BlockPos pos) {
        return getWorkspace(level).activeMap.get(pos.asLong());
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

    public static void tickLevelThermal(ServerLevel level) {
        if (!EternalSnowState.isOverworldLike(level)) {
            return;
        }
        if (level.getGameTime() % 10L != 0L) {
            return;
        }

        ServerSnowSettings settings = EternalSnowState.getServerSettings();
        if (!settings.enabled || level.players().isEmpty()) {
            return;
        }

        LevelThermalWorkspace ws = getWorkspace(level);
        Long2FloatOpenHashMap heatMap = ws.activeMap;

        float speedMult = settings.globalHeatingSpeedMultiplier;
        BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos rayPos = new BlockPos.MutableBlockPos();

        ws.seenSources.clear();
        int scanR = 16;
        int scanY = 10;

        // 1. O(1) Chunk-Indexed Heat Emitter Discovery (zero 25,000-block scans!)
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            int px = player.getBlockX();
            int py = player.getBlockY();
            int pz = player.getBlockZ();
            int minCx = (px - scanR) >> 4;
            int maxCx = (px + scanR) >> 4;
            int minCz = (pz - scanR) >> 4;
            int maxCz = (pz + scanR) >> 4;

            for (int cx = minCx; cx <= maxCx; cx++) {
                for (int cz = minCz; cz <= maxCz; cz++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) {
                        continue;
                    }
                    LongOpenHashSet emitters = getOrIndexChunkEmitters(level, ws, chunk);
                    if (emitters.isEmpty()) {
                        continue;
                    }
                    LongIterator eIt = emitters.iterator();
                    while (eIt.hasNext()) {
                        long srcKey = eIt.nextLong();
                        if (ws.seenSources.contains(srcKey)) {
                            continue;
                        }
                        int sx = BlockPos.getX(srcKey);
                        int sy = BlockPos.getY(srcKey);
                        int sz = BlockPos.getZ(srcKey);
                        if (Math.abs(sx - px) > scanR || Math.abs(sz - pz) > scanR || Math.abs(sy - py) > scanY) {
                            continue;
                        }
                        scanPos.set(sx, sy, sz);
                        BlockState st = chunk.getBlockState(scanPos);
                        if (!isPotentialHeatEmitter(st)) {
                            eIt.remove();
                            continue;
                        }
                        float sourcePower = getHeatSourcePowerC(level, scanPos, st, settings);
                        if (sourcePower > 0.5F) {
                            ws.seenSources.add(srcKey);
                            heatMap.put(srcKey, Math.max(sourcePower, heatMap.get(srcKey)));
                            boolean isFireplace = st.getBlock() instanceof FireplaceBlock;
                            injectSphericalRadiation(level, heatMap, sx, sy, sz, sourcePower, speedMult, isFireplace ? 4 : 3, scanPos, rayPos);
                        }
                    }
                }
            }
        }

        // 1b. v2.5.2: room geometry caches are invalidated when admin changes material physics
        //     and refreshed periodically (snow on the roof, growing walls, etc.).
        float stamp = conductivityStamp(settings);
        if (Float.isNaN(ws.settingsStamp) || ws.settingsStamp != stamp) {
            ws.invalidateGeometry();
            ws.settingsStamp = stamp;
        }
        ws.tickCounter++;
        float roomCapDelta = Math.max(6.0F, settings.roomComfortCeilingC - EternalSnowState.worldAmbientTempC);
        if (ws.tickCounter % ROOM_REFRESH_TICKS == 0) {
            refreshRooms(level, ws, settings, scanPos, rayPos);
        }
        applyEnclosedRoomHeating(level, ws, heatMap, settings, roomCapDelta, scanPos, rayPos);

        if (heatMap.isEmpty()) {
            return;
        }

        // 2. Zero-GC Ping-Pong 26-Neighbor Isotropic Air Convection + Wall Conduction
        float stepRate = Math.min(1.85F, 0.65F + 0.35F * speedMult);
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos nPos = new BlockPos.MutableBlockPos();

        for (int sub = 0; sub < 2; sub++) {
            LongOpenHashSet candidates = ws.candidates;
            candidates.clear();

            for (Long2FloatMap.Entry entry : heatMap.long2FloatEntrySet()) {
                long key = entry.getLongKey();
                float val = entry.getFloatValue();
                if (val < 0.18F) {
                    continue;
                }
                candidates.add(key);
                if (val >= 0.45F && candidates.size() < MAX_ACTIVE_VOXELS_PER_LEVEL) {
                    int x = BlockPos.getX(key);
                    int y = BlockPos.getY(key);
                    int z = BlockPos.getZ(key);
                    for (int i = 0; i < 26; i++) {
                        if (DIST2_26[i] == 1 || val >= 1.8F) {
                            candidates.add(BlockPos.asLong(x + DX26[i], y + DY26[i], z + DZ26[i]));
                        }
                    }
                }
            }

            Long2FloatOpenHashMap nextMap = ws.backMap;
            nextMap.clear();

            LongIterator it = candidates.iterator();
            while (it.hasNext()) {
                long key = it.nextLong();
                int x = BlockPos.getX(key);
                int y = BlockPos.getY(key);
                int z = BlockPos.getZ(key);
                LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                if (chunk == null) {
                    continue;
                }

                mPos.set(x, y, z);
                BlockState state = chunk.getBlockState(mPos);
                float u = heatMap.get(key);

                float srcPower = getHeatSourcePowerC(level, mPos, state, settings);
                if (srcPower > 0.5F) {
                    nextMap.put(key, Math.max(srcPower, u * 0.92F));
                    continue;
                }

                if (isAirOrOpenPassage(state)) {
                    // v2.5.2: an enclosed air cell loses heat ONLY through its room envelope
                    // (wall material + wall thickness), not through a universal fudge decay.
                    boolean skyHere = level.canSeeSky(mPos);
                    if (skyHere) {
                        float windBleed = 0.20F + EternalSnowState.windSpeed * 0.14F;
                        u *= (1.0F - Mth.clamp(windBleed, 0.18F, 0.36F));
                    } else {
                        float leak = ws.leakCache.get(key);
                        if (leak < 0.0F) {
                            leak = ROOM_LEAK_FALLBACK;
                        }
                        u *= (1.0F - leak);
                    }

                    if (!skyHere && ((state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.OPEN))
                            || (state.getBlock() instanceof TrapDoorBlock && state.getValue(TrapDoorBlock.OPEN)))) {
                        // З-1: Open Door Draft («Сквозняк»)! Leaving an exterior door open lets cold gusts pull warmth out!
                        for (Direction hd : HORIZ_4) {
                            nPos.set(x + hd.getStepX(), y, z + hd.getStepZ());
                            if (level.canSeeSky(nPos) || heatMap.get(nPos.asLong()) < 1.2F) {
                                u *= (1.0F - (0.14F + EternalSnowState.windSpeed * 0.22F));
                                break;
                            }
                        }
                    }

                    float weightSum = 0.0F;
                    float netFlow = 0.0F;

                    for (int i = 0; i < 26; i++) {
                        int dx = DX26[i];
                        int dy = DY26[i];
                        int dz = DZ26[i];
                        int dist2 = DIST2_26[i];

                        int nx = x + dx;
                        int ny = y + dy;
                        int nz = z + dz;

                        BlockState nState = getFastBlockState(level, chunk, nx, ny, nz, nPos);
                        if (nState == null) {
                            continue;
                        }

                        if (isAirOrOpenPassage(nState)) {
                            if (dist2 == 2) {
                                if (!isDiagonalAirOpen2D(level, chunk, x, y, z, dx, dy, dz, nPos)) {
                                    continue;
                                }
                            } else if (dist2 == 3) {
                                if (!isDiagonalAirOpen3D(level, chunk, x, y, z, dx, dy, dz, nPos)) {
                                    continue;
                                }
                            }
                            float nu = heatMap.get(BlockPos.asLong(nx, ny, nz));
                            float airK = 0.28F * WEIGHT26[i] * stepRate;
                            netFlow += (nu - u) * airK;
                            weightSum += airK;
                        } else if (dist2 == 1) {
                            BlockThermalCategory cat = BlockThermalCategory.classify(nState);
                            float cond = settings.blockConductivity[cat.ordinal()];
                            float nu = heatMap.get(BlockPos.asLong(nx, ny, nz));
                            float wallK = 0.058F * cond;
                            netFlow += (nu - u) * wallK;
                            weightSum += wallK;
                        }
                    }

                    if (weightSum > 0.86F) {
                        netFlow *= (0.86F / weightSum);
                    }

                    float updated = (u + netFlow) * (skyHere ? 0.998F : 0.9999F);
                    if (!skyHere && updated > roomCapDelta) {
                        updated = roomCapDelta; // comfort ceiling for enclosed air only
                    }
                    if (updated >= 0.18F) {
                        nextMap.put(key, updated);
                    }
                } else {
                    BlockThermalCategory cat = BlockThermalCategory.classify(state);
                    float selfCond = settings.blockConductivity[cat.ordinal()];
                    float selfCap = Math.max(0.25F, settings.blockCapacity[cat.ordinal()]);

                    float netHeatFlux = 0.0F;
                    for (Direction d : DIRS_6) {
                        int nx = x + d.getStepX();
                        int ny = y + d.getStepY();
                        int nz = z + d.getStepZ();
                        BlockState nState = getFastBlockState(level, chunk, nx, ny, nz, nPos);
                        if (nState == null) {
                            continue;
                        }
                        float nu = heatMap.get(BlockPos.asLong(nx, ny, nz));
                        nPos.set(nx, ny, nz);

                        if (isAirOrOpenPassage(nState)) {
                            if (nu < 0.8F && level.canSeeSky(nPos)) {
                                netHeatFlux -= u * (0.14F * selfCond);
                            } else {
                                netHeatFlux += (nu - u) * (0.105F * selfCond);
                            }
                        } else {
                            BlockThermalCategory nCat = BlockThermalCategory.classify(nState);
                            float pairCond = Math.min(selfCond, settings.blockConductivity[nCat.ordinal()]);
                            netHeatFlux += (nu - u) * (0.065F * pairCond);
                        }
                    }

                    float updated = (u + (netHeatFlux * stepRate) / selfCap) * 0.9985F;
                    if (updated >= 0.15F) {
                        nextMap.put(key, updated);

                        if (sub == 1) {
                            if (settings.meltRoofSnowFromWarmBlocks && level.random.nextInt(4) == 0) {
                                float blockActualTempC = EternalSnowState.worldAmbientTempC + updated;
                                if (blockActualTempC > 2.0F) {
                                    meltOneSnowLayerAt(level, mPos.above());
                                    // Д-2: Occasional natural Icicle growth under warm snow-covered roof eaves!
                                    if (EternalSnowState.worldAmbientTempC < -2.0F && level.random.nextInt(32) == 0) {
                                        tryGrowEaveIcicle(level, mPos);
                                    }
                                }
                            }
                            if ((state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK))
                                    && (EternalSnowState.worldAmbientTempC + updated) > 1.0F
                                    && level.random.nextInt(3) == 0) {
                                meltOneSnowLayerAt(level, mPos);
                            }
                        }
                    }
                }
            }

            // Ping-pong swap without zeroing or allocating!
            ws.backMap = heatMap;
            ws.activeMap = nextMap;
            heatMap = nextMap;
        }
    }

    // =====================================================================================
    // v2.5.2: Enclosed-room lumped heat model (power in, envelope resistance out)
    // =====================================================================================

    /** Cheap checksum so an admin conductivity/capacity change invalidates cached geometry. */
    private static float conductivityStamp(ServerSnowSettings s) {
        float h = 0.0F;
        for (float v : s.blockConductivity) {
            h = h * 1.07F + v;
        }
        for (float v : s.blockCapacity) {
            h = h * 1.07F + v;
        }
        return h + s.roomHeatingPowerPercent * 0.013F;
    }

    /** First enclosed air neighbour of a heat source (front face preferred for the 3x2x2 hearth). */
    private static long findRoomEntry(ServerLevel level, int sx, int sy, int sz,
                                      BlockState srcState, BlockPos.MutableBlockPos tmp) {
        if (srcState.getBlock() instanceof FireplaceBlock && srcState.hasProperty(FireplaceBlock.FACING)) {
            Direction f = srcState.getValue(FireplaceBlock.FACING);
            tmp.set(sx + f.getStepX(), sy + f.getStepY(), sz + f.getStepZ());
            BlockState front = level.getBlockState(tmp);
            if (isAirOrOpenPassage(front) && !level.canSeeSky(tmp)) {
                return tmp.asLong();
            }
        }
        for (Direction d : DIRS_6) {
            tmp.set(sx + d.getStepX(), sy + d.getStepY(), sz + d.getStepZ());
            BlockState st = level.getBlockState(tmp);
            if (isAirOrOpenPassage(st) && !level.canSeeSky(tmp)) {
                return tmp.asLong();
            }
        }
        return Long.MIN_VALUE; // outdoors: fine-grained radiation solver handles it
    }

    /**
     * Bounded flood-fill of the enclosed air volume + series-resistance envelope scan.
     * Runs only on a cache miss (block change, settings change or every 20 s), never per tick.
     */
    private static RoomData scanRoom(ServerLevel level, LevelThermalWorkspace ws, long entryKey,
                                     ServerSnowSettings settings,
                                     BlockPos.MutableBlockPos tmp, BlockPos.MutableBlockPos tmp2) {
        RoomData room = new RoomData();
        LongOpenHashSet visited = ws.roomVisited;
        long[] queue = ws.roomQueue;
        visited.clear();
        int head = 0;
        int tail = 0;
        visited.add(entryKey);
        queue[tail++] = entryKey;
        boolean tooBig = false;

        while (head < tail) {
            long key = queue[head++];
            int x = BlockPos.getX(key);
            int y = BlockPos.getY(key);
            int z = BlockPos.getZ(key);
            room.air.add(key);

            for (Direction d : DIRS_6) {
                int dx = d.getStepX();
                int dy = d.getStepY();
                int dz = d.getStepZ();
                int nx = x + dx;
                int ny = y + dy;
                int nz = z + dz;
                tmp.set(nx, ny, nz);
                BlockState nState = level.getBlockState(tmp);

                if (isAirOrOpenPassage(nState)) {
                    if (level.canSeeSky(tmp)) {
                        // Open door / window / chimney hole: strong convective leak, no expansion.
                        room.conductance += ROOM_OPEN_FACE_U;
                        room.boundaryFaces++;
                        continue;
                    }
                    long nKey = BlockPos.asLong(nx, ny, nz);
                    if (!visited.contains(nKey)) {
                        if (tail >= MAX_ROOM_CELLS) {
                            tooBig = true;
                            break;
                        }
                        visited.add(nKey);
                        queue[tail++] = nKey;
                    }
                    continue;
                }

                // Solid neighbour: walk the wall as a SERIES of thermal resistances.
                // 1 wool block -> R = 1/0.354; 2 wool blocks -> R = 2/0.354 (U halves!).
                float resistance = 0.0F;
                boolean outside = false;
                for (int step = 0; step < MAX_WALL_DEPTH; step++) {
                    tmp2.set(nx + dx * step, ny + dy * step, nz + dz * step);
                    BlockState wall = level.getBlockState(tmp2);
                    if (isAirOrOpenPassage(wall)) {
                        outside = reachesOutsideAir(level, tmp2.getX(), tmp2.getY(), tmp2.getZ(), dx, dy, dz, tmp2);
                        break;
                    }
                    BlockThermalCategory cat = BlockThermalCategory.classify(wall);
                    float cond = settings.blockConductivity[cat.ordinal()];
                    float kWall = ROOM_WALL_K_BASE + ROOM_WALL_K_COND * cond;
                    resistance += 1.0F / Math.max(0.02F, kWall);
                    if (step == 0) {
                        room.capacitySum += settings.blockCapacity[cat.ordinal()];
                        room.boundaryFaces++;
                    }
                }
                if (outside) {
                    room.conductance += 1.0F / Math.max(0.02F, resistance);
                }
            }
            if (tooBig) {
                break;
            }
        }

        room.valid = !tooBig && !room.air.isEmpty();
        if (!room.valid) {
            return room;
        }
        room.volume = Math.max(1.0F, room.air.size());
        float capAvg = room.boundaryFaces > 0 ? room.capacitySum / room.boundaryFaces : 2.0F;
        room.inertia = 1.0F / (1.0F + capAvg * ROOM_CAP_INERTIA);
        room.leakPerCell = Mth.clamp(
                (room.conductance / room.volume) * ROOM_LEAK_SCALE * room.inertia,
                ROOM_LEAK_MIN, ROOM_LEAK_MAX);

        // Mark these air cells as OWNED by the lumped room model: the fine-grained solver must not
        // apply a second, independent leak to them (that would double-count the heat loss).
        LongIterator airIt = room.air.iterator();
        while (airIt.hasNext()) {
            ws.leakCache.put(airIt.nextLong(), 0.0F);
        }
        return room;
    }

    /**
     * The air right behind a wall can sit under a roof overhang: canSeeSky() is false there,
     * but it is still outdoor air. Walk a few blocks further; a second solid means an enclosed
     * cavity (a real insulator), a sky-visible cell means the wall really loses heat outside.
     */
    private static boolean reachesOutsideAir(ServerLevel level, int x, int y, int z,
                                             int dx, int dy, int dz, BlockPos.MutableBlockPos tmp) {
        for (int i = 0; i < 3; i++) {
            tmp.set(x + dx * i, y + dy * i, z + dz * i);
            BlockState st = level.getBlockState(tmp);
            if (!isAirOrOpenPassage(st)) {
                return false;
            }
            if (level.canSeeSky(tmp)) {
                return true;
            }
        }
        return false;
    }

    /** Periodic geometry re-scan (snow on the roof, new walls) without losing the room cache. */
    private static void refreshRooms(ServerLevel level, LevelThermalWorkspace ws,
                                     ServerSnowSettings settings,
                                     BlockPos.MutableBlockPos tmp, BlockPos.MutableBlockPos tmp2) {
        if (ws.roomCache.isEmpty()) {
            return;
        }
        long[] keys = new long[ws.roomCache.size()];
        int n = 0;
        LongIterator keyIt = ws.roomCache.keySet().iterator();
        while (keyIt.hasNext()) {
            keys[n++] = keyIt.nextLong();
        }
        ws.leakCache.clear();
        for (int i = 0; i < n; i++) {
            ws.roomCache.put(keys[i], scanRoom(level, ws, keys[i], settings, tmp, tmp2));
        }
    }

    /**
     * Injects fireplace/engine POWER uniformly into every enclosed air cell of the room.
     * Equilibrium is power / (2 * U * leakScale): doubling wall thickness halves U and therefore
     * roughly doubles the temperature above ambient (until the comfort ceiling is reached).
     */
    private static void applyEnclosedRoomHeating(ServerLevel level, LevelThermalWorkspace ws,
                                                 Long2FloatOpenHashMap heatMap,
                                                 ServerSnowSettings settings, float roomCapDelta,
                                                 BlockPos.MutableBlockPos tmp,
                                                 BlockPos.MutableBlockPos tmp2) {
        // Reset per-tick power for every known room (rooms with no fire must keep cooling down).
        for (RoomData r : ws.roomCache.values()) {
            r.power = 0.0F;
        }
        if (ws.roomCache.size() > 32) {
            ws.invalidateGeometry();
        }

        LongIterator srcIt = ws.seenSources.iterator();
        while (srcIt.hasNext()) {
            long srcKey = srcIt.nextLong();
            int sx = BlockPos.getX(srcKey);
            int sy = BlockPos.getY(srcKey);
            int sz = BlockPos.getZ(srcKey);
            tmp.set(sx, sy, sz);
            BlockState srcState = level.getBlockState(tmp);
            float power = getHeatSourcePowerC(level, tmp, srcState, settings);
            if (power <= 0.5F) {
                continue;
            }
            long entry = findRoomEntry(level, sx, sy, sz, srcState, tmp);
            if (entry == Long.MIN_VALUE) {
                continue;
            }

            RoomData room = ws.roomCache.get(entry);
            if (room == null) {
                // Two different sources in the SAME room must share one RoomData, not scan it twice.
                for (RoomData known : ws.roomCache.values()) {
                    if (known.valid && known.air.contains(entry)) {
                        room = known;
                        break;
                    }
                }
            }
            if (room == null) {
                room = scanRoom(level, ws, entry, settings, tmp, tmp2);
                ws.roomCache.put(entry, room);
            }
            if (room.valid) {
                room.power += power;
            }
        }

        // Lumped-capacitance update: one uniform room temperature, power in / envelope resistance out.
        // δT_eq = gain / (2 * leakPerCell). Doubling wall thickness halves leakPerCell, so δT doubles
        // until the comfort ceiling is reached. Wall capacity scales BOTH gain and leak (inertia),
        // i.e. it changes warm-up / cool-down time but NOT the steady temperature — correct physics.
        float powerScale = ROOM_POWER_GAIN * (settings.roomHeatingPowerPercent / 100.0F);
        for (RoomData room : ws.roomCache.values()) {
            if (!room.valid || room.air.isEmpty()) {
                continue;
            }
            float sum = 0.0F;
            LongIterator airIt = room.air.iterator();
            while (airIt.hasNext()) {
                sum += heatMap.get(airIt.nextLong());
            }
            float avg = sum / room.volume;
            float gain = room.power * powerScale * room.inertia / room.volume;
            float keep = Math.max(0.0F, 1.0F - 2.0F * room.leakPerCell);
            float val = Math.min(roomCapDelta, (avg + gain) * keep);

            airIt = room.air.iterator();
            if (val < 0.18F) {
                while (airIt.hasNext()) {
                    heatMap.remove(airIt.nextLong());
                }
            } else {
                while (airIt.hasNext()) {
                    heatMap.put(airIt.nextLong(), val);
                }
            }
        }
    }

    private static void tryGrowEaveIcicle(ServerLevel level, BlockPos warmRoofPos) {
        if (!ModRegistry.ICICLE.isPresent()) {
            return;
        }
        Direction dir = HORIZ_4[level.random.nextInt(4)];
        BlockPos eaveEdge = warmRoofPos.relative(dir);
        BlockPos underEave = eaveEdge.below();
        if (!level.getBlockState(underEave).isAir()) {
            return;
        }
        if (!level.canSeeSky(eaveEdge.above())) {
            return;
        }
        BlockState icicleState = ModRegistry.ICICLE.get().defaultBlockState();
        if (icicleState.canSurvive(level, underEave)) {
            level.setBlock(underEave, icicleState, Block.UPDATE_CLIENTS);
        }
    }

    private static void injectSphericalRadiation(
            ServerLevel level,
            Long2FloatOpenHashMap heatMap,
            int sx, int sy, int sz,
            float sourcePower,
            float speedMult,
            int radius,
            BlockPos.MutableBlockPos checkPos,
            BlockPos.MutableBlockPos rayPos
    ) {
        int maxD2 = (radius >= 4) ? 18 : 11;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    int d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 > maxD2) {
                        continue;
                    }
                    int tx = sx + dx;
                    int ty = sy + dy;
                    int tz = sz + dz;
                    LevelChunk chunk = level.getChunkSource().getChunkNow(tx >> 4, tz >> 4);
                    if (chunk == null) {
                        continue;
                    }
                    checkPos.set(tx, ty, tz);
                    BlockState targetState = chunk.getBlockState(checkPos);

                    if (isAirOrOpenPassage(targetState)) {
                        if (!hasClearLineOfSight(level, sx, sy, sz, tx, ty, tz, rayPos)) {
                            continue;
                        }
                        long key = BlockPos.asLong(tx, ty, tz);
                        float cur = heatMap.get(key);
                        boolean sky = level.canSeeSky(checkPos);
                        float falloff = (radius >= 4) ? 0.24F : 0.32F;
                        float radTarget = sky
                                ? sourcePower * (0.76F / (1.0F + 0.48F * d2))
                                : sourcePower * (0.78F / (1.0F + falloff * d2));
                        if (radTarget > cur) {
                            float alpha = sky
                                    ? Math.min(0.85F, 0.55F * speedMult)
                                    : Math.min(0.75F, 0.34F * speedMult);
                            heatMap.put(key, cur + (radTarget - cur) * alpha);
                        }
                    } else if (d2 == 1) {
                        ServerSnowSettings s = EternalSnowState.getServerSettings();
                        BlockThermalCategory cat = BlockThermalCategory.classify(targetState);
                        float cond = s.blockConductivity[cat.ordinal()];
                        float cap = Math.max(0.25F, s.blockCapacity[cat.ordinal()]);
                        long key = BlockPos.asLong(tx, ty, tz);
                        float cur = heatMap.get(key);
                        float gain = (sourcePower - cur) * (0.20F * cond * speedMult) / cap;
                        if (gain > 0.02F) {
                            heatMap.put(key, cur + gain);
                        }
                    }
                }
            }
        }
    }

    private static boolean hasClearLineOfSight(
            ServerLevel level,
            int sx, int sy, int sz,
            int tx, int ty, int tz,
            BlockPos.MutableBlockPos rayPos
    ) {
        int dx = tx - sx;
        int dy = ty - sy;
        int dz = tz - sz;
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) * 2;
        if (steps <= 2) {
            int dist2 = dx * dx + dy * dy + dz * dz;
            if (dist2 == 1) {
                return true;
            }
            LevelChunk chunk = level.getChunkSource().getChunkNow(sx >> 4, sz >> 4);
            if (chunk == null) {
                return false;
            }
            if (dist2 == 2) {
                return isDiagonalAirOpen2D(level, chunk, sx, sy, sz, dx, dy, dz, rayPos);
            }
            return isDiagonalAirOpen3D(level, chunk, sx, sy, sz, dx, dy, dz, rayPos);
        }

        for (int i = 1; i < steps; i++) {
            float t = i / (float) steps;
            int bx = Math.round(sx + dx * t);
            int by = Math.round(sy + dy * t);
            int bz = Math.round(sz + dz * t);
            if ((bx == sx && by == sy && bz == sz) || (bx == tx && by == ty && bz == tz)) {
                continue;
            }
            LevelChunk chunk = level.getChunkSource().getChunkNow(bx >> 4, bz >> 4);
            if (chunk == null) {
                return false;
            }
            rayPos.set(bx, by, bz);
            BlockState mid = chunk.getBlockState(rayPos);
            // FireplacePartBlock is an INVISIBLE proxy of the same 3x2x2 hearth:
            // radiation must pass through it, otherwise the fire is walled in by its own model.
            if (!isAirOrOpenPassage(mid)
                    && !(mid.getBlock() instanceof FireplaceBlock)
                    && !(mid.getBlock() instanceof FireplacePartBlock)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDiagonalAirOpen2D(
            ServerLevel level,
            LevelChunk baseChunk,
            int x, int y, int z,
            int dx, int dy, int dz,
            BlockPos.MutableBlockPos tmp
    ) {
        BlockState sA;
        BlockState sB;
        if (dx != 0 && dz != 0) {
            sA = getFastBlockState(level, baseChunk, x + dx, y, z, tmp);
            sB = getFastBlockState(level, baseChunk, x, y, z + dz, tmp);
        } else if (dx != 0) {
            sA = getFastBlockState(level, baseChunk, x + dx, y, z, tmp);
            sB = getFastBlockState(level, baseChunk, x, y + dy, z, tmp);
        } else {
            sA = getFastBlockState(level, baseChunk, x, y + dy, z, tmp);
            sB = getFastBlockState(level, baseChunk, x, y, z + dz, tmp);
        }
        return (sA != null && isAirOrOpenPassage(sA)) || (sB != null && isAirOrOpenPassage(sB));
    }

    private static boolean isDiagonalAirOpen3D(
            ServerLevel level,
            LevelChunk baseChunk,
            int x, int y, int z,
            int dx, int dy, int dz,
            BlockPos.MutableBlockPos tmp
    ) {
        BlockState sX = getFastBlockState(level, baseChunk, x + dx, y, z, tmp);
        BlockState sY = getFastBlockState(level, baseChunk, x, y + dy, z, tmp);
        BlockState sZ = getFastBlockState(level, baseChunk, x, y, z + dz, tmp);
        return (sX != null && isAirOrOpenPassage(sX))
                || (sY != null && isAirOrOpenPassage(sY))
                || (sZ != null && isAirOrOpenPassage(sZ));
    }

    private static BlockState getFastBlockState(
            ServerLevel level,
            LevelChunk baseChunk,
            int x, int y, int z,
            BlockPos.MutableBlockPos tmp
    ) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
            return null;
        }
        LevelChunk c = ((x >> 4) == baseChunk.getPos().x && (z >> 4) == baseChunk.getPos().z)
                ? baseChunk
                : level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (c == null) {
            return null;
        }
        tmp.set(x, y, z);
        return c.getBlockState(tmp);
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
}
