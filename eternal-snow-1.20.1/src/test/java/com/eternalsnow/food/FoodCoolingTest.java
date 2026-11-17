package com.eternalsnow.food;

import org.junit.Test;
import static org.junit.Assert.*;

public class FoodCoolingTest {
    private int simulate(double env, int ticks) {
        int t = 36600;
        for (int n = 0; n < ticks; n += 200) {
            t = FoodCooling.advanceMilliC(t, env, Math.min(200, ticks - n), 4648);
        }
        return t;
    }

    @Test public void freezesAtMinus42() {
        assertTrue(simulate(-42, 3200) < -2000);
    }

    @Test public void threeMinuteAnchor() {
        assertEquals(-2000, simulate(-35, 3600), 100);
    }

    @Test public void coldPantryDoesNotStallAboveZero() {
        assertTrue(simulate(-5, 18000) < -2000);
    }

    @Test public void frequentScansMatchOneLongStep() {
        assertEquals(FoodCooling.advanceMilliC(36600, -42, 3600, 4648), simulate(-42, 3600), 20);
    }

    @Test public void noTimeNoChange() {
        assertEquals(36600, FoodCooling.advanceMilliC(36600, -42, 0, 4648));
    }
    @Test public void longLifeFoodStillAgesAtQuarterSpeed() {
        assertTrue(FoodCooling.spoilageMicros(50, 20, 288000) > 0);
    }

    @Test public void rotStopsWhenFrozenOrPaused() {
        assertEquals(0, FoodCooling.spoilageMicros(200, -3, 14400));
        assertEquals(0, FoodCooling.spoilageMicros(0, 20, 14400));
    }

    @Test public void quarterSpeedExtendsShelfLife() {
        assertEquals(FoodCooling.spoilageMicros(200, 20, 14400),
                4 * FoodCooling.spoilageMicros(50, 20, 14400), 4);
    }
}
