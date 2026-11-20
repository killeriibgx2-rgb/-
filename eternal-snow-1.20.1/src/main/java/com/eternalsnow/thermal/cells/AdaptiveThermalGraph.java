package com.eternalsnow.thermal.cells;

import net.minecraft.core.BlockPos;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.*;

/** Symmetric finite-volume contacts across coarse/fine interfaces. */
public final class AdaptiveThermalGraph {
    final List<AdaptiveThermalTile> tiles;
    public final CellHeatSolver solver;
    private final double[] power;
    /** Air mixing above plain face contact; a closed room mixes in seconds. 1 = old behaviour. */
    private double roomAirMixing=1;
    /** Equalisation sweeps per step; 0 disables forced mixing. */
    private int mixingSweeps=0;
    /**
     * Highest air temperature a source can hold, in C. Disabled by default: with a physical air
     * heat capacity the room finds its own equilibrium, and a flame cap only got in the way.
     * Kept as a safety valve a server can enable from the config.
     */
    private float sourceAirCapC=0;
    private final it.unimi.dsi.fastutil.ints.IntArrayList sourceNodes=new it.unimi.dsi.fastutil.ints.IntArrayList();
    private final Long2ObjectOpenHashMap<AdaptiveThermalTile> byPosition=new Long2ObjectOpenHashMap<>();
    public AdaptiveThermalGraph(Collection<? extends AdaptiveThermalTile> input) {
        tiles=new ArrayList<>(input);
        // Stable spatial ordering prevents hash iteration from changing splitting order on rebuild.
        tiles.sort(Comparator.comparingInt((AdaptiveThermalTile t)->t.x).thenComparingInt(t->t.y).thenComparingInt(t->t.z));
        int n=0;
        for(var t:tiles){t.node=n;n+=t.nodes();byPosition.put(BlockPos.asLong(t.x,t.y,t.z),t);}
        double[] temperature=new double[n],capacity=new double[n];
        var edges=new ArrayList<CellHeatSolver.Edge>();
        for(var t:tiles) {
            if(t.detailed) {
                for(int i=0;i<512;i++){temperature[t.node+i]=t.temperatures.get(i);capacity[t.node+i]=t.capacities[i];}
                for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++) {
                    int a=AdaptiveThermalTile.index(x,y,z);
                    if(x<7)add(edges,t,a,t,a+1,.5,.5);
                    if(y<7)add(edges,t,a,t,a+64,.5,.5);
                    if(z<7)add(edges,t,a,t,a+8,.5,.5);
                }
            } else {temperature[t.node]=t.mean();capacity[t.node]=t.totalCapacity;}
            for(int axis=0;axis<3;axis++) {
                var other=byPosition.get(BlockPos.asLong(t.x+(axis==0?8:0),t.y+(axis==1?8:0),t.z+(axis==2?8:0)));
                if(other==null)continue;
                if(!t.detailed&&!other.detailed) {
                    edges.add(new CellHeatSolver.Edge(t.node,other.node,AdaptiveThermalTile.contact(t.conductivity,other.conductivity,4,4,64)));
                } else for(int a=0;a<8;a++)for(int b=0;b<8;b++) {
                    int i=axis==0?AdaptiveThermalTile.index(7,a,b):axis==1?AdaptiveThermalTile.index(a,7,b):AdaptiveThermalTile.index(a,b,7);
                    int j=axis==0?AdaptiveThermalTile.index(0,a,b):axis==1?AdaptiveThermalTile.index(a,0,b):AdaptiveThermalTile.index(a,b,0);
                    add(edges,t,i,other,j,t.detailed?.5:4,other.detailed?.5:4);
                }
            }
        }
        solver=new CellHeatSolver(temperature,capacity,edges.toArray(CellHeatSolver.Edge[]::new));power=new double[n];
    }
    private static void add(List<CellHeatSolver.Edge> edges,AdaptiveThermalTile a,int i,AdaptiveThermalTile b,int j,double da,double db) {
        edges.add(new CellHeatSolver.Edge(a.nodeAt(i),b.nodeAt(j),conductance(a,i,b,j,da,db)));
    }
    /**
     * True when the same tile exists at this position with the same resolution,
     * so new materials can be applied to the live solver without reallocating it.
     * New tiles or detail changes still require a full rebuild.
     */
    public boolean canRefreshInPlace(AdaptiveThermalTile fresh) {
        AdaptiveThermalTile old=byPosition.get(BlockPos.asLong(fresh.x,fresh.y,fresh.z));
        return old!=null && old.detailed==fresh.detailed;
    }
    /** Apply edited materials/contacts to the existing solver; temperatures and power survive. */
    public void refreshInPlace(AdaptiveThermalTile fresh) {
        long position=BlockPos.asLong(fresh.x,fresh.y,fresh.z);
        AdaptiveThermalTile old=byPosition.get(position);
        if(old==null||old.detailed!=fresh.detailed)throw new IllegalStateException("Resolution change needs rebuild");
        int index=-1;
        for(int i=0;i<tiles.size();i++)if(tiles.get(i)==old){index=i;break;}
        if(index<0)throw new IllegalStateException("Tile missing from graph");
        fresh.node=old.node;
        tiles.set(index,fresh);byPosition.put(position,fresh);
        if(fresh.detailed) {
            for(int i=0;i<512;i++)solver.setCapacity(fresh.node+i,fresh.capacities[i]);
            for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++) {
                int a=AdaptiveThermalTile.index(x,y,z);
                if(x<7)connect(fresh,a,fresh,a+1,.5,.5);
                if(y<7)connect(fresh,a,fresh,a+64,.5,.5);
                if(z<7)connect(fresh,a,fresh,a+8,.5,.5);
            }
        } else solver.setCapacity(fresh.node,fresh.totalCapacity);
        for(int axis=0;axis<3;axis++) {
            var other=byPosition.get(BlockPos.asLong(fresh.x+(axis==0?8:0),fresh.y+(axis==1?8:0),fresh.z+(axis==2?8:0)));
            if(other==null)continue;
            if(!fresh.detailed&&!other.detailed) {
                solver.setConnection(fresh.node,other.node,
                        AdaptiveThermalTile.contact(fresh.conductivity,other.conductivity,4,4,64));
            } else for(int a=0;a<8;a++)for(int b=0;b<8;b++) {
                int i=axis==0?AdaptiveThermalTile.index(7,a,b):axis==1?AdaptiveThermalTile.index(a,7,b):AdaptiveThermalTile.index(a,b,7);
                int j=axis==0?AdaptiveThermalTile.index(0,a,b):axis==1?AdaptiveThermalTile.index(a,0,b):AdaptiveThermalTile.index(a,b,0);
                connect(fresh,i,other,j,fresh.detailed?.5:4,other.detailed?.5:4);
            }
        }
    }
    private void connect(AdaptiveThermalTile a,int i,AdaptiveThermalTile b,int j,double da,double db) {
        solver.setConnection(a.nodeAt(i),b.nodeAt(j),conductance(a,i,b,j,da,db));
    }
    /**
     * Conductance between two adjacent cells.
     *
     * <p>For two different materials in series: G = 1 / (dA/kA + dB/kB).
     * For the SAME material the resistance is dA/kA + dB/kB = (dA+dB)/k, so G = k/(dA+dB) —
     * not 2kA*kB/(kA+kB). Applying the mixed formula to identical cells silently halves air-air
     * transfer and seals pockets of air off from each other, which is one reason a room full of
     * air behaved like an insulator.
     */
    private static double conductance(AdaptiveThermalTile a,int i,AdaptiveThermalTile b,int j,double da,double db) {
        double ka=a.detailed?a.conductivities[i]:a.conductivity;
        double kb=b.detailed?b.conductivities[j]:b.conductivity;
        if(a.detailed&&b.detailed) {
            ka=a.rawConductivities[i];kb=b.rawConductivities[j];
            double value=ka==kb?ka/(da+db):AdaptiveThermalTile.contact(ka,kb,da,db,1);
            if(a.air!=null&&b.air!=null&&a.air[i]&&b.air[j])value*=Math.min(a.mixing,b.mixing);
            return value;
        }
        return ka==kb?ka/(da+db):AdaptiveThermalTile.contact(ka,kb,da,db,1);
    }
    /**
     * Room-scale mixing: long links between air cells of one tile that are not face neighbours.
     * Without them the only path out of a fire is its six faces, so heat crawls out and a large
     * room never warms. With them the room's air is close to isothermal, like real air.
     */
    /**
     * Uniform air mixing: every air cell next to another air cell gets the same conductance,
     * whether or not the two cells happen to share a tile. Tiles are our bookkeeping, not
     * physics — the previous version mixed air strongly inside a tile and barely across tile
     * borders, which would show up as 8-block temperature steps in a large hall.
     *
     * <p>No room detection, no flood fill: an open door simply lets heat flow out through the
     * same law. Air becomes nearly isothermal, which is what a real closed room does.
     */
    /** @param sweeps equalisation passes per step; 0 keeps only the plain conductance. */
    public void setMixingSweeps(int sweeps) { mixingSweeps=Math.max(0,Math.min(64,sweeps)); }

    public void setRoomAirMixing(double value) {
        roomAirMixing=Math.max(1,Math.min(200,value));
        if(roomAirMixing<=1)return;
        boolean[] airNode=airNodeTable();
        // Every air-air face pair gets exactly the same conductance, including pairs that cross
        // a tile border. Tiles are our bookkeeping, not physics: adding extra "long" links inside
        // a tile made border cells poorer than interior ones and produced a 2.5 C step at every
        // 8-block boundary. Uniform links remove that asymmetry.
        for(int e=0;e<solver.edgeCount();e++) {
            int a=solver.edgeA(e),b=solver.edgeB(e);
            if(airNode[a]&&airNode[b])solver.setConnection(a,b,roomAirMixing);
        }
    }
    private boolean[] airNodeTable() {
        boolean[] airNode=new boolean[solver.size()];
        for(var t:tiles)for(int i=0;i<t.nodes();i++)airNode[t.node+i]=!t.detailed||isAir(t,i);
        return airNode;
    }
    private static boolean isAir(AdaptiveThermalTile tile,int local) {
        return tile.air!=null && tile.air[local];
    }
    public void setSourceAirCap(float celsius){sourceAirCapC=celsius;}
    public void clearPower(){
        Arrays.fill(power,0);sourceNodes.clear();
    }
    /**
     * A fire heats the air around it, not one gram of air. Pouring a room's worth of power into
     * a single cell made that cell hundreds of degrees while the room stayed cold — physically
     * meaningless and useless in play. The power is spread evenly over the air cells of the
     * source's own tile, which is the volume the fire actually sits in.
     */
    public void addPower(AdaptiveThermalTile tile,int local,double value){
        if(tile.detailed&&tile.air!=null) {
            // Spread only over the air the fire can actually see, i.e. air with no direct path to
            // the sky. Spreading over every air cell of the tile divided the fire's heat with the
            // outdoor air standing in the same tile: measured, a campfire of 1500 W lifted a
            // 5x5x3 plank room by only 3.7 C because most of its heat went into the field outside.
            int enclosed=0;
            for(int i=0;i<512;i++)
                if(tile.air[i]&&tile.outsideG!=null&&tile.outsideG[i]<=0)enclosed++;
            if(enclosed>0) {
                double share=value/enclosed;
                for(int i=0;i<512;i++)
                    if(tile.air[i]&&tile.outsideG[i]<=0)power[tile.node+i]+=share;
                sourceNodes.add(tile.node+local);
                return;
            }
        }
        int node=tile.nodeAt(local);
        power[node]+=value;
        sourceNodes.add(node);
    }

    /**
     * A fire is a surface at flame temperature, not an unlimited watt tap. Added as a thermal
     * boundary it limits itself: in a cold room it pours heat in, and as the room nears the
     * flame temperature the flux falls to zero. Injecting fixed watts instead either overheats
     * a small room without bound or, once clamped, throttles the fire to a trickle.
     */
    private void applySourceBoundary(double ambient) {
        // A non-positive or effectively infinite cap means "plain watts", used by tests and by
        // anyone who turns the flame cap off in the config.
        if(sourceNodes.isEmpty()||!Float.isFinite(sourceAirCapC)||sourceAirCapC<=0||sourceAirCapC>=10000)return;
        double span=Math.max(1,sourceAirCapC-ambient);
        for(int i=0;i<sourceNodes.size();i++) {
            int node=sourceNodes.getInt(i);
            double flameConductance=power[node]/span;
            solver.setPower(node,0);
            if(flameConductance<=0)continue;
            double existing=solver.outsideConductance(node);
            double total=existing+flameConductance;
            double outside=(existing*solver.outsideTemperature(node)+flameConductance*sourceAirCapC)/total;
            solver.setOutside(node,total,outside);
        }
    }
    public void step(double seconds,double ambient) {
        for(int i=0;i<power.length;i++)solver.setPower(i,power[i]);
        for(var t:tiles)t.configureBoundary(solver,ambient);
        applySourceBoundary(ambient);
        for(var t:tiles)t.anchor(solver,ambient,seconds);
        solver.stepConservative(seconds);
        mixAir();
        for(var t:tiles)t.capture(solver,seconds);
    }
    /**
     * Spread air temperature across the room. Each sweep equalises every air-air pair, which
     * moves heat by one block; several sweeps make a whole hall behave as one volume.
     */
    private void mixAir() {
        if(mixingSweeps<=0)return;
        boolean[] airNode=airNodeTable();
        // Only enclosed air is forced to equalise. Open-sky air is moved by wind and convection
        // far faster than by conduction, and forced mixing there let a single torch warm a
        // twelve-block bubble of open air. A cell that sees the sky keeps its own boundary.
        for(int i=0;i<airNode.length;i++)
            if(airNode[i]&&solver.outsideConductance(i)>0)airNode[i]=false;
        for(int sweep=0;sweep<mixingSweeps;sweep++)solver.mixingSweep(airNode);
    }
    public int fineCells(){int n=0;for(var t:tiles)if(t.detailed)n+=512;return n;}
}
