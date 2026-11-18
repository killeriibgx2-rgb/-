package com.eternalsnow.village;

import org.junit.Test;
import static org.junit.Assert.*;

public class ConfectionerPolicyTest {
    @Test public void onlyVanillaResidentialTemplates() {
        for(String biome:new String[]{"plains","desert","savanna","snowy","taiga"})
            assertTrue(ConfectionerPolicy.eligibleTemplate("minecraft","village/"+biome+"/houses/test"));
        assertFalse(ConfectionerPolicy.eligibleTemplate("othermod","village/plains/houses/test"));
        assertFalse(ConfectionerPolicy.eligibleTemplate("minecraft","village/plains/zombie/houses/test"));
        assertFalse(ConfectionerPolicy.eligibleTemplate("minecraft","village/plains/streets/test"));
        assertFalse(ConfectionerPolicy.eligibleTemplate("minecraft","igloo/top"));
    }
    @Test public void agreedBuyPrices() {
        assertEquals(12,ConfectionerPolicy.SUGAR_BUY);
        assertEquals(8,ConfectionerPolicy.BEANS_BUY);
        assertEquals(4,ConfectionerPolicy.CARAMEL_BUY);
        assertEquals(3,ConfectionerPolicy.CANDY_BUY);
    }
    @Test public void protectedCandyRoundTripLosesEmeralds() {
        for(int purchases=1;purchases<100;purchases++) {
            int returned=purchases*ConfectionerPolicy.CANDY_SELL_COUNT/ConfectionerPolicy.CANDY_BUY;
            assertTrue(returned<purchases*ConfectionerPolicy.CANDY_SELL_PRICE);
        }
    }
}
