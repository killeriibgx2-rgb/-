package com.eternalsnow.thermal.cells;

import com.eternalsnow.block.HeatAccumulatorBlock;
import com.eternalsnow.block.HeatAccumulatorBlockEntity;
import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.thermal.BlockThermalCategory;
import com.eternalsnow.thermal.ThermalEngine;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Minecraft adapter. Single server-thread owner; reads never force chunks to load. */
public final class WorldCellThermal {
    private static final Map<ResourceKey<Level>, Workspace> WORLDS = new ConcurrentHashMap<>();
    private static final Direction[] FORWARD = {Direction.EAST,Direction.UP,Direction.SOUTH};
    private static final Direction[] ALL = Direction.values();
    private static final int EXPANSION_BUDGET = 512;

    private static final class Workspace {
        final ConcurrentLinkedQueue<Long> pending = new ConcurrentLinkedQueue<>();
        final LongOpenHashSet loaded = new LongOpenHashSet();
        final LongOpenHashSet sources = new LongOpenHashSet();
        final LongArrayFIFOQueue scanQueue = new LongArrayFIFOQueue();
        final Long2ObjectOpenHashMap<BlockState> lastStates = new Long2ObjectOpenHashMap<>();
        CellHeatSolver solver;
        BlockPos[] positions = new BlockPos[0];
        Long2IntOpenHashMap indices = new Long2IntOpenHashMap();
        BlockState[] states = new BlockState[0];
        double[] conductivity = new double[0];
        double[] powers = new double[0];
        boolean[] air = new boolean[0];
        final it.unimi.dsi.fastutil.ints.IntOpenHashSet emitters = new it.unimi.dsi.fastutil.ints.IntOpenHashSet();
        final LongOpenHashSet changed = new LongOpenHashSet();
        final LongOpenHashSet replaced = new LongOpenHashSet();
        final LongOpenHashSet imported = new LongOpenHashSet();
        final IdentityHashMap<BlockState,Material> materials = new IdentityHashMap<>();
        boolean dirty = true;
        long expansionCursor;
        int settingsHash;
    }
    private static Workspace ws(ServerLevel level) { return WORLDS.computeIfAbsent(level.dimension(), k -> new Workspace()); }
    public static void clearAll() { WORLDS.clear(); }
    public static void load(ServerLevel level,long chunk) { ws(level).pending.add(chunk); }
    public static void unload(ServerLevel level,long chunk) {
        Workspace w=ws(level);
        boolean admitted=w.loaded.remove(chunk);
        w.pending.removeIf(key -> key.longValue()==chunk);
        w.imported.remove(chunk);
        var stored=CellThermalSavedData.get(level).chunks.get(chunk);
        // Empty terrain chunks have no graph topology to compact.
        if(admitted && stored!=null && !stored.isEmpty())w.dirty=true;
        w.sources.removeIf(key -> chunkKey(BlockPos.of(key))==chunk);
        w.lastStates.keySet().removeIf(key -> chunkKey(BlockPos.of(key))==chunk);
    }
    private static long chunkKey(BlockPos pos) { return ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4); }
    private static boolean ready(ServerLevel level,BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;
    }
    private static void admit(ServerLevel level,Workspace w,BlockPos pos) {
        long key=chunkKey(pos);
        if (w.loaded.add(key)) { w.scanQueue.enqueue(key); w.imported.add(key); }
    }
    public static double temperature(ServerLevel level,BlockPos pos) {
        if (!ready(level,pos)) {
            var stored=CellThermalSavedData.get(level).chunks.get(chunkKey(pos));
            double t=stored==null?Double.NaN:stored.get(pos.asLong());
            return Double.isNaN(t)?EternalSnowState.worldAmbientTempC:t;
        }
        Workspace w=ws(level); admit(level,w,pos);
        return ensure(level,w,CellThermalSavedData.get(level),pos);
    }
    private static double ensure(ServerLevel level,Workspace w,CellThermalSavedData data,BlockPos pos) {
        var cells=data.chunk(chunkKey(pos)); double t=cells.get(pos.asLong());
        if (Double.isNaN(t)) {
            t=ThermalEngine.computeBaseAmbientC(level,pos);
            if (level.getBlockEntity(pos) instanceof HeatAccumulatorBlockEntity accumulator)
                t+=Math.max(0,accumulator.getHeatC()); // one-time migration; existing cell wins thereafter
            cells.put(pos.asLong(),t); data.setDirty(); w.changed.add(pos.asLong());
        }
        return t;
    }
    private static double localAir(ServerLevel level,CellThermalSavedData data,BlockPos pos) {
        double sum=0; int count=0;
        for (Direction d:ALL) {
            BlockPos n=pos.relative(d);
            if (!ready(level,n) || !ThermalEngine.isAirOrOpenPassage(level.getBlockState(n))) continue;
            var cells=data.chunks.get(chunkKey(n));
            double t=cells==null?Double.NaN:cells.get(n.asLong());
            if (!Double.isNaN(t)) { sum+=t; count++; }
        }
        return count==0?ThermalEngine.computeBaseAmbientC(level,pos):sum/count;
    }
    public static void replaced(ServerLevel level,BlockPos pos) {
        if (!ready(level,pos)) return;
        Workspace w=ws(level); admit(level,w,pos);
        var data=CellThermalSavedData.get(level);
        data.chunk(chunkKey(pos)).put(pos.asLong(),localAir(level,data,pos));
        data.setDirty(); w.changed.add(pos.asLong()); w.replaced.add(pos.asLong()); w.lastStates.remove(pos.asLong());
        w.sources.add(pos.asLong()); // validate after the placement/break event completes
    }
    public static void blockChanged(ServerLevel level, BlockPos pos, BlockState before, BlockState after) {
        if (!EternalSnowState.isOverworldLike(level) || !ready(level,pos)) return;
        Workspace w=ws(level);
        boolean emitter=ThermalEngine.isPotentialHeatEmitter(after);
        // Do not materialise every natural snowfall block. Only existing thermal cells
        // and newly introduced emitters need state; source indexing handles loaded chunks.
        var map=CellThermalSavedData.get(level).chunks.get(chunkKey(pos));
        if (!emitter && (map==null || !map.containsKey(pos.asLong()))) return;
        if (before.getBlock()!=after.getBlock()) replaced(level,pos);
        else w.changed.add(pos.asLong());
        // This mutation has already been handled, don't reset it a second time on rebuild.
        w.lastStates.put(pos.asLong(),after);
        if(emitter) w.sources.add(pos.asLong());
        else w.sources.remove(pos.asLong());
    }
    public static void sourceChanged(ServerLevel level,BlockPos pos) {
        if (!ready(level,pos)) return;
        Workspace w=ws(level); admit(level,w,pos);
        w.sources.add(pos.asLong());
        ensure(level,w,CellThermalSavedData.get(level),pos); // never resets an existing temperature
    }
    private static void seed(ServerLevel level,Workspace w,CellThermalSavedData data,BlockPos center,int radius) {
        for (int x=-radius;x<=radius;x++) for(int y=-radius;y<=radius;y++) for(int z=-radius;z<=radius;z++) {
            BlockPos p=center.offset(x,y,z);
            if (ready(level,p)) { admit(level,w,p); ensure(level,w,data,p); }
        }
    }
    private static void scanSources(ServerLevel level,Workspace w) {
        // At most one chunk index per thermal step. Palettes skip sections with no emitters.
        if (w.scanQueue.isEmpty()) return;
        long key=w.scanQueue.dequeueLong();
        LevelChunk chunk=level.getChunkSource().getChunkNow(ChunkPos.getX(key),ChunkPos.getZ(key));
        if(chunk==null || !w.loaded.contains(key)) return;
        var sections=chunk.getSections();
        for(int i=0;i<sections.length;i++) {
            var section=sections[i];
            if(section.hasOnlyAir() || !section.maybeHas(ThermalEngine::isPotentialHeatEmitter)) continue;
            int baseY=(chunk.getMinSection()+i)*16;
            for(int y=0;y<16;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++)
                if(ThermalEngine.isPotentialHeatEmitter(section.getBlockState(x,y,z)))
                    w.sources.add(BlockPos.asLong(chunk.getPos().getMinBlockX()+x,baseY+y,chunk.getPos().getMinBlockZ()+z));
        }
    }
    public static void tick(ServerLevel level) {
        if(level.getGameTime()%10!=0 || !EternalSnowState.isOverworldLike(level)) return;
        ServerSnowSettings settings=EternalSnowState.getServerSettings();
        if(!settings.enabled) return;
        Workspace w=ws(level); var data=CellThermalSavedData.get(level);
        int pending=w.pending.size();
        for(int i=0;i<pending;i++) {
            Long key=w.pending.poll(); if(key==null) break;
            LevelChunk chunk=level.getChunkSource().getChunkNow(ChunkPos.getX(key),ChunkPos.getZ(key));
            if(chunk!=null) admit(level,w,chunk.getPos().getWorldPosition());
            else w.pending.add(key);
        }
        scanSources(level,w);
        // Player volumes start a local field even in houses without burning sources.
        if(level.getGameTime()%40==0)
            for(var player:level.players()) seed(level,w,data,player.blockPosition(),4);
        var src=w.sources.iterator();
        while(src.hasNext()) {
            BlockPos pos=BlockPos.of(src.nextLong());
            if(!ready(level,pos)) { src.remove(); continue; }
            BlockState state=level.getBlockState(pos);
            if(!ThermalEngine.isPotentialHeatEmitter(state)) { src.remove(); continue; }
            if(ThermalEngine.getHeatSourcePowerC(level,pos,state,settings)>0)
                seed(level,w,data,pos,2);
        }
        // Grow only the frontier that has a real temperature contrast. Retained interior
        // temperatures are never rebased on weather, even after a source stops burning.
        int added=0, n=w.positions.length;
        for(int visited=0;visited<n && added<EXPANSION_BUDGET;visited++) {
            int i=(int)(w.expansionCursor++ % n); BlockPos p=w.positions[i];
            if(!ready(level,p)) continue;
            double t=data.chunk(chunkKey(p)).get(p.asLong());
            for(Direction d:ALL) {
                BlockPos q=p.relative(d);
                if(!ready(level,q)) continue;
                var map=data.chunk(chunkKey(q));
                if(!map.containsKey(q.asLong()) && Math.abs(t-ThermalEngine.computeBaseAmbientC(level,q))>.25) {
                    admit(level,w,q); ensure(level,w,data,q); added++;
                }
            }
        }
        int hash=Arrays.hashCode(settings.blockCapacity)*31+Arrays.hashCode(settings.blockConductivity);
        hash=31*hash+Float.floatToIntBits(settings.globalCapacityMultiplier);
        hash=31*hash+Float.floatToIntBits(settings.globalConductivityMultiplier);
        hash=31*hash+(settings.enableConvection?1:0);
        hash=31*hash+settings.convectionStrengthPercent;
        if(hash!=w.settingsHash) { w.settingsHash=hash; w.materials.clear(); w.dirty=true; }
        // Catch piston/explosion/state changes, not only player place/break events.
        for(int i=0;i<w.positions.length;i++) {
            BlockPos p=w.positions[i];
            if(!ready(level,p)) { w.dirty=true; continue; }
            if(level.getBlockState(p)!=w.states[i]) w.changed.add(p.asLong());
        }
        if(w.dirty) rebuild(level,w,data,settings);
        else updateChanged(level,w,data,settings);
        if(w.solver==null) return;
        Arrays.fill(w.powers,0);
        // Split existing flame power between its support and directly touching air.
        // A campfire was previously treated entirely as a massive stone voxel.
        for(int i:w.emitters) {
            BlockPos p=w.positions[i];
            if(!ready(level,p))continue;
            BlockState state=w.states[i];
            double power=ThermalEngine.getHeatSourcePowerC(level,p,state,settings)
                    * settings.roomHeatingPowerPercent/100.0;
            power*=Math.max(0,1-(w.solver.temperature(i)-20)/480.0);
            if(power<=0)continue;
            boolean flame=state.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock
                    || state.is(net.minecraft.world.level.block.Blocks.FIRE)
                    || state.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE);
            int neighbours=0;
            if(flame)for(Direction direction:ALL) {
                int j=w.indices.get(p.relative(direction).asLong());
                if(j>=0 && w.air[j] && ready(level,w.positions[j]))neighbours++;
            }
            double share=AirHeatTransfer.airShare(flame,neighbours);
            w.powers[i]+=power*(1-share);
            if(share>0)for(Direction direction:ALL) {
                int j=w.indices.get(p.relative(direction).asLong());
                if(j>=0 && w.air[j] && ready(level,w.positions[j]))w.powers[j]+=power*share/neighbours;
            }
        }
        for(int i=0;i<w.positions.length;i++) {
            BlockPos p=w.positions[i]; BlockState state=w.states[i];
            boolean loaded=ready(level,p);
            w.solver.setLoaded(i,loaded);
            if(!loaded) continue;
            w.solver.setPower(i,w.powers[i]);
            boolean air=ThermalEngine.isAirOrOpenPassage(state);
            // Outdoor air sees the weather. Solid top surfaces exchange through their material;
            // interior air/walls receive NO ambient relaxation term.
            boolean exposed=air?level.canSeeSky(p):p.getY()>=level.getHeight(Heightmap.Types.MOTION_BLOCKING,p.getX(),p.getZ())-1;
            double g=exposed?(air?1.0:Math.min(1.0,w.conductivity[i])):0;
            w.solver.setOutside(i,g,ThermalEngine.computeBaseAmbientC(level,p));
        }
        w.solver.step(.5); // loaded simulation time only; NEVER gameTime minus last loaded time
        for(int i=0;i<w.positions.length;i++) {
            BlockPos p=w.positions[i];
            if(ready(level,p)) data.chunk(chunkKey(p)).put(p.asLong(),w.solver.temperature(i));
        }
        data.setDirty();
        if(settings.meltRoofSnowFromWarmBlocks && level.getGameTime()%80==0) {
            // At most 32 checks per pass; no per-frame effects or full second world scan.
            for(int j=0;j<Math.min(32,w.positions.length);j++) {
                int i=level.random.nextInt(w.positions.length);
                if(w.solver.temperature(i)>2 && ready(level,w.positions[i]))
                    ThermalEngine.processWarmSurface(level,w.positions[i]);
            }
        }
    }
    private record Material(boolean air,double capacity,double conductivity) {}
    private static Material material(Workspace w,BlockState state,ServerSnowSettings settings) {
        return w.materials.computeIfAbsent(state,key->{
            boolean air=ThermalEngine.isAirOrOpenPassage(key);
            if(air)return new Material(true,1,1); // no tag classification for air
            var category=BlockThermalCategory.classify(key);
            double capacity=Math.max(.25,settings.blockCapacity[category.ordinal()]*settings.globalCapacityMultiplier*20);
            if(key.getBlock() instanceof HeatAccumulatorBlock)capacity*=4;
            return new Material(false,capacity,Math.max(.001,settings.blockConductivity[category.ordinal()]*settings.globalConductivityMultiplier));
        });
    }
    private static void updateChanged(ServerLevel level,Workspace w,CellThermalSavedData data,ServerSnowSettings settings) {
        for(long chunk:w.imported) {
            var cells=data.chunks.get(chunk);
            if(cells!=null)w.changed.addAll(cells.keySet());
        }
        w.imported.clear();
        if(w.changed.isEmpty())return;
        long[] changes=w.changed.toLongArray();
        int oldSize=w.positions.length;
        List<BlockPos> added=new ArrayList<>();
        for(long key:changes) {
            BlockPos pos=BlockPos.of(key);
            if(!ready(level,pos)||w.indices.containsKey(key))continue;
            int index=oldSize+added.size();w.indices.put(key,index);added.add(pos);
        }
        int size=oldSize+added.size();
        if(size!=oldSize) {
            w.positions=Arrays.copyOf(w.positions,size);w.states=Arrays.copyOf(w.states,size);
            w.conductivity=Arrays.copyOf(w.conductivity,size);w.air=Arrays.copyOf(w.air,size);
            w.powers=Arrays.copyOf(w.powers,size);
            for(int a=0;a<added.size();a++)w.positions[oldSize+a]=added.get(a);
        }
        // Append in index order before changing connections. Saved temperatures remain authoritative.
        for(int i=oldSize;i<size;i++) {
            BlockPos pos=w.positions[i];
            var mat=material(w,level.getBlockState(pos),settings);
            w.solver.addCell(data.chunk(chunkKey(pos)).get(pos.asLong()),mat.capacity);
        }
        for(long key:changes) {
            int i=w.indices.get(key);if(i<0)continue;
            BlockPos pos=w.positions[i];if(!ready(level,pos))continue;
            BlockState state=level.getBlockState(pos),previous=w.states[i];
            var map=data.chunk(chunkKey(pos));
            // Exact block-change hook updates saved local-air T. Fallback catches missed replacements.
            if(previous!=null && previous.getBlock()!=state.getBlock() && w.lastStates.get(key)!=state)
                map.put(key,localAir(level,data,pos));
            var mat=material(w,state,settings);
            if(i<oldSize && (w.replaced.contains(key)||previous==null||previous.getBlock()!=state.getBlock()))
                w.solver.replace(i,map.get(key),mat.capacity);
            else w.solver.setCapacity(i,mat.capacity);
            w.states[i]=state;w.lastStates.put(key,state);
            w.air[i]=mat.air;w.conductivity[i]=mat.conductivity;
            boolean emitter=ThermalEngine.isPotentialHeatEmitter(state);
            if(emitter)w.emitters.add(i);
            else w.emitters.remove(i);
        }
        // Only six contacts per modified cell. Unchanged contacts and arrays survive.
        for(long key:changes) {
            int i=w.indices.get(key);if(i<0||!ready(level,w.positions[i]))continue;
            for(Direction d:ALL) {
                int j=w.indices.get(w.positions[i].relative(d).asLong());if(j<0)continue;
                w.solver.setConnection(i,j,AirHeatTransfer.conductance(w.conductivity[i],w.conductivity[j],
                        w.air[i],w.air[j],settings.enableConvection,settings.convectionStrengthPercent));
            }
        }
        w.changed.clear();w.replaced.clear();
    }
    private static void rebuild(ServerLevel level,Workspace w,CellThermalSavedData data,ServerSnowSettings settings) {
        List<BlockPos> cells=new ArrayList<>();
        for(long chunk:w.loaded.toLongArray()) {
            if(level.getChunkSource().getChunkNow(ChunkPos.getX(chunk),ChunkPos.getZ(chunk))==null) continue;
            var map=data.chunks.get(chunk);
            if(map!=null) for(long key:map.keySet()) cells.add(BlockPos.of(key));
        }
        int n=cells.size();
        w.positions=cells.toArray(BlockPos[]::new); w.states=new BlockState[n]; w.conductivity=new double[n];
        w.powers=new double[n]; w.air=new boolean[n]; w.emitters.clear();
        double[] t=new double[n],cap=new double[n];
        w.indices=new Long2IntOpenHashMap(n);w.indices.defaultReturnValue(-1);
        for(int i=0;i<n;i++) {
            BlockPos p=w.positions[i];BlockState state=level.getBlockState(p);w.states[i]=state;
            if(ThermalEngine.isPotentialHeatEmitter(state))w.emitters.add(i);
            var map=data.chunk(chunkKey(p));
            BlockState previous=w.lastStates.put(p.asLong(),state);
            if(previous!=null && previous.getBlock()!=state.getBlock()) map.put(p.asLong(),localAir(level,data,p));
            t[i]=map.get(p.asLong());
            Material material=material(w,state,settings);
            w.air[i]=material.air;cap[i]=material.capacity;w.conductivity[i]=material.conductivity;
            w.indices.put(p.asLong(),i);
        }
        List<CellHeatSolver.Edge> edges=new ArrayList<>(n*3);
        for(int i=0;i<n;i++) for(Direction d:FORWARD) {
            int j=w.indices.get(w.positions[i].relative(d).asLong());if(j<0) continue;
            // Two half-cell resistances in series. One unordered edge per shared face.
            double a=w.conductivity[i],b=w.conductivity[j];
            edges.add(new CellHeatSolver.Edge(i,j,AirHeatTransfer.conductance(a,b,w.air[i],w.air[j],
                    settings.enableConvection,settings.convectionStrengthPercent)));
        }
        w.solver=new CellHeatSolver(t,cap,edges.toArray(CellHeatSolver.Edge[]::new));w.dirty=false;
        w.changed.clear();w.replaced.clear();w.imported.clear();
    }
}
