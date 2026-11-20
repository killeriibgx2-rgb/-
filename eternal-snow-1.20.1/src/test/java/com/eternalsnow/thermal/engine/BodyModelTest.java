package com.eternalsnow.thermal.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Clothing as physics, not as a list of "+2 warmth" buffs.
 *
 * <p>Computed expectations: 100 W of resting heat across the clothing
 * resistance gives the comfortable air temperature for each outfit. A naked
 * body is comfortable only at about +31 °C; this is why the first thing a
 * player needs in a cold world is a coat, not a bigger fire.
 */
public class BodyModelTest {

    @Test
    public void nakedComfortIsAboutThirtyOneDegrees() {
        assertEquals(30.9, BodyModel.comfortableAmbient(0.0), 0.2);
    }

    @Test
    public void aWinterCoatMovesComfortToFourteenDegrees() {
        assertEquals(13.7, BodyModel.comfortableAmbient(2.0), 0.3);
    }

    @Test
    public void polarKitIsComfortableJustAboveFreezing() {
        assertEquals(5.1, BodyModel.comfortableAmbient(3.0), 0.3);
    }

    @Test
    public void everyLayerHelpsAndNoneIsAThreshold() {
        double previous = BodyModel.comfortableAmbient(0);
        for (double clo = 0.5; clo <= 4.0; clo += 0.5) {
            double now = BodyModel.comfortableAmbient(clo);
            assertTrue("more clothing must never hurt: " + clo + " clo -> " + now, now < previous);
            previous = now;
        }
    }

    @Test
    public void conductanceMatchesTheSeriesResistance() {
        // R = 1 clo * 0.155 + 0.1 skin; G = 1.8 / R.
        assertEquals(18.0, BodyModel.conductance(0), 0.01);
        assertEquals(7.06, BodyModel.conductance(1), 0.02);
    }

    @Test
    public void aBodyInFreezingAirLosesHeatFast() {
        BodyModel body = new BodyModel();
        double after = body.step(-200, 2.0, 60, BodyModel.RESTING_WATTS);
        assertTrue("one minute naked-ish in -200 C must be lethal territory, got " + after,
                after < BodyModel.DEATH_CELSIUS);
    }

    @Test
    public void aBodyInAColdButShelteredRoomStaysAliveForAWhile() {
        BodyModel body = new BodyModel();
        // +10 C in a winter coat: below comfort, but survivable for a long time.
        for (int i = 0; i < 60; i++) {
            body.step(10, 2.0, 60, BodyModel.RESTING_WATTS);
        }
        assertTrue("should be chilled but alive: " + body.coreCelsius(),
                body.coreCelsius() > BodyModel.DEATH_CELSIUS);
        assertTrue("should be drifting down: " + body.coreCelsius(),
                body.coreCelsius() < BodyModel.COMFORT_CELSIUS);
    }

    @Test
    public void noAmountOfClothingKeepsYouWarmAtMinusTwoHundred() {
        // Honest limit of the model: clothing slows loss, it does not stop it.
        BodyModel body = new BodyModel();
        for (int i = 0; i < 120; i++) {
            body.step(-200, 4.0, 60, BodyModel.RESTING_WATTS);
        }
        assertTrue("even 4 clo must fail at -200 C: " + body.coreCelsius(),
                body.coreCelsius() < BodyModel.COMFORT_CELSIUS);
        assertTrue(body.isHypothermic());
    }

    @Test
    public void activityBuysTime() {
        BodyModel resting = new BodyModel();
        BodyModel working = new BodyModel();
        for (int i = 0; i < 30; i++) {
            resting.step(-50, 2.0, 60, BodyModel.RESTING_WATTS);
            working.step(-50, 2.0, 60, 400);
        }
        assertTrue("working must keep you warmer: " + working.coreCelsius()
                        + " vs " + resting.coreCelsius(),
                working.coreCelsius() > resting.coreCelsius());
    }

    @Test
    public void overheatingIsDetectedToo() {
        BodyModel body = new BodyModel();
        body.step(200, 0, 600, 500);
        assertTrue("sitting naked at +200 C must be hyperthermic", body.isHyperthermic());
        assertFalse(body.isHypothermic());
    }

    @Test
    public void invalidInputsAreRejected() {
        BodyModel body = new BodyModel();
        assertRejected(() -> body.step(0, -1, 1, 100));
        assertRejected(() -> body.step(0, 1, 0, 100));
        assertRejected(() -> body.step(0, 1, 1, Double.NaN));
        assertRejected(() -> BodyModel.conductance(-0.5));
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
