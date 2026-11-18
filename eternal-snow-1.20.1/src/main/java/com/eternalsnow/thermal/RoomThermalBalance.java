package com.eternalsnow.thermal;

/** Exact lumped C*dT/dt = P - U*(T - outside), with constant inputs over one step. */
public final class RoomThermalBalance {
    private RoomThermalBalance() {}

    public static double advance(double temperature, double outside, double equilibriumRise,
                                 double elapsedTicks, double tauTicks) {
        if (elapsedTicks <= 0) return temperature;
        double fraction = -Math.expm1(-elapsedTicks / Math.max(0.001, tauTicks));
        return temperature + (outside + equilibriumRise - temperature) * fraction;
    }
}
