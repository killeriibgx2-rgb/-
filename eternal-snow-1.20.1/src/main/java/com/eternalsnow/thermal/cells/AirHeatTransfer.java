package com.eternalsnow.thermal.cells;

/** Local, conservative heat transfer. No radius aura, room reset or extra energy. */
public final class AirHeatTransfer {
    private AirHeatTransfer() {}
    public static double conductance(double a,double b,boolean airA,boolean airB,
                                     boolean convection,int strengthPercent) {
        double contact=2*a*b/(a+b);
        // Effective mixing of adjacent air volumes, NOT conduction through walls.
        // Isotropic mixing also serves corridors; this is not a fluid/buoyancy solver.
        if(airA && airB && convection)
            contact*=1+2*Math.max(0,Math.min(300,strengthPercent))/100.0;
        return contact;
    }
    public static double airShare(boolean openFlame,int neighbours) {
        return openFlame && neighbours>0 ? .8 : 0;
    }
}
