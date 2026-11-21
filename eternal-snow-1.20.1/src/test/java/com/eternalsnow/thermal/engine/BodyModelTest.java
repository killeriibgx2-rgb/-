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
        assertEquals(31.2, BodyModel.comfortableAmbient(0.0), 0.2);
    }

    @Test
    public void aWinterCoatMovesComfortToFourteenDegrees() {
        assertEquals(14.0, BodyModel.comfortableAmbient(2.0), 0.3);
    }

    @Test
    public void polarKitIsComfortableJustAboveFreezing() {
        assertEquals(5.4, BodyModel.comfortableAmbient(3.0), 0.3);
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
    public void aBodyInCryogenicAirSurvivesBrieflyAndThenDies() {
        // The body is 245 kJ/K, so one minute of -200 C costs about a quarter
        // of a degree, not a life. With 2 clo the margin to death is ~39 min —
        // plenty of time to feel the danger and act, which is the design goal.
        BodyModel body = new BodyModel();
        double afterMinute = body.step(-200, 2.0, 60, BodyModel.RESTING_WATTS);
        assertTrue("a minute must not kill: " + afterMinute,
                afterMinute > BodyModel.HYPOTHERMIA_CELSIUS);
        for (int i = 0; i < 60; i++) {
            body.step(-200, 2.0, 60, BodyModel.RESTING_WATTS);
        }
        assertTrue("but an hour must: " + body.coreCelsius(),
                body.coreCelsius() < BodyModel.DEATH_CELSIUS);
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
