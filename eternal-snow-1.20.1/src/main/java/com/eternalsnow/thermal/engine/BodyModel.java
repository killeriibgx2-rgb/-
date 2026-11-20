package com.eternalsnow.thermal.engine;

/**
 * Human body as a lumped thermal mass with clothing resistance.
 *
 * <p>Level agreed with the user: body plus clothing layers, no metabolism
 * simulation, no sweat, no illness. Those are a later stage and must not be
 * faked here.
 *
 * <p>Physics:
 * <pre>
 *   G  = A_body / (R_clothing + R_skin)     W/K
 *   dT/dt = (P_metabolic + Q_environment) / C_body
 * </pre>
 * where {@code P_metabolic} is 100 W at rest (a real figure), clothing is in
 * clo (1 clo = 0.155 m²·K/W) and the skin film is about 0.1 m²·K/W.
 *
 * <p>Measured consequences (computed, see {@code BodyModelTest}): 100 W of
 * resting heat keeps a naked body 5.6 K above the air, so comfortable air is
 * about +31 °C; a winter coat (+2 clo) needs +14 °C; polar kit (+3 clo) needs
 * +5 °C. This is why clothing, not fire, is the first answer to cold.
 */
public final class BodyModel {
    /** Skin surface area of an adult, m². */
    public static final double BODY_AREA = 1.8;
    /** Thermal resistance of the skin film and still air layer, m²·K/W. */
    public static final double SKIN_RESISTANCE = 0.1;
    /** One clo in SI, m²·K/W. */
    public static final double CLO = 0.155;
    /** Body thermal capacity, J/K: 70 kg at 3.5 kJ/(kg·K). */
    public static final double BODY_CAPACITY = 70 * 3500.0;
    /** Resting metabolic heat production, W. */
    public static final double RESTING_WATTS = 100.0;

    /** Normal core temperature, °C. */
    public static final double COMFORT_CELSIUS = 36.8;
    /** Below this the player is in serious trouble. */
    public static final double HYPOTHERMIA_CELSIUS = 34.0;
    /** Below this is death. */
    public static final double DEATH_CELSIUS = 28.0;
    /** Above this is heat stroke. */
    public static final double HYPERTHERMIA_CELSIUS = 39.5;
    /** Above this is death. */
    public static final double HEAT_DEATH_CELSIUS = 42.0;

    private double coreCelsius = COMFORT_CELSIUS;

    public double coreCelsius() {
        return coreCelsius;
    }

    public void setCoreCelsius(double celsius) {
        if (!Double.isFinite(celsius)) throw new IllegalArgumentException("non-finite core");
        this.coreCelsius = celsius;
    }

    /** Conductance from core to ambient through clothing, W/K. */
    public static double conductance(double clothingClo) {
        if (!(clothingClo >= 0) || !Double.isFinite(clothingClo)) {
            throw new IllegalArgumentException("clothing must be finite and >= 0");
        }
        double resistance = clothingClo * CLO + SKIN_RESISTANCE;
        return BODY_AREA / resistance;
    }

    /**
     * One step of body temperature.
     *
     * @param ambientCelsius air temperature the player is standing in
     * @param clothingClo    summed insulation of worn layers
     * @param seconds        model seconds elapsed
     * @param activityWatts  metabolic output; {@link #RESTING_WATTS} at rest
     * @return new core temperature, °C
     */
    public double step(double ambientCelsius, double clothingClo,
                       double seconds, double activityWatts) {
        if (!(seconds > 0) || !Double.isFinite(seconds)) {
            throw new IllegalArgumentException("seconds must be finite and > 0");
        }
        if (!Double.isFinite(activityWatts)) {
            throw new IllegalArgumentException("activity must be finite");
        }
        double net = activityWatts - conductance(clothingClo) * (coreCelsius - ambientCelsius);
        coreCelsius += net * seconds / BODY_CAPACITY;
        return coreCelsius;
    }

    /** True when the player is dangerously cold. */
    public boolean isHypothermic() {
        return coreCelsius < HYPOTHERMIA_CELSIUS;
    }

    /** True when the player is dangerously hot. */
    public boolean isHyperthermic() {
        return coreCelsius > HYPERTHERMIA_CELSIUS;
    }

    /**
     * Air temperature at which this clothing is comfortable at rest: where the
     * 100 W of resting heat exactly balances the loss.
     */
    public static double comfortableAmbient(double clothingClo) {
        return COMFORT_CELSIUS - RESTING_WATTS / conductance(clothingClo);
    }
}
