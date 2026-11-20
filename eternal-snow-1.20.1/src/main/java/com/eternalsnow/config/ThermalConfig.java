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

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Eternal Snow 3.0 thermal engine").push("thermal");

        ENABLED = b.comment("Master switch. Off means the engine does nothing at all.")
                .define("enabled", true);

        AMBIENT_CELSIUS = b.comment("Outside air temperature in Celsius.",
                        "This is the climate of the world, not a difficulty slider:",
                        "the engine derives every wall loss from it.")
                .defineInRange("ambientCelsius", -30.0, -273.0, 200.0);

        SPEED_MULTIPLIER = b.comment("Model time per real second.",
                        "1.0 is physically honest. Higher values heat and cool the world",
                        "faster; 60 makes a 1 m woollen shell warm up in 43 minutes",
                        "instead of 43 hours, which is what cryogenic climates need.")
                .defineInRange("speedMultiplier", 60.0, 0.0, 1000.0);

        ZONE_RADIUS = b.comment("Half-size of the simulated box around each player, in blocks.",
                        "Cost grows with the cube of this: 8 is 4913 cells, 12 is 15625,",
                        "16 is 35937. Lower it if the server struggles.")
                .defineInRange("zoneRadius", 12, 4, 24);

        BODY_ENABLED = b.comment("Track player body temperature through clothing.")
                .define("bodyEnabled", true);

        b.pop();
        SPEC = b.build();
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
