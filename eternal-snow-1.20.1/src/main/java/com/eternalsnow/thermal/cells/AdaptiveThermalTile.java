package com.eternalsnow.thermal.cells;

/** Coarse reservoir or 512 detailed cells, with identical total heat capacity. */
public final class AdaptiveThermalTile {
    public final int x,y,z;
    public final TileTemperatureState temperatures;
    public boolean detailed;
    public int node;
    double totalCapacity,conductivity,baseMean,gTotal,gSlope,gOffset,gBase;
    double[] capacities,conductivities,outsideG,slopes,offsets,rawConductivities;
    boolean[] air;
    double mixing=1;
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
        capacities=detail?c:null;conductivities=detail?k:null;rawConductivities=detail?k:null;air=null;mixing=1;
        outsideG=detail?g:null;slopes=detail?slope:null;offsets=detail?offset:null;
    }
    public double mean() {return baseMean*temperatures.scale+temperatures.offset;}
    public int nodes() {return detailed?512:1;}
    public int nodeAt(int local) {return node+(detailed?local:0);}
    public static int index(int x,int y,int z) {return x|(z<<3)|(y<<6);}
    public static double contact(double ka,double kb,double distanceA,double distanceB,double area) {
        return area/(distanceA/ka+distanceB/kb);
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
            // Internal contrast relaxation preserves the reservoir's weighted mean/energy.
            double decay=Math.exp(-seconds*8*conductivity/totalCapacity);
            temperatures.coarseUpdate(solver.temperature(node),baseMean,decay);
        }
    }
}
