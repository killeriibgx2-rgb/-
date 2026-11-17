package com.eternalsnow.config;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.Test;
import static org.junit.Assert.*;

public class FoodSettingsTest {
    private ServerSnowSettings custom() {
        ServerSnowSettings s = new ServerSnowSettings();
        s.foodSpoilageSpeedPercent = 15;
        s.foodFreezingSpeedPercent = 250;
        s.foodThawingSpeedPercent = 0;
        return s;
    }

    private void check(ServerSnowSettings s) {
        assertEquals(15, s.foodSpoilageSpeedPercent);
        assertEquals(250, s.foodFreezingSpeedPercent);
        assertEquals(0, s.foodThawingSpeedPercent);
    }

    @Test public void copiesFields() { check(custom().copy()); }

    @Test public void persistsFields() {
        CompoundTag tag = new CompoundTag();
        custom().saveToTag(tag);
        ServerSnowSettings s = new ServerSnowSettings();
        s.loadFromTag(tag);
        check(s);
    }

    @Test public void networkRoundTrip() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            custom().writeToBuf(buf);
            ServerSnowSettings s = new ServerSnowSettings();
            s.readFromBuf(buf);
            check(s);
            assertEquals(0, buf.readableBytes());
        } finally { buf.release(); }
    }

    @Test public void clampsAndDefaults() {
        ServerSnowSettings s = new ServerSnowSettings();
        assertEquals(25, s.foodSpoilageSpeedPercent);
        s.foodFreezingSpeedPercent = -100;
        s.foodThawingSpeedPercent = 5000;
        s.clamp();
        assertEquals(0, s.foodFreezingSpeedPercent);
        assertEquals(400, s.foodThawingSpeedPercent);
    }
}
