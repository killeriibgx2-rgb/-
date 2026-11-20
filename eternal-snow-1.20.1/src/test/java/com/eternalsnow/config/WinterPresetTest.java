package com.eternalsnow.config;
import org.junit.Test;
import static org.junit.Assert.*;
public class WinterPresetTest {
    @Test public void mediumUsesApprovedTorchAndManualEditsRemainPossible() {
        var s=new ServerSnowSettings();WinterPreset.MEDIUM.apply(s);
        assertEquals(7,s.heatTorch,0);assertEquals(-34,s.minWorldTempC);
        s.heatTorch=11;assertEquals(11,s.heatTorch,0);
    }
    @Test public void difficultiesIncreaseColdFoodAndThirstPressure() {
        var prev=new ServerSnowSettings();WinterPreset.EASY.apply(prev);
        for(var p:new WinterPreset[]{WinterPreset.MEDIUM,WinterPreset.HARD,WinterPreset.HARDCORE}) {
            var s=new ServerSnowSettings();p.apply(s);
            assertTrue(s.minWorldTempC<prev.minWorldTempC);
            assertTrue(s.playerTempRateMultiplier>prev.playerTempRateMultiplier);
            assertTrue(s.thirstDrainMultiplier>prev.thirstDrainMultiplier);
            assertTrue(s.foodSpoilageSpeedPercent>prev.foodSpoilageSpeedPercent);
            assertTrue(s.heatTorch<prev.heatTorch);prev=s;
        }
    }
    @Test public void presetsResetBalanceButPreserveCustomDrinkIntegration() {
        var s=new ServerSnowSettings();s.customDrinks="example:drink";
        s.heatTorch=80;s.blockConductivity[0]=.9F;s.enablePlayerTemperature=false;
        WinterPreset.MEDIUM.apply(s);
        assertTrue(s.enablePlayerTemperature);assertEquals(new ServerSnowSettings().blockConductivity[0],s.blockConductivity[0],.0001F);
        assertEquals("example:drink",s.customDrinks);
        var other=new ServerSnowSettings();WinterPreset.HARD.apply(other);WinterPreset.MEDIUM.apply(other);
        assertEquals(s.heatFireplace,other.heatFireplace,0);
        assertEquals(s.foodFreezingSpeedPercent,other.foodFreezingSpeedPercent);
    }
}
