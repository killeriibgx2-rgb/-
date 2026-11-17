package com.eternalsnow.food;

/** Physics independent of Minecraft, so the scan cadence can be regression-tested. */
public final class FoodCooling {
    private FoodCooling() {}

    public static int advanceMilliC(int temperature, double environment, long ticks, double tau) {
        return (int) Math.round((environment + (temperature / 1000.0 - environment)
                * Math.exp(-Math.max(0L, ticks) / tau)) * 1000.0);
    }
}
