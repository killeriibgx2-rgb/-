package com.eternalsnow.thermal.world;

/** Small client-only-by-use mirror, kept free of Minecraft client dependencies for dedicated servers. */
public final class ClientBodyState {
    private static boolean enabled;
    private static double coreCelsius;
    private static double airCelsius;

    private ClientBodyState() {
    }

    public static void update(boolean isEnabled, double core, double air) {
        enabled = isEnabled && Double.isFinite(core) && Double.isFinite(air);
        coreCelsius = enabled ? core : 0.0;
        airCelsius = enabled ? air : 0.0;
    }

    public static void clear() {
        update(false, 0.0, 0.0);
    }

    public static boolean enabled() {
        return enabled;
    }

    public static double coreCelsius() {
        return coreCelsius;
    }

    public static double airCelsius() {
        return airCelsius;
    }
}
