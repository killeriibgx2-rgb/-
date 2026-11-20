package com.eternalsnow.thermal.engine;

/** Validation for absolute temperatures. */
public final class Temperature {
    private Temperature() {
    }

    /**
     * Physically meaningful absolute temperatures only. The floor is above 0 K
     * because a cell at absolute zero has no heat to give and breaks every
     * ratio in the model; climate extremes never approach it anyway.
     */
    public static double requireValid(double kelvin) {
        if (!Double.isFinite(kelvin)) {
            throw new IllegalArgumentException("temperature must be finite, got " + kelvin);
        }
        if (kelvin <= 0) {
            throw new IllegalArgumentException(
                    "temperature must be above absolute zero, got " + kelvin + " K");
        }
        return kelvin;
    }
}
