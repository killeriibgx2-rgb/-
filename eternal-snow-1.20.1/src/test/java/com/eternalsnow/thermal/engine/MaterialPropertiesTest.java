package com.eternalsnow.thermal.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The material table is the whole progression of the mod, so it is pinned down
 * by number. If someone "balances" a value, this test says so out loud.
 *
 * <p>Expected values are computed from published physical properties, not from
 * what felt fine in game.
 */
public class MaterialPropertiesTest {

    @Test
    public void woolInsulatesBetterThanWoodWhichBeatsStoneWhichBeatsSteel() {
        double wool = Transfer.wallTransmittance(Materials.WOOL);
        double wood = Transfer.wallTransmittance(Materials.WOOD);
        double stone = Transfer.wallTransmittance(Materials.STONE);
        double steel = Transfer.wallTransmittance(Materials.STEEL);

        assertTrue("wool should beat wood", wool < wood);
        assertTrue("wood should beat stone", wood < stone);
        assertTrue("stone should beat steel", stone < steel);
    }

    @Test
    public void oneMetreWallTransmittancesMatchBuildingPhysics() {
        // U = 1 / (d/λ + R_film), R_film = 0.165 m²K/W, d = 1 m.
        assertU(0.036, 0.0358, "wool");
        assertU(0.13, 0.1273, "wood");
        assertU(0.7, 0.6275, "brick");
        assertU(1.7, 1.3276, "stone");
        assertU(50, 5.4054, "steel");
    }

    private void assertU(double lambda, double expectedU, String label) {
        Material material = new Material(label, lambda, 1000, 1000, 0.9);
        assertEquals(label + " U-value", expectedU, Transfer.wallTransmittance(material), 1e-4);
    }

    @Test
    public void roomHeatLossAtMinus200MatchesTheDesignTable() {
        // 5x5x3 room: 110 m² envelope, inside +20 °C, outside -200 °C, ΔT = 220 K.
        double area = 110;
        double deltaT = 220;
        assertEquals("wool", 0.87, Transfer.wallTransmittance(Materials.WOOL) * area * deltaT / 1000, 0.05);
        assertEquals("wood", 3.08, Transfer.wallTransmittance(Materials.WOOD) * area * deltaT / 1000, 0.05);
        assertEquals("stone", 32.1, Transfer.wallTransmittance(Materials.STONE) * area * deltaT / 1000, 0.2);
    }

    @Test
    public void airHoldsAlmostNoHeatComparedToWalls() {
        ThermalCell air = new ThermalCell(Materials.AIR, Constants.toKelvin(20));
        ThermalCell wood = new ThermalCell(Materials.WOOD, Constants.toKelvin(20));
        assertTrue("a cubic metre of air is ~1.2 kJ/K", Math.abs(air.heatCapacity() - 1206) < 1);
        assertTrue("a cubic metre of wood is ~850 kJ/K", Math.abs(wood.heatCapacity() - 850_000) < 1);
        assertTrue("wood stores hundreds of times more than air", wood.heatCapacity() > 500 * air.heatCapacity());
    }

    @Test
    public void invalidMaterialValuesAreRejected() {
        assertRejected(() -> new Material("x", -1, 100, 100, 0.5));
        assertRejected(() -> new Material("x", 1, 0, 100, 0.5));
        assertRejected(() -> new Material("x", 1, 100, 100, 1.5));
        assertRejected(() -> new Material("", 1, 100, 100, 0.5));
    }

    private void assertRejected(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected IllegalArgumentException");
    }
}
