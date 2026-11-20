package com.eternalsnow.thermal.engine;

/**
 * Model time versus real time.
 *
 * <p>The multiplier does not shrink the step — it scales the fluxes. Multiplying
 * every conductance and every power by N is mathematically identical to letting
 * N times more time pass, and it costs nothing: the sub-stepping limit grows
 * with the capacities, so a bigger N does not mean more iterations
 * (docs/redesign-3.0-thermal-core.md §5.3).
 *
 * <p>Why it has to exist at all: at −200 °C a 1 m woollen shell needs 0.78 GJ
 * to come up to temperature, which is 43 hours of a 5 kW fire. At 1:1 that is
 * honest and unplayable. At ×60 it is 43 minutes. The default reflects that
 * measurement, not taste.
 */
public final class SimulationClock {
    /** Default for cryogenic worlds: 43 h of warm-up become 43 min. */
    public static final double CRYOGENIC_SPEED = 60.0;
    /** Default for an ordinary winter, where 1:1 is already comfortable. */
    public static final double NORMAL_SPEED = 1.0;
    /** Upper bound the UI may select; keeps numbers in a sane range. */
    public static final double MAX_SPEED = 1000.0;

    private double speedMultiplier;

    public SimulationClock(double speedMultiplier) {
        this.speedMultiplier = requireSpeed(speedMultiplier);
    }

    public static SimulationClock cryogenic() {
        return new SimulationClock(CRYOGENIC_SPEED);
    }

    public static SimulationClock normal() {
        return new SimulationClock(NORMAL_SPEED);
    }

    public double speedMultiplier() {
        return speedMultiplier;
    }

    /** Sets the multiplier; 0 pauses the physics while the world keeps ticking. */
    public void setSpeedMultiplier(double value) {
        this.speedMultiplier = requireSpeed(value);
    }

    /** Model seconds that elapse during {@code realSeconds} of wall time. */
    public double modelSeconds(double realSeconds) {
        if (!(realSeconds >= 0) || !Double.isFinite(realSeconds)) {
            throw new IllegalArgumentException("realSeconds must be finite and >= 0");
        }
        return realSeconds * speedMultiplier;
    }

    private static double requireSpeed(double value) {
        if (!Double.isFinite(value) || value < 0 || value > MAX_SPEED) {
            throw new IllegalArgumentException(
                    "speed multiplier must be within 0.." + MAX_SPEED + ", got " + value);
        }
        return value;
    }
}
