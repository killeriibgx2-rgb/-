package com.eternalsnow.thermal;

import org.junit.Test;
import static org.junit.Assert.*;

public class RoomThermalBalanceTest {
    @Test public void removingFireDoesNotEraseWarmth() {
        double t = RoomThermalBalance.advance(20, -42, 0, 10, 2600);
        assertTrue(t > 19 && t < 20);
    }
    @Test public void weatherChangeIsNotInstant() {
        double t = RoomThermalBalance.advance(20, -60, 62, 10, 2600);
        assertTrue(t > 19 && t < 20);
    }
    @Test public void capacitySlowsHeatingAndCooling() {
        assertTrue(RoomThermalBalance.advance(-42,-42,62,200,5200)
                < RoomThermalBalance.advance(-42,-42,62,200,2600));
        assertTrue(RoomThermalBalance.advance(20,-42,0,200,5200)
                > RoomThermalBalance.advance(20,-42,0,200,2600));
    }
    @Test public void elapsedTimeIsIndependentOfStepCount() {
        double t = 20;
        for (int i=0;i<100;i++) t = RoomThermalBalance.advance(t,-42,0,10,2600);
        assertEquals(RoomThermalBalance.advance(20,-42,0,1000,2600),t,1e-9);
    }
    @Test public void noTimeNoJumpAndLongAbsenceNoOvershoot() {
        assertEquals(20,RoomThermalBalance.advance(20,-42,0,0,2600),0);
        assertEquals(-42,RoomThermalBalance.advance(20,-42,0,1000000,2600),1e-6);
    }
}
