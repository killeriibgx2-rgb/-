package com.eternalsnow.thermal.cells;

/** Domain expansion requires an inward spatial gradient, not a uniform weather offset. */
public final class ThermalFrontier {
    private ThermalFrontier() {}
    public static boolean shouldExpand(double temperature,double outside,double inward,boolean powered) {
        double contrast=temperature-outside;
        if(Math.abs(contrast)<=.25)return false;
        return powered || (contrast>0 ? inward-temperature>.25 : temperature-inward>.25);
    }
}
