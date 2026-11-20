package com.eternalsnow.thermal.cells;

import com.eternalsnow.block.HeatAccumulatorBlock;
import com.eternalsnow.block.HeatAccumulatorBlockEntity;
import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.thermal.BlockThermalCategory;
import com.eternalsnow.thermal.ThermalEngine;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Adaptive finite volumes: retained temperatures are NOT synonymous with solver nodes. */
public final class WorldCellThermal {
    private static final Map<ResourceKey<Level>,Workspace> WORLDS=new ConcurrentHashMap<>();
    private static final Direction[] DIRECTIONS=Direction.values();
    private static final int DETAIL_HOLD_TICKS=200;
    /**
     * Heat capacity of one air cell relative to solid materials.
     *
     * <p>Measured, not guessed. At 0.02 the time constant of one cell was 0.02 s, so the room sat
     * exactly at its quasi-static equilibrium at all times: the owner reported that a 5x5x3 room
     * dropped to outdoor temperature ten seconds after the campfire went out, and came back the
     * instant it was relit. At 0.5 the same room warms over minutes and keeps heat for half an
     * hour after the fire dies, which is what a room with a mass of wooden walls should do.
     */
    static final double AIR_HEAT_CAPACITY=110.0;
    /**
     * Everything in this model was reaching equilibrium in seconds, so a room behaved as if it
     * had no mass: a campfire out, outdoor temperature ten seconds later. Scaling every heat
     * capacity by the same factor slows every approach curve by that factor and changes no
     * steady state at all (equilibrium is P/G and does not contain capacity). Sixty turns the
     * second-scale response into the minute-scale one a player expects from a wooden house.
     */
    /**
     * Solids are not slowed down: a wall must be able to pass heat on and warm up. The measured
     * problem was the opposite — a 1 m thick wall with capacity 600 swallowed the whole room for
     * minutes, so the air cooled while the wall stayed at -7 C.
     */
    static final double THERMAL_TIME_SCALE=1;
    /** Air is a poor conductor: about twenty times worse than brick. See BlockThermalCategory. */
    static final double AIR_CONDUCTIVITY=0.02;
    /**
     * Open air is flushed by wind and convection, so it stays at the weather instead of storing
     * heat. A cell with a hole above it is not open air — it is the inside of a room that happens
     * to be vented — so it gets {@link #VENT_COUPLING} instead: enough to let the weather in over
     * minutes, not enough to empty the room in seconds.
     */
    static final double AIR_OPEN_COUPLING=6.0;
    /** Coupling of the air cell directly under a ceiling hole. */
    static final double VENT_COUPLING=0.08;
    private static final class Region {
        final AdaptiveThermalTile field;
        final IntArrayList sources=new IntArrayList();
        long detailedUntil;
        boolean powered;
        Region(AdaptiveThermalTile field){this.field=field;}
    }
    private record Material(double capacity,double conductivity,boolean air) {}
    private static final class Workspace {
        final Set<Long> pending=ConcurrentHashMap.newKeySet();
        final ConcurrentLinkedQueue<Long> pendingOrder=new ConcurrentLinkedQueue<>();
        final LongLinkedOpenHashSet scans=new LongLinkedOpenHashSet(), background=new LongLinkedOpenHashSet(), urgent=new LongLinkedOpenHashSet();
        final LongOpenHashSet loaded=new LongOpenHashSet(),dirty=new LongOpenHashSet(),wanted=new LongOpenHashSet();
        final Long2ObjectOpenHashMap<Region> regions=new Long2ObjectOpenHashMap<>();
        final IdentityHashMap<BlockState,Material> materials=new IdentityHashMap<>();
        AdaptiveThermalGraph graph;
        boolean rebuild=true;
        int settingsHash,initializedLast;
        long lastNanos,solverNanos,maxNanos,rebuilds,expansionCursor;
    }
    private static Workspace ws(ServerLevel level){return WORLDS.computeIfAbsent(level.dimension(),k->new Workspace());}
    public static void clearAll(){WORLDS.clear();}
    public static void forgetLevel(ServerLevel level){WORLDS.remove(level.dimension());}
    private static long chunkKey(BlockPos p){return ChunkPos.asLong(p.getX()>>4,p.getZ()>>4);}
    static long tileKey(BlockPos p){return BlockPos.asLong(p.getX()&~7,p.getY()&~7,p.getZ()&~7);}
    private static int local(BlockPos p){return AdaptiveThermalTile.index(p.getX()&7,p.getY()&7,p.getZ()&7);}
    private static BlockPos at(AdaptiveThermalTile t,int i){return new BlockPos(t.x+(i&7),t.y+(i>>6),t.z+((i>>3)&7));}
    private static boolean ready(ServerLevel l,BlockPos p){return !l.isOutsideBuildHeight(p)&&l.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null;}
    public static void load(ServerLevel level,long chunk){var w=ws(level);if(w.pending.add(chunk))w.pendingOrder.add(chunk);}
    public static void unload(ServerLevel level,long chunk) {
        Workspace w=WORLDS.get(level.dimension());if(w==null)return;
        w.pending.remove(chunk);w.pendingOrder.removeIf(k->k.longValue()==chunk);w.loaded.remove(chunk);w.scans.remove(chunk);
        w.background.removeIf(k->chunkKey(BlockPos.of(k))==chunk);w.urgent.removeIf(k->chunkKey(BlockPos.of(k))==chunk);
        w.dirty.removeIf(k->chunkKey(BlockPos.of(k))==chunk);w.wanted.removeIf(k->chunkKey(BlockPos.of(k))==chunk);
        if(w.regions.keySet().removeIf(k->chunkKey(BlockPos.of(k))==chunk))w.rebuild=true;
        // Each completed step has already captured temperatures into SavedData; no unload catch-up.
    }
    private static void request(ServerLevel level,Workspace w,long key,boolean urgent) {
        if(w.regions.containsKey(key)||!ready(level,BlockPos.of(key)))return;
        if(urgent){w.background.remove(key);w.urgent.add(key);}else if(!w.urgent.contains(key))w.background.add(key);
    }
    private static void halo(ServerLevel level,Workspace w,BlockPos p) {
        for(Direction d:DIRECTIONS)request(level,w,tileKey(p.relative(d,8)),false);
    }
    public static double temperature(ServerLevel level,BlockPos pos) {
        // Pure read: thermometers, crops, mobs and snow must not materialize the whole world.
        var data=CellThermalSavedData.get(level);var state=data.tiles.get(tileKey(pos));
        if(state!=null)return state.get(local(pos));
        var old=data.chunks.get(chunkKey(pos));double t=old==null?Double.NaN:old.get(pos.asLong());
        if(!Double.isNaN(t))return t;
        return ready(level,pos)?ThermalEngine.computeBaseAmbientC(level,pos):EternalSnowState.worldAmbientTempC;
    }
    private static double localAir(ServerLevel level,BlockPos pos) {
        double sum=0;int count=0;
        for(Direction d:DIRECTIONS) {
            BlockPos q=pos.relative(d);
            if(ready(level,q)&&ThermalEngine.isAirOrOpenPassage(level.getBlockState(q))) {sum+=temperature(level,q);count++;}
        }
        return count==0?ThermalEngine.computeBaseAmbientC(level,pos):sum/count;
    }
    public static void replaced(ServerLevel level,BlockPos pos) {
        if(!ready(level,pos)||!EternalSnowState.isOverworldLike(level))return;
        var data=CellThermalSavedData.get(level);long key=tileKey(pos);var state=data.tiles.get(key);
        boolean emitter=ThermalEngine.isPotentialHeatEmitter(level.getBlockState(pos));
        var old=data.chunks.get(chunkKey(pos));
        if(state==null && !emitter && (old==null||!old.containsKey(pos.asLong())))return;
        double t=localAir(level,pos);
        if(state!=null)state.set(local(pos),t);else data.chunk(chunkKey(pos)).put(pos.asLong(),t);
        data.setDirty();Workspace w=ws(level);w.dirty.add(key);request(level,w,key,true);
    }
    public static void blockChanged(ServerLevel level,BlockPos pos,BlockState before,BlockState after) {
        if(!EternalSnowState.isOverworldLike(level)||!ready(level,pos))return;
        Workspace w=WORLDS.get(level.dimension());
        boolean emitter=ThermalEngine.isPotentialHeatEmitter(after);
        if(w==null&&!emitter)return;
        if(w==null)w=ws(level);
        long key=tileKey(pos);
        if(before.getBlock()!=after.getBlock())replaced(level,pos);
        var settings=EternalSnowState.getServerSettings();
        boolean materialChange=!material(w,before,settings).equals(material(w,after,settings));
        boolean sourceChange=ThermalEngine.isPotentialHeatEmitter(before)!=emitter;
        if(w.regions.containsKey(key)&&(materialChange||sourceChange))w.dirty.add(key);
        // Do not rescan every vertical tile for fuel/lit changes or snow-layer count changes.
        BlockPos origin=BlockPos.of(key);
        boolean exposureChange=before.isAir()!=after.isAir()
                || before.getLightBlock(level,pos)!=after.getLightBlock(level,pos)
                || before.getFluidState().isEmpty()!=after.getFluidState().isEmpty();
        if(exposureChange)for(int y=level.getMinBuildHeight()&~7;y<level.getMaxBuildHeight();y+=8) {
            long affected=BlockPos.asLong(origin.getX(),y,origin.getZ());
            if(w.regions.containsKey(affected))w.dirty.add(affected);
        }
        if(emitter){request(level,w,key,true);halo(level,w,origin);}
    }
    public static void sourceChanged(ServerLevel level,BlockPos pos) {
        if(!ready(level,pos))return;Workspace w=ws(level);long key=tileKey(pos);
        request(level,w,key,true);
        Region region=w.regions.get(key);
        // Fuel/active changes are read every step; only membership needs a rescan.
        if(region!=null && region.sources.contains(local(pos))
                !=ThermalEngine.isPotentialHeatEmitter(level.getBlockState(pos)))w.dirty.add(key);
        halo(level,w,BlockPos.of(key));
    }
    /**
     * How strongly one block is tied to the weather, and at which temperature.
     *
     * <p>This is the single place where "heat has a way out" is decided, so it is pure and
     * strongly tested. An air cell with a free path to the sky follows the weather directly.
     * Solid blocks follow the weather only through their outer face.
     *
     * <p>Owner's report: a basement with eight torches reached +150 C while the outside air was
     * -2.2 C. The cause was here: an undisturbed solid block under the surface got no weather
     * coupling at all, so the ground around a cellar was a heat reservoir with no leak. It has
     * to be tied to the weather — quickly when it is shallow, slowly when it is deep.
     */
    static double weatherCoupling(boolean air,boolean sky,int y,int surfaceY,double conductivity) {
        if(air)return sky?AIR_OPEN_COUPLING:0;
        // A solid block is never a vent.
        // A wall block touches the weather on one face out of six, so its coupling must be a
        // fraction of its conductivity, not the conductivity itself. Giving planks 1.0 made a
        // small room a short circuit to the outside: measured, a 5x5x3 room fell from +13 C to
        // +3 C in ten seconds once the fire died.
        double surface=Math.max(0.02,Math.min(0.35,0.02*conductivity));
        if(sky||y>=surfaceY-1)return surface;
        // Buried solid is an effectively infinite reservoir at the ground temperature of its
        // depth: no player fire heats the rock around a cellar. Keeping it strongly coupled is
        // what stops a cellar from running away to +150 C with a handful of torches.
        int depth=Math.max(0,surfaceY-y);
        return Math.min(1,surface*(depth<=8?3.0:10.0));
    }

