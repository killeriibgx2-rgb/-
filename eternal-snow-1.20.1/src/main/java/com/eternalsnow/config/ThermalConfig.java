package com.eternalsnow.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Everything an admin is expected to change lives here.
 *
 * <p>The outside temperature is deliberately a setting, not a constant: the
 * target climate of the mod is not fixed yet, and −200 °C is only the extreme
 * case the physics was stress-tested against.
 */
public final class ThermalConfig {
    private ThermalConfig() {
    }

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue AMBIENT_CELSIUS;
    public static final ForgeConfigSpec.DoubleValue SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue ZONE_RADIUS;
    public static final ForgeConfigSpec.BooleanValue BODY_ENABLED;
    public static final ForgeConfigSpec.BooleanValue DOOR_INFILTRATION;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Eternal Snow 3.0 thermal engine").push("thermal");

        ENABLED = b.comment("Master switch. Off means the engine does nothing at all.")
                .define("enabled", true);

        AMBIENT_CELSIUS = b.comment("Fallback outside air temperature in Celsius.",
                        "The default is a temporary playtest value, not the chosen target climate.",
                        "A biome, altitude, time and weather field is not calibrated yet.")
                .defineInRange("ambientCelsius", -30.0, -273.0, 200.0);

        SPEED_MULTIPLIER = b.comment("Model time per real second.",
                        "1.0 is the unaccelerated physical baseline; higher values heat and cool faster.",
                        "60 is an example for extreme climates, not a default: the target climate is unknown.")
                .defineInRange("speedMultiplier", 1.0, 0.0, 1000.0);

        ZONE_RADIUS = b.comment("Half-size of the simulated box around each player, in blocks.",
                        "Cost grows with the cube of this: 8 is 4913 cells, 12 is 15625,",
                        "16 is 35937. Lower it if the server struggles.")
                .defineInRange("zoneRadius", 8, 4, 16);

        BODY_ENABLED = b.comment("Track player body temperature through clothing.")
                .define("bodyEnabled", true);

        DOOR_INFILTRATION = b.comment("Open doors between indoor and outside air leak heat.")
                .define("doorInfiltration", true);

        b.pop();
        SPEC = b.build();
    }

    public static boolean isValid(double ambientCelsius, double speedMultiplier, int zoneRadius) {
        return Double.isFinite(ambientCelsius) && ambientCelsius >= -273.0 && ambientCelsius <= 200.0
                && Double.isFinite(speedMultiplier) && speedMultiplier >= 0.0 && speedMultiplier <= 1000.0
                && zoneRadius >= 4 && zoneRadius <= 16;
    }

    /** Applies and persists administrator-edited values on the logical server. */
    public static void applyFromPanel(boolean enabled, double ambientCelsius, double speedMultiplier,
                                      int zoneRadius, boolean bodyEnabled, boolean doorInfiltration) {
        if (!isValid(ambientCelsius, speedMultiplier, zoneRadius)) {
            throw new IllegalArgumentException("Thermal settings are outside their allowed ranges");
        }
        ENABLED.set(enabled);
        AMBIENT_CELSIUS.set(ambientCelsius);
        SPEED_MULTIPLIER.set(speedMultiplier);
        ZONE_RADIUS.set(zoneRadius);
        BODY_ENABLED.set(bodyEnabled);
        DOOR_INFILTRATION.set(doorInfiltration);
        SPEED_MULTIPLIER.save();
    }

    /** Safe accessor used by the engine: works even before configs load. */
    public static boolean enabled() {
        try {
            return ENABLED.get();
        } catch (RuntimeException notLoadedYet) {
            return true;
        }
    }
}
