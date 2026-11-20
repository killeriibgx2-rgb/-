package com.eternalsnow.thermal.engine;

/**
 * Rated heat output of the vanilla devices the mod treats as heaters, and the
 * reference values for a future modded stove.
 *
 * <p>These are the only "balance" numbers in the whole engine, and they exist
 * because vanilla mechanics are kept as-is. Everything else in the model comes
 * from materials and geometry.
 */
public final class Devices {
    private Devices() {
    }

    /** A campfire: the primary way to stay alive. Strongest vanilla heater. */
    public static final double CAMPFIRE_WATTS = 5000;

    /** A lit furnace: incidental heat while smelting, not a heater. */
    public static final double FURNACE_WATTS = 1000;

    /** A lit soul campfire: hotter flame, more heat. */
    public static final double SOUL_CAMPFIRE_WATTS = 6500;

    /** A torch or lantern: barely measurable, but real. */
    public static final double TORCH_WATTS = 50;

    /** A future dedicated stove with slow burning — the honest-joules path. */
    public static final double MODDED_STOVE_WATTS = 8000;
}
