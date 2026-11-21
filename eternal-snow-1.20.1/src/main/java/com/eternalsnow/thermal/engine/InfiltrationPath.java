package com.eternalsnow.thermal.engine;

/**
 * A leak into one grid cell: an aperture plus the outside air it connects to.
 *
 * <p>Height matters and is not decoration. Stack-driven flow grows with the
 * square root of the opening height, so a gap near the ceiling is worse than
 * the same gap at floor level. That is a real, predictable effect a player can
 * reason about, unlike a hand-tuned "ventilation coefficient".
 *
 * @param cellIndex      cell the leak enters
 * @param apertureAreaM2 total open area, m²
 * @param heightM        height of the opening, m
 * @param ambient        the outside air
 */
public record InfiltrationPath(int cellIndex, double apertureAreaM2, double heightM, Ambient ambient) {
    public InfiltrationPath {
        if (cellIndex < 0) throw new IllegalArgumentException("cellIndex must be >= 0");
        if (ambient == null) throw new NullPointerException("ambient");
        if (!(apertureAreaM2 >= 0) || !Double.isFinite(apertureAreaM2)) {
            throw new IllegalArgumentException("apertureAreaM2 must be finite and >= 0");
        }
        if (!(heightM > 0) || !Double.isFinite(heightM)) {
            throw new IllegalArgumentException("heightM must be finite and > 0");
        }
    }
}
