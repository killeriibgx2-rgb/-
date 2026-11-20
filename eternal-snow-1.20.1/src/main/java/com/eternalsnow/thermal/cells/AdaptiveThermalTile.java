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
    /** 1 on the one capture that changes the physical content of the tile. */
    double switchShare;
    boolean switchPending;
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
    /**
     * Promoting or demoting a tile changes what the tile physically holds. A detailed tile can
     * keep a room at +14 C inside a field at -5 C; a coarse tile is one uniform body. Doing that
     * for free would create or destroy energy on every walk-away, so the change is charged to the
     * common pool of the graph exactly once, through the boundary term.
     */
    void capture(CellHeatSolver solver,double seconds) {
        if(detailed) {
            temperatures.normalize();
            double energy=0;gBase=0;
            for(int i=0;i<512;i++){temperatures.base[i]=solver.temperature(node+i);energy+=capacities[i]*temperatures.base[i];gBase+=outsideG[i]*temperatures.base[i];}
            baseMean=energy/totalCapacity;temperatures.refreshExtrema();
        } else {
            // A coarse tile is one reservoir, so the solver holds a single temperature for it.
            // The 512-cell pattern is kept as it is. Without that, a player's room is erased the
            // moment the tile stops being detailed — every cell collapses onto the mean of the
            // field around the house, which is the weather. That is the reported "a campfire in a
            // 5x5x3 house does not warm anything".
            //
            // Keeping the pattern means the tile can hold more (or less) energy than the single
            // number the solver tracks, so the difference is charged to the weather term once:
            // it is neither invented nor silently lost.
            if(switchShare>0&&!switchPending) {
                temperatures.normalize();
                double energy=0,total=0;
                for(int i=0;i<512;i++){energy+=lastCapacities[i]*temperatures.base[i];total+=lastCapacities[i];}
                if(total>0) {
                    double patternMean=energy/total;
                    gOffset-=(patternMean-solver.temperature(node))*switchShare*totalCapacity;
                }
                switchPending=true;
            }
            double decay=Math.exp(-seconds*8*conductivity/totalCapacity);
            temperatures.coarseUpdate(solver.temperature(node),baseMean,decay);
        }
    }
}
