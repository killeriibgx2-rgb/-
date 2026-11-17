package com.eternalsnow.food;

/** Physics independent of Minecraft, so the scan cadence can be regression-tested. */
public final class FoodCooling {
    private FoodCooling() {}

    public static int advanceMilliC(int temperature, double environment, long ticks, double tau) {
        return (int) Math.round((environment + (temperature / 1000.0 - environment)
                * Math.exp(-Math.max(0L, ticks) / tau)) * 1000.0);
    }
    /** Fractional spoilage survives slow settings and long-life profiles. */
    public static int spoilageMicros(long elapsedTicks, int temperature, int ticksPerStep) {
        if (ticksPerStep <= 0 || elapsedTicks <= 0) return 0;
        float factor = Math.max(0.0F, Math.min(3.0F, temperature / 20.0F));
        long amount = elapsedTicks * 1000000L * Math.round(factor * 100.0F) / (ticksPerStep * 100L);
        return (int) Math.min(amount, Integer.MAX_VALUE / 2);
    }
}
