package com.eternalsnow.thermal.cells;

import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import static org.junit.Assert.*;

public class CellThermalSavedDataTest {
    @Test public void dimensionStorePreservesMultipleChunksAndColdTemperatures() {
        CellThermalSavedData data=new CellThermalSavedData();
        data.chunk(1).put(10,80.123456789);
        data.chunk(2).put(20,-42.25);
        CellThermalSavedData loaded=CellThermalSavedData.load(data.save(new CompoundTag()));
        assertEquals(80.123456789,loaded.chunk(1).get(10),0);
        assertEquals(-42.25,loaded.chunk(2).get(20),0);
        assertTrue(Double.isNaN(loaded.chunk(1).get(999)));
    }
    @Test public void unloadedChunksRemainInSaveWithoutCatchUpTimestamp() {
        CellThermalSavedData data=new CellThermalSavedData();
        data.chunk(5).put(123,66.5);
        CompoundTag saved=data.save(new CompoundTag());
        assertFalse(saved.contains("lastTick"));
        for(int i=0;i<3;i++) data=CellThermalSavedData.load(data.save(new CompoundTag()));
        assertEquals(66.5,data.chunk(5).get(123),0);
    }
    @Test public void graphRebuildResumesStoredTemperatureWithoutResettingToWeather() {
        CellHeatSolver original=new CellHeatSolver(new double[]{70,-30},new double[]{50,1},
                new CellHeatSolver.Edge[]{new CellHeatSolver.Edge(0,1,.2)});
        original.step(.5);
        CellThermalSavedData data=new CellThermalSavedData();
        data.chunk(1).put(10,original.temperature(0));
        data.chunk(2).put(20,original.temperature(1));
        var restored=CellThermalSavedData.load(data.save(new CompoundTag()));
        CellHeatSolver rebuilt=new CellHeatSolver(new double[]{restored.chunk(1).get(10),restored.chunk(2).get(20)},
                new double[]{50,1},new CellHeatSolver.Edge[]{new CellHeatSolver.Edge(0,1,.2)});
        original.step(.5); rebuilt.step(.5);
        assertArrayEquals(original.temperatures(),rebuilt.temperatures(),0);
    }
}
