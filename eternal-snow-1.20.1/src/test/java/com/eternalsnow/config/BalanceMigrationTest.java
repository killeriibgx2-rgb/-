package com.eternalsnow.config;

import com.eternalsnow.thermal.BlockThermalCategory;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * A world saved by an older build keeps every setting in its NBT, including the source wattages
 * and the material tables. The owner's room stayed at -8.1 C after the rebalance because his
 * world still had a 68 W campfire, 0.42 brick and 1.0 air conduction: the new physics never had
 * a chance to apply.
 */
public class BalanceMigrationTest {

    /** NBT as an older build wrote it: no balanceRevision tag at all. */
    private static CompoundTag legacyWorld(int revision) {
        CompoundTag tag = new CompoundTag();
        if (revision > 0) tag.putInt("balanceRevision", revision);
        tag.putFloat("heatTorch", 7.0F);
        tag.putFloat("heatLantern", 20.0F);
        tag.putFloat("heatFurnace", 48.0F);
        tag.putFloat("heatBlastSmoker", 62.0F);
        tag.putFloat("heatCampfire", 68.0F);
        tag.putFloat("heatSoulCampfire", 45.0F);
        tag.putFloat("heatFireplace", 94.0F);
        tag.putFloat("heatMagma", 75.0F);
        tag.putFloat("heatLava", 115.0F);
        var cond = new CompoundTag();
        var cap = new CompoundTag();
        for (var cat : BlockThermalCategory.VALUES) {
            cond.putFloat(cat.id, cat.defaultConductivity * 10F);   // shipped before the change
            cap.putFloat(cat.id, cat.defaultCapacity);
        }
        tag.put("blockConductivity", cond);
        tag.put("blockCapacity", cap);
        return tag;
    }

    @Test public void anOldWorldGetsTheNewBalance() {
        var s = new ServerSnowSettings();
        s.loadFromTag(legacyWorld(0));
        var now = new ServerSnowSettings();
        System.out.println(String.format(java.util.Locale.ROOT,
            "Migrated campfire %.0f W (new default %.0f), brick conductivity %.4f (new %.4f)",
            s.heatCampfire, now.heatCampfire,
            s.blockConductivity[BlockThermalCategory.BRICK.ordinal()],
            now.blockConductivity[BlockThermalCategory.BRICK.ordinal()]));
        assertEquals("an old campfire must be rescaled to the new physics", now.heatCampfire,
                s.heatCampfire, 0.5F);
        assertEquals("brick must get the new resistance", now.blockConductivity[BlockThermalCategory.BRICK.ordinal()],
                s.blockConductivity[BlockThermalCategory.BRICK.ordinal()], 0.001F);
        assertEquals("brick must keep its capacity", now.blockCapacity[BlockThermalCategory.BRICK.ordinal()],
                s.blockCapacity[BlockThermalCategory.BRICK.ordinal()], 0.001F);
    }

    @Test public void aTunedWorldKeepsItsRatio() {
        CompoundTag tag = legacyWorld(0);
        tag.putFloat("heatCampfire", 136.0F);          // player doubled the campfire
        CompoundTag cond = tag.getCompound("blockConductivity");
        cond.putFloat(BlockThermalCategory.BRICK.id, .84F);   // and made brick a leak
        tag.put("blockConductivity", cond);
        var s = new ServerSnowSettings();
        s.loadFromTag(tag);
        var now = new ServerSnowSettings();
        assertEquals("a doubled campfire stays doubled", now.heatCampfire * 2, s.heatCampfire, 1F);
        assertEquals("a doubled conductivity stays doubled",
                now.blockConductivity[BlockThermalCategory.BRICK.ordinal()] * 2,
                s.blockConductivity[BlockThermalCategory.BRICK.ordinal()], 0.001F);
    }

    @Test public void aNewWorldIsLeftAlone() {
        var fresh = new ServerSnowSettings();
        CompoundTag tag = fresh.saveToTag(new CompoundTag());
        var loaded = new ServerSnowSettings();
        loaded.loadFromTag(tag);
        assertEquals(fresh.heatCampfire, loaded.heatCampfire, 0.001F);
        assertEquals(fresh.heatTorch, loaded.heatTorch, 0.001F);
        assertEquals(fresh.blockConductivity[BlockThermalCategory.BRICK.ordinal()],
                loaded.blockConductivity[BlockThermalCategory.BRICK.ordinal()], 0.0001F);
    }
}
