package com.eternalsnow.thermal.cells;

import net.minecraft.core.BlockPos;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.*;

/** Symmetric finite-volume contacts across coarse/fine interfaces. */
public final class AdaptiveThermalGraph {
    final List<AdaptiveThermalTile> tiles;
    public final CellHeatSolver solver;
    private final double[] power;
    public AdaptiveThermalGraph(Collection<? extends AdaptiveThermalTile> input) {
        tiles=new ArrayList<>(input);
        // Stable spatial ordering prevents hash iteration from changing splitting order on rebuild.
        tiles.sort(Comparator.comparingInt((AdaptiveThermalTile t)->t.x).thenComparingInt(t->t.y).thenComparingInt(t->t.z));
        var map=new Long2ObjectOpenHashMap<AdaptiveThermalTile>();int n=0;
        for(var t:tiles){t.node=n;n+=t.nodes();map.put(BlockPos.asLong(t.x,t.y,t.z),t);}
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
                var other=map.get(BlockPos.asLong(t.x+(axis==0?8:0),t.y+(axis==1?8:0),t.z+(axis==2?8:0)));
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
        double ka=a.detailed?a.conductivities[i]:a.conductivity,kb=b.detailed?b.conductivities[j]:b.conductivity;
        double conductance=AdaptiveThermalTile.contact(ka,kb,da,db,1);
        if(a.detailed&&b.detailed) {
            conductance=AdaptiveThermalTile.contact(a.rawConductivities[i],b.rawConductivities[j],.5,.5,1);
            if(a.air!=null&&b.air!=null&&a.air[i]&&b.air[j])conductance*=Math.min(a.mixing,b.mixing);
        }
        edges.add(new CellHeatSolver.Edge(a.nodeAt(i),b.nodeAt(j),conductance));
    }
    public void clearPower(){Arrays.fill(power,0);}
    public void addPower(AdaptiveThermalTile tile,int local,double value){power[tile.nodeAt(local)]+=value;}
    public void step(double seconds,double ambient) {
        for(int i=0;i<power.length;i++)solver.setPower(i,power[i]);
        for(var t:tiles)t.configureBoundary(solver,ambient);
        solver.stepConservative(seconds);
        for(var t:tiles)t.capture(solver,seconds);
    }
    public int fineCells(){int n=0;for(var t:tiles)if(t.detailed)n+=512;return n;}
}
