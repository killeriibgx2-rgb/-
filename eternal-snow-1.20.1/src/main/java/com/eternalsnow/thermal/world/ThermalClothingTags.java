package com.eternalsnow.thermal.world;

import com.eternalsnow.EternalSnowMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Datapack-extensible item → clothing insulation mapping, in clo. */
public final class ThermalClothingTags {
    public static final TagKey<Item> CLO_005 = tag("clothing/clo_005");
    public static final TagKey<Item> CLO_025 = tag("clothing/clo_025");
    public static final TagKey<Item> CLO_035 = tag("clothing/clo_035");
    public static final TagKey<Item> CLO_050 = tag("clothing/clo_050");
    public static final TagKey<Item> CLO_100 = tag("clothing/clo_100");
    public static final TagKey<Item> CLO_200 = tag("clothing/clo_200");
    public static final TagKey<Item> CLO_300 = tag("clothing/clo_300");

    private ThermalClothingTags() {
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(EternalSnowMod.MOD_ID, path));
    }
}