    /**
     * A hole vents the room: enclosed air with open air directly above or below it is connected
     * to the weather through that hole. Without this, breaking one roof block only chilled the
     * block under it and the rest of the room stayed warm forever, which was the report.
     */
    static void applyVents(boolean[] air,double[] g) {
        for(int y=1;y<7;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
            int i=AdaptiveThermalTile.index(x,y,z);
            if(!air[i]||g[i]>0)continue;
            if(g[AdaptiveThermalTile.index(x,y-1,z)]>0||g[AdaptiveThermalTile.index(x,y+1,z)]>0)
                g[i]=VENT_COUPLING;
        }
    }
    private static Material material(Workspace w,BlockState state,ServerSnowSettings settings) {
        return w.materials.computeIfAbsent(state,s->{
            boolean air=ThermalEngine.isAirOrOpenPassage(s);
            // Air holds very little heat. With capacity 1, the air of one 8x8x8 room needed
            // hundreds of thousands of energy units to warm, so even a huge fire took tens of
            // minutes to make a room habitable. This keeps the ratio between air and solid
            // materials close to reality while letting a normal fire warm a room in minutes.
            // Air conducts heat poorly; that is the whole point of a wall. With air at k = 1 it
            // conducted as well as brick, so a room leaked through its shell faster than any fire
            // could fill it. Convection is still applied on top as extra mixing.
            if(air)return new Material(AIR_HEAT_CAPACITY*THERMAL_TIME_SCALE,
                    AIR_CONDUCTIVITY*(settings.enableConvection?1+2*settings.convectionStrengthPercent/100.0:1),true);
            var category=BlockThermalCategory.classify(s);
            double c=Math.max(.25,settings.blockCapacity[category.ordinal()]*settings.globalCapacityMultiplier*20)*THERMAL_TIME_SCALE;
            if(s.getBlock() instanceof HeatAccumulatorBlock)c*=4;
            return new Material(c,Math.max(.001,settings.blockConductivity[category.ordinal()]*settings.globalConductivityMultiplier),false);
        });
    }
    private static Region initialize(ServerLevel level,Workspace w,CellThermalSavedData data,long key,boolean detail) {
        BlockPos origin=BlockPos.of(key);if(!ready(level,origin))return null;
        var chunk=level.getChunkSource().getChunkNow(origin.getX()>>4,origin.getZ()>>4);
        var state=data.tiles.get(key);boolean fresh=state==null;
        double[] temperatures=fresh?new double[512]:null;
        var old=data.chunks.get(chunkKey(origin));
        double[] c=new double[512],k=new double[512],g=new double[512],slope=new double[512],offset=new double[512];
        var sources=new IntArrayList();var settings=EternalSnowState.getServerSettings();
        double[] raw=new double[512];boolean[] air=new boolean[512];
        double mixing=settings.enableConvection?1+2*settings.convectionStrengthPercent/100.0:1;
        // MOTION_BLOCKING height is per column; one query per 8-block column, not per block.
        int[] columnHeight=new int[64];
        for(int column=0;column<64;column++)columnHeight[column]=level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                origin.getX()+(column&7),origin.getZ()+((column>>3)&7));
        for(int i=0;i<512;i++) {
            BlockPos p=origin.offset(i&7,i>>6,(i>>3)&7);BlockState block=chunk.getBlockState(p);
            Material m=material(w,block,settings);c[i]=m.capacity;k[i]=m.conductivity;raw[i]=m.air?1:m.conductivity;air[i]=m.air;
            // Skylight, not MOTION_BLOCKING: glass roofs must still see the sky.
            boolean sky=level.canSeeSky(p);
            int surfaceY=columnHeight[(i&7)|(((i>>3)&7)<<3)];
            g[i]=weatherCoupling(m.air,sky,p.getY(),surfaceY,m.conductivity);
            double blend=p.getY()<52&&!sky?Math.max(0,Math.min(.85,(surfaceY-p.getY()-8)/36.0)):0;
            slope[i]=1-blend;offset[i]=(p.getY()>85?-(p.getY()-85)*.12:0)*(1-blend)+6*blend;
            if(fresh) {
                double retained=old==null?Double.NaN:old.get(p.asLong());
                temperatures[i]=Double.isNaN(retained)?slope[i]*EternalSnowState.worldAmbientTempC+offset[i]:retained;
                if(Double.isNaN(retained)&&block.getBlock() instanceof HeatAccumulatorBlock&&level.getBlockEntity(p) instanceof HeatAccumulatorBlockEntity accumulator)
                    temperatures[i]+=Math.max(0,accumulator.getHeatC());
            }
            if(ThermalEngine.isPotentialHeatEmitter(block))sources.add(i);
        }
        applyVents(air,g);
        if(fresh) {
            state=data.migrateTile(key,temperatures);
        }
        Region previous=w.regions.get(key);
        Region region=new Region(new AdaptiveThermalTile(origin.getX(),origin.getY(),origin.getZ(),state));
        region.field.configure(detail,c,k,g,slope,offset);region.field.fineMaterials(raw,air,mixing);region.sources.addAll(sources);
        if(previous!=null&&previous.field.detailed!=detail) {
            region.field.switchShare=1;
            region.field.switchPending=false;
        }
        region.detailedUntil=previous==null?0:previous.detailedUntil;
        if(detail&&w.wanted.contains(key))region.detailedUntil=level.getGameTime()+DETAIL_HOLD_TICKS;
        w.regions.put(key,region);w.initializedLast++;
        // Material/contact edits update the live solver; only new tiles or detail changes rebuild it.
        if(w.graph!=null&&w.graph.canRefreshInPlace(region.field))w.graph.refreshInPlace(region.field);
        else w.rebuild=true;
        return region;
    }
    private static void admitChunks(ServerLevel level,Workspace w,CellThermalSavedData data) {
        int attempts=Math.min(64,w.pending.size());
        for(int i=0;i<attempts;i++) {
            Long key=w.pendingOrder.poll();if(key==null)break;if(!w.pending.contains(key))continue;
            if(level.getChunkSource().getChunkNow(ChunkPos.getX(key),ChunkPos.getZ(key))==null){w.pendingOrder.add(key);continue;}
            w.pending.remove(key);if(!w.loaded.add(key))continue;w.scans.add(key);
            var legacy=data.chunks.get(key);
            if(legacy!=null)for(long p:legacy.keySet())request(level,w,tileKey(BlockPos.of(p)),false);
            var retained=data.tileChunks.get(key);
            if(retained!=null)for(long p:retained)request(level,w,p,false);
        }
    }
    private static void scanSources(ServerLevel level,Workspace w) {
        if(w.scans.isEmpty())return;long key=w.scans.removeFirstLong();
        var chunk=level.getChunkSource().getChunkNow(ChunkPos.getX(key),ChunkPos.getZ(key));if(chunk==null)return;
        var sections=chunk.getSections();var sourceTiles=new LongOpenHashSet();
        for(int sectionIndex=0;sectionIndex<sections.length;sectionIndex++) {
            var section=sections[sectionIndex];
            if(section.hasOnlyAir()||!section.maybeHas(ThermalEngine::isPotentialHeatEmitter))continue;
            int y0=(chunk.getMinSection()+sectionIndex)*16;
            for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++)
                if(ThermalEngine.isPotentialHeatEmitter(section.getBlockState(x,y,z)))
                    sourceTiles.add(tileKey(new BlockPos(chunk.getPos().getMinBlockX()+x,y0+y,chunk.getPos().getMinBlockZ()+z)));
        }
        for(long tile:sourceTiles){request(level,w,tile,false);halo(level,w,BlockPos.of(tile));}
    }
    private static void selectDetail(ServerLevel level,Workspace w) {
        w.wanted.clear();
        for(var player:level.players()) {
            if(player.isSpectator())continue;BlockPos origin=BlockPos.of(tileKey(player.blockPosition()));
            for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++) {
                BlockPos p=origin.offset(x*8,y*8,z*8);if(!ready(level,p))continue;
                long key=p.asLong();w.wanted.add(key);request(level,w,key,true);halo(level,w,p);
            }
        }
        for(var entry:w.regions.long2ObjectEntrySet()) {
            var region=entry.getValue();boolean wanted=w.wanted.contains(entry.getLongKey());
            if(wanted)region.detailedUntil=level.getGameTime()+DETAIL_HOLD_TICKS;
            // A tile that contains an enclosure must never be collapsed into one uniform number:
            // a 5x5x3 room is a small part of an 8^3 tile, and a coarse tile is stored as a
            // single temperature, so the room would be replaced by the mean of the field around
            // it. That is why a campfire inside a small house read as "not warming at all".
            if(region.field.keepDetailed())region.detailedUntil=level.getGameTime()+DETAIL_HOLD_TICKS;
            if((wanted&&!region.field.detailed)||(!wanted&&region.field.detailed&&level.getGameTime()>=region.detailedUntil))
                w.dirty.add(entry.getLongKey());
        }
    }
    private static void initializeQueued(ServerLevel level,Workspace w,CellThermalSavedData data) {
        for(int i=0;i<64&&!w.urgent.isEmpty();i++) {
            long key=w.urgent.removeFirstLong();w.background.remove(key);
            if(!w.regions.containsKey(key))initialize(level,w,data,key,w.wanted.contains(key));
        }
        for(int i=0;i<16&&!w.background.isEmpty();i++) {
            long key=w.background.removeFirstLong();
            if(!w.regions.containsKey(key))initialize(level,w,data,key,w.wanted.contains(key));
        }
        // Exact block edits are processed before the next simulation step, never against stale materials.
        for(long key:w.dirty.toLongArray()) {
            Region region=w.regions.get(key);if(region==null)continue;
            boolean detail=w.wanted.contains(key)||(region.field.detailed&&level.getGameTime()<region.detailedUntil);
            if(w.wanted.contains(key))detail=true;
            initialize(level,w,data,key,detail);
        }
        w.dirty.clear();
    }
    private static void grow(ServerLevel level,Workspace w) {
        if(w.regions.isEmpty())return;
        // Sparse coarse extension, not hundreds of new individual voxels per half-second.
        var keys=w.regions.keySet().toLongArray();int added=0;
        for(int visit=0;visit<Math.min(32,keys.length)&&added<4;visit++) {
            long key=keys[(int)(w.expansionCursor++%keys.length)];Region r=w.regions.get(key);
            BlockPos p=BlockPos.of(key);double t=r.field.mean();
            double outside=ThermalEngine.computeBaseAmbientC(level,p.offset(4,4,4)),inward=t;
            for(Direction d:DIRECTIONS){Region q=w.regions.get(p.relative(d,8).asLong());if(q!=null)inward=t>outside?Math.max(inward,q.field.mean()):Math.min(inward,q.field.mean());}
            if(!ThermalFrontier.shouldExpand(t,outside,inward,r.powered))continue;
            for(Direction d:DIRECTIONS) {
                if(added>=4)break;long q=p.relative(d,8).asLong();
                if(!w.regions.containsKey(q)&&!w.background.contains(q)&&!w.urgent.contains(q)&&ready(level,BlockPos.of(q))){request(level,w,q,false);added++;}
            }
        }
    }
    public static void tick(ServerLevel level) {
        if(level.getGameTime()%10!=0||!EternalSnowState.isOverworldLike(level))return;
        var settings=EternalSnowState.getServerSettings();if(!settings.enabled)return;
        long start=System.nanoTime();Workspace w=ws(level);w.initializedLast=0;
        var data=CellThermalSavedData.get(level);
        var checkedChunks=new LongOpenHashSet();var missingChunks=new LongOpenHashSet();
        for(long key:w.regions.keySet()) {
            long ck=chunkKey(BlockPos.of(key));
            if(checkedChunks.add(ck)&&level.getChunkSource().getChunkNow(ChunkPos.getX(ck),ChunkPos.getZ(ck))==null)missingChunks.add(ck);
        }
        for(long ck:missingChunks)unload(level,ck);
        admitChunks(level,w,data);scanSources(level,w);selectDetail(level,w);
        int hash=Arrays.hashCode(settings.blockCapacity)*31+Arrays.hashCode(settings.blockConductivity);
        hash=31*hash+Float.floatToIntBits(settings.globalCapacityMultiplier);hash=31*hash+Float.floatToIntBits(settings.globalConductivityMultiplier);
        hash=31*hash+(settings.enableConvection?settings.convectionStrengthPercent:-1);
        hash=31*hash+Float.floatToIntBits(settings.roomAirMixing);
hash=31*hash+settings.roomMixingSweeps;
        if(hash!=w.settingsHash){w.settingsHash=hash;w.materials.clear();w.dirty.addAll(w.regions.keySet());}
        initializeQueued(level,w,data);grow(level,w);
        if(w.rebuild) {
            var fields=new ArrayList<AdaptiveThermalTile>();for(var region:w.regions.values())fields.add(region.field);
            w.graph=new AdaptiveThermalGraph(fields);w.rebuild=false;w.rebuilds++;
            w.graph.setRoomAirMixing(settings.roomAirMixing);
            w.graph.setMixingSweeps(settings.roomMixingSweeps);
        }
        if(w.graph==null)return;w.graph.clearPower();
        for(var region:w.regions.values()) {
            region.powered=false;AdaptiveThermalTile t=region.field;
            for(int s=0;s<region.sources.size();s++) {
                int i=region.sources.getInt(s);BlockPos p=at(t,i);if(!ready(level,p))continue;
                BlockState state=level.getBlockState(p);
                double power=ThermalEngine.getHeatSourcePowerC(level,p,state,settings)
                        *settings.roomHeatingPowerPercent/100.0*settings.roomSourcePower;
                power*=Math.max(0,1-(t.temperatures.get(i)-20)/480.0);
                if(power<=0)continue;region.powered=true;
                boolean flame=t.detailed&&(state.getBlock() instanceof CampfireBlock||state.is(Blocks.FIRE)||state.is(Blocks.SOUL_FIRE));
                int airNeighbours=0;
                if(flame)for(Direction d:DIRECTIONS) {
                    BlockPos q=p.relative(d);Region other=w.regions.get(tileKey(q));
                    if(other!=null&&ready(level,q)&&ThermalEngine.isAirOrOpenPassage(level.getBlockState(q)))airNeighbours++;
                }
                double share=AirHeatTransfer.airShare(flame,airNeighbours);w.graph.addPower(t,i,power*(1-share));
                if(share>0)for(Direction d:DIRECTIONS) {
                    BlockPos q=p.relative(d);Region other=w.regions.get(tileKey(q));
                    if(other!=null&&ready(level,q)&&ThermalEngine.isAirOrOpenPassage(level.getBlockState(q)))
                        w.graph.addPower(other.field,local(q),power*share/airNeighbours);
                }
            }
        }
        long solverStart=System.nanoTime();w.graph.step(.5,EternalSnowState.worldAmbientTempC);
        w.solverNanos=System.nanoTime()-solverStart;if(!w.regions.isEmpty())data.setDirty();
        w.lastNanos=System.nanoTime()-start;w.maxNanos=Math.max(w.maxNanos,w.lastNanos);
        if(settings.meltRoofSnowFromWarmBlocks&&level.getGameTime()%80==0) {
            int checked=0;
            for(var region:w.regions.values()) {
                if(!region.field.detailed||checked++>=32)continue;
                int i=level.random.nextInt(512);if(region.field.temperatures.get(i)>2)ThermalEngine.processWarmSurface(level,at(region.field,i));
            }
        }
    }
    public static String diagnostics(ServerLevel level) {
        Workspace w=WORLDS.get(level.dimension());if(w==null||w.graph==null)return "Thermal: no active graph in "+level.dimension().location();
        var data=CellThermalSavedData.get(level);long legacy=0;for(var map:data.chunks.values())legacy+=map.size();
        int sources=0;for(var region:w.regions.values())sources+=region.sources.size();
        int fine=w.graph.fineCells(),coarse=w.regions.size()-fine/512;
        return String.format(Locale.ROOT,"Thermal %s: nodes=%d fineCells=%d coarseTiles=%d retained=%d legacy=%d edges=%d substeps=%d initialized=%d total=%.2fms solver=%.2fms max=%.2fms rebuilds=%d sources=%d queued=%d",
                level.dimension().location(),w.graph.solver.size(),fine,coarse,(long)data.tiles.size()*512+legacy,legacy,w.graph.solver.edgeCount(),w.graph.solver.lastSubsteps(),w.initializedLast,
                w.lastNanos/1e6,w.solverNanos/1e6,w.maxNanos/1e6,w.rebuilds,sources,w.background.size()+w.urgent.size()+w.pending.size()+w.scans.size());
    }
}
