package com.eternalsnow.thermal.cells;

import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import static org.junit.Assert.*;

public class CellTemperatureSnapshotTest {
    @Test public void persistenceRoundTripIsLossless() {
        long[] keys = {123,456,789};
        double[] temps = {-42.123456789, 0, 80.25};
        CellTemperatureSnapshot restored = CellTemperatureSnapshot.load(new CellTemperatureSnapshot(keys,temps).save());
        assertArrayEquals(keys,restored.positions());
        assertArrayEquals(temps,restored.temperatures(),0);
    }
    @Test public void callerCannotMutateStoredState() {
        long[] keys={1}; double[] temps={20};
        CellTemperatureSnapshot s = new CellTemperatureSnapshot(keys,temps);
        temps[0]=99; keys[0]=2; s.temperatures()[0]=44;
        assertEquals(20,s.temperatures()[0],0);
        assertEquals(1,s.positions()[0]);
    }
    @Test(expected=IllegalArgumentException.class) public void unsupportedVersionIsRejected() {
        CompoundTag tag=new CellTemperatureSnapshot(new long[]{1},new double[]{20}).save();
        tag.putInt("version",999);
        CellTemperatureSnapshot.load(tag);
    }
    @Test(expected=IllegalArgumentException.class) public void corruptLengthsAreRejected() {
        CompoundTag tag=new CellTemperatureSnapshot(new long[]{1},new double[]{20}).save();
        tag.putLongArray("temperatureBits",new long[0]);
        CellTemperatureSnapshot.load(tag);
    }
    @Test(expected=IllegalArgumentException.class) public void duplicatePositionsAreRejected() {
        new CellTemperatureSnapshot(new long[]{1,1},new double[]{20,30});
    }
    @Test(expected=IllegalArgumentException.class) public void nanIsRejected() {
        new CellTemperatureSnapshot(new long[]{1},new double[]{Double.NaN});
    }
}
