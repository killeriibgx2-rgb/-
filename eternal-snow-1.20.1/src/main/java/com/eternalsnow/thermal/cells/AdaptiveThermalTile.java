package com.eternalsnow.thermal.cells;

/** Coarse reservoir or 512 detailed cells, with identical total heat capacity. */
public final class AdaptiveThermalTile {
    /** Above this capacity a cell is not air; used only to recognise enclosed air. */
    static final double AIR_ENCLOSED_CAPACITY_LIMIT = 2000;
    public final int x,y,z;
    public final TileTemperatureState temperatures;
    public boolean detailed;
    public int node;
    double totalCapacity,conductivity,baseMean,gTotal,gSlope,gOffset,gBase;
    double[] capacities,conductivities,outsideG,slopes,offsets,rawConductivities;
    /** Any enclosed air cell (no direct sky path) means the tile has its own micro-climate. */
    boolean holdsEnclosure;
    /** Capacities of the last configured state, kept for the coarse energy bookkeeping. */
    double[] lastCapacities=new double[512];
    boolean[] air;
    double mixing=1;
    /** Per-cell ground anchor (0..1); only solid cells that are weakly coupled get it. */
    double[] anchor;
    void fineMaterials(double[] raw,boolean[] air,double mixing){this.rawConductivities=detailed?raw:null;this.air=detailed?air:null;this.mixing=mixing;}
    public AdaptiveThermalTile(int x,int y,int z,TileTemperatureState temperatures) {
        this.x=x;this.y=y;this.z=z;this.temperatures=temperatures;
    }
    public void configure(boolean detail,double[] c,double[] k,double[] g,double[] slope,double[] offset) {
        if(c.length!=512||k.length!=512||g.length!=512||slope.length!=512||offset.length!=512)
            throw new IllegalArgumentException("Tile material size");
        temperatures.normalize();
        double energy=0,resistance=0;totalCapacity=0;gTotal=0;gSlope=0;gOffset=0;gBase=0;
        for(int i=0;i<512;i++) {
            if(!Double.isFinite(c[i])||!Double.isFinite(k[i])||c[i]<=0||k[i]<=0
                    ||!Double.isFinite(g[i])||g[i]<0||!Double.isFinite(slope[i])||!Double.isFinite(offset[i]))
                throw new IllegalArgumentException("Invalid tile material");
            totalCapacity+=c[i];energy+=c[i]*temperatures.base[i];resistance+=1/k[i];
            gTotal+=g[i];gSlope+=g[i]*slope[i];gOffset+=g[i]*offset[i];gBase+=g[i]*temperatures.base[i];
        }
        baseMean=energy/totalCapacity;conductivity=512/resistance;detailed=detail;
        lastCapacities=c;
        capacities=detail?c:null;conductivities=detail?k:null;rawConductivities=detail?k:null;air=null;mixing=1;anchor=null;
        outsideG=detail?g:null;slopes=detail?slope:null;offsets=detail?offset:null;
        holdsEnclosure=false;
        for(int i=0;i<512;i++)if(g[i]<=0&&c[i]<=AIR_ENCLOSED_CAPACITY_LIMIT){holdsEnclosure=true;break;}
    }
    public double mean() {return baseMean*temperatures.scale+temperatures.offset;}
    public int nodes() {return detailed?512:1;}
    public int nodeAt(int local) {return node+(detailed?local:0);}
    public static int index(int x,int y,int z) {return x|(z<<3)|(y<<6);}
    public static double contact(double ka,double kb,double distanceA,double distanceB,double area) {
        return area/(distanceA/ka+distanceB/kb);
    }
    /**
     * Weather coupling is weak for deep ground. Rather than wait hours of real time for the
     * ground to reach its own steady state (meanwhile a cellar with torches runs away to +150 C),
     * every so often pull each solid cell a small part of the way to the temperature it would
     * have with no heat sources. Air is never anchored: a heated room must stay heated.
     */
    void anchor(CellHeatSolver solver,double ambient,double tickSeconds) {
        if(anchor==null)return;
        for(int i=0;i<512;i++) {
            double a=anchor[i];if(a<=0)continue;
            double relax=Math.min(1,tickSeconds/a);
            solver.anchorToAmbient(node+i,relax);
        }
    }
    /** True when this tile holds heated enclosures; such a tile cannot be one uniform number. */
    boolean keepDetailed() {
        return detailed && outsideG != null && air != null && holdsEnclosure;
    }

    void configureBoundary(CellHeatSolver solver,double ambient) {
        if(detailed) {
            for(int i=0;i<512;i++)solver.setOutside(node+i,outsideG[i],slopes[i]*ambient+offsets[i]);
        } else {
            // Standard finite-volume boundary on the reservoir mean; sub-tile detail is unresolved.
            solver.setOutside(node,gTotal,gTotal==0?0:(gSlope*ambient+gOffset)/gTotal);
        }
    }
    void capture(CellHeatSolver solver,double seconds) {
        if(detailed) {
            temperatures.normalize();
            double energy=0;gBase=0;
            for(int i=0;i<512;i++){temperatures.base[i]=solver.temperature(node+i);energy+=capacities[i]*temperatures.base[i];gBase+=outsideG[i]*temperatures.base[i];}
            baseMean=energy/totalCapacity;temperatures.refreshExtrema();
        } else {
            // A coarse tile is one reservoir, so the solver sees a single temperature. The full
            // 512-cell pattern is kept as it was: without that, a player's room is erased the
            // moment they walk away (the "hole in the ceiling shows outdoor temperature" report),
            // because every cell of the tile collapses onto the mean of the field around it.
            // Only the overall energy is taken from the solver.
            double energy=0,total=0;
            for(int i=0;i<512;i++){energy+=lastCapacities[i]*temperatures.get(i);total+=lastCapacities[i];}
            double mean=total>0?energy/total:solver.temperature(node);
            double target=solver.temperature(node);
            double shift=target-mean;
            if(Double.isFinite(shift)&&Math.abs(shift)>1e-9)
                for(int i=0;i<512;i++)temperatures.set(i,temperatures.get(i)+shift);
        }
    }
}
