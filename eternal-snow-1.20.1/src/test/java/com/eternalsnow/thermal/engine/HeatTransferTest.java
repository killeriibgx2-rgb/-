package com.eternalsnow.thermal.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The three transfer channels, checked against hand-computed numbers. */
public class HeatTransferTest {

    @Test
    public void conductionBetweenTwoStoneCellsMatchesSeriesResistance() {
        // R = 0.5/1.7 + 0.5/1.7 = 0.5882 m²K/W, A = 1 m², G = 1.7 W/K.
        double g = Transfer.conductionConductance(Materials.STONE, Materials.STONE);
        assertEquals(1.7, g, 0.001);
    }

    @Test
    public void conductionIsSymmetric() {
        assertEquals(
                Transfer.conductionConductance(Materials.WOOD, Materials.STEEL),
                Transfer.conductionConductance(Materials.STEEL, Materials.WOOD),
                1e-12);
    }

    @Test
    public void airToSolidTransferIncludesTheSolidHalfCellResistance() {
        double expectedWood = 1.0 / (1.0 / Constants.FREE_CONVECTION
                + (Constants.CELL_SIZE / 2.0) / Materials.WOOD.conductivity());
        assertEquals(expectedWood, Transfer.airSolidConductance(Materials.WOOD), 1e-12);
        assertTrue("a whole wooden block must not instantly absorb heat at its surface",
                Transfer.airSolidConductance(Materials.WOOD) < Transfer.convectionConductance());
        assertTrue("wool insulates the air from the block core",
                Transfer.airSolidConductance(Materials.WOOL)
                        < Transfer.airSolidConductance(Materials.WOOD));
    }

    @Test
    public void buoyancyCarriesHeatFromTheLowerAirCellUpward() {
        ThermalGrid grid = new ThermalGrid(1, 2, 1);
        grid.set(0, 0, 0, Materials.AIR, Constants.toKelvin(30));
        grid.set(0, 1, 0, Materials.AIR, Constants.toKelvin(-3));
        double before = grid.totalEnergy();
        double lowerBefore = grid.cell(0, 0, 0).temperatureKelvin();
        double upperBefore = grid.cell(0, 1, 0).temperatureKelvin();

        assertTrue("a warm lower layer should trigger an updraft",
                grid.faceConductance(0, 0, 0, ThermalGrid.Axis.Y)
                        > Transfer.horizontalAirMixingConductance());
        grid.step(1.0);

        assertTrue("lower air should lose heat", grid.cell(0, 0, 0).temperatureKelvin() < lowerBefore);
        assertTrue("the voxel above should gain heat", grid.cell(0, 1, 0).temperatureKelvin() > upperBefore);
        assertEquals("air mixing must conserve energy", before, grid.totalEnergy(), Math.abs(before) * 1e-12);
    }

    @Test
    public void aStableInversionKeepsReturnFlowButReceivesNoExtraUpdraft() {
        ThermalGrid grid = new ThermalGrid(1, 2, 1);
        grid.set(0, 0, 0, Materials.AIR, Constants.toKelvin(-3));
        grid.set(0, 1, 0, Materials.AIR, Constants.toKelvin(30));

        double returnFlow = grid.faceConductance(0, 0, 0, ThermalGrid.Axis.Y);
        assertEquals(Transfer.horizontalAirMixingConductance(), returnFlow, 1e-12);
        assertTrue(returnFlow < Transfer.maximumVerticalAirMixingConductance());
    }

    @Test
    public void aSteelBoltThroughAWallLeaksFarMoreThanTheWall() {
        double wood = Transfer.conductionConductance(Materials.WOOD, Materials.WOOD);
        double steel = Transfer.conductionConductance(Materials.STEEL, Materials.STEEL);
        assertTrue("steel conducts ~385x better per cell", steel > 300 * wood);
    }

    @Test
    public void radiationBecomesDominantAtCryogenicTemperatureDifferences() {
        double hot = Constants.toKelvin(20);
        double cold = Constants.toKelvin(-200);
        double radiative = Transfer.radiativeConductance(Materials.WOOL, Materials.WOOL, hot, cold);
        double conductive = Transfer.wallTransmittance(Materials.WOOL);
        // Per square metre: radiation carries more than conduction through wool.
        assertTrue("radiation " + radiative + " should exceed wall conduction " + conductive,
                radiative > conductive);
    }

    @Test
    public void radiationVanishesForLowEmissivity() {
        double hot = Constants.toKelvin(20);
        double cold = Constants.toKelvin(-200);
        double shiny = Transfer.radiativeConductance(Materials.STEEL, Materials.WOOD, hot, cold);
        double matte = Transfer.radiativeConductance(Materials.WOOL, Materials.WOOD, hot, cold);
        assertTrue("a polished surface radiates less", shiny < matte / 5);
    }

    @Test
    public void radiationIsZeroForAZeroEmissivityPair() {
        Material mirror = new Material("mirror", 1, 1000, 1000, 0.0);
        assertEquals(0.0, Transfer.radiativeConductance(mirror, mirror,
                Constants.toKelvin(20), Constants.toKelvin(-200)), 1e-12);
    }

    @Test
    public void radiativeConductanceIsSymmetricInTemperature() {
        double a = Constants.toKelvin(30);
        double b = Constants.toKelvin(-100);
        assertEquals(
                Transfer.radiativeConductance(Materials.WOOD, Materials.WOOL, a, b),
                Transfer.radiativeConductance(Materials.WOOL, Materials.WOOD, b, a),
                1e-12);
    }
}
