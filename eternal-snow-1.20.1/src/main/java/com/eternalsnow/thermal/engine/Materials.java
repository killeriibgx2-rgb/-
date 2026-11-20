package com.eternalsnow.thermal.engine;

/**
 * The material table. Every number here is a published physical property, so a
 * player who knows how insulation works can predict the result before building.
 *
 * <p>Sources: building-physics tables for conductivity and density (wood
 * 0.13 W/(m·K), mineral wool 0.036, brick 0.7, concrete 0.5–1.8, steel 50);
 * standard specific heats. See docs/redesign-3.0-thermal-core.md §3.3.
 *
 * <p>This is deliberately a plain table, not a config file yet: the point of
 * stage 1 is that the numbers are honest and testable. Moving it to JSON is a
 * later step that changes no physics.
 */
public final class Materials {
    private Materials() {
    }

    /** Still air. Low mass, low conductivity — the reason a room responds fast. */
    public static final Material AIR = new Material("air", 0.026, 1.2, 1005, 0.0);

    /** Mineral wool / wool blocks. The best cheap insulator in the game. */
    public static final Material WOOL = new Material("wool", 0.036, 40, 840, 0.9);

    /** Softwood, the Minecraft default building block. */
    public static final Material WOOD = new Material("wood", 0.13, 500, 1700, 0.9);

    /** Lightweight concrete: decent structure, poor insulation. */
    public static final Material CONCRETE = new Material("concrete", 0.5, 1600, 880, 0.9);

    /** Common brick. */
    public static final Material BRICK = new Material("brick", 0.7, 1800, 840, 0.9);

    /** Window glass. Conducts better than brick, radiates a lot. */
    public static final Material GLASS = new Material("glass", 0.9, 2500, 840, 0.9);

    /** Stone: a heat sink, not an insulator. */
    public static final Material STONE = new Material("stone", 1.7, 2400, 840, 0.9);

    /** Liquid water near room temperature; phase change is a later model stage. */
    public static final Material WATER = new Material("water", 0.6, 1000, 4182, 0.96);

    /** Ice: conducts well, holds a lot of cold mass. */
    public static final Material ICE = new Material("ice", 2.2, 917, 2050, 0.97);

    /** Steel: a thermal bridge. Never a wall. Polished, so it barely radiates. */
    public static final Material STEEL = new Material("steel", 50, 7850, 490, 0.05);

    /** Packed snow, for completeness — low density, so it insulates a little. */
    public static final Material SNOW = new Material("snow", 0.2, 400, 2090, 0.9);
}
