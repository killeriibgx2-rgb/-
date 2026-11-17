package com.eternalsnow.food;

import com.eternalsnow.EternalSnowMod;
import com.eternalsnow.registry.ModRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * v2.7 — how fast a given food freezes/spoils.
 *
 * <p>Classification is done with item tags, not with hard-coded item checks, so a datapack (or
 * Farmer's Delight through {@code required:false} entries) can move any item into the right group
 * without touching code.
 */
public final class FoodProfiles {

    /** Never spoils: our drinks, candy, anything the player does not expect to rot. */
    public static final int PRESERVED = 0;
    /** Raw meat — the main reason the whole system exists. */
    public static final int RAW_MEAT = 1;
    /** Fish goes off first. */
    public static final int FISH = 2;
    /** Cooked meat and cooked dishes: keeps noticeably longer than raw. */
    public static final int COOKED = 3;
    /** Dry baked goods (bread, cookies, pie). */
    public static final int DRY = 4;
    /** Fruit and vegetables. */
    public static final int PRODUCE = 5;

    public static final TagKey<Item> RAW_MEAT_TAG = tag("foods/raw_meat");
    public static final TagKey<Item> FISH_TAG = tag("foods/fish");
    public static final TagKey<Item> COOKED_TAG = tag("foods/cooked");
    public static final TagKey<Item> DRY_TAG = tag("foods/dry");
    public static final TagKey<Item> PRODUCE_TAG = tag("foods/produce");
    public static final TagKey<Item> PRESERVED_TAG = tag("foods/preserved");

    private FoodProfiles() {
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                new ResourceLocation(EternalSnowMod.MODID, path));
    }

    /**
     * @return spoilage profile of this stack, or {@code -1} when the item is not food at all and the
     *         whole system should leave it alone.
     */
    public static int profileOf(ItemStack stack) {
        if (stack.isEmpty() || !stack.isEdible()) {
            return -1;
        }
        if (stack.is(PRESERVED_TAG) || isModDrink(stack)) {
            return PRESERVED;
        }
        if (stack.is(RAW_MEAT_TAG)) {
            return RAW_MEAT;
        }
        if (stack.is(FISH_TAG)) {
            return FISH;
        }
        if (stack.is(COOKED_TAG)) {
            return COOKED;
        }
        if (stack.is(DRY_TAG)) {
            return DRY;
        }
        if (stack.is(PRODUCE_TAG)) {
            return PRODUCE;
        }
        // Unknown edible (a mod we have never seen, or a datapack dish): treat it as preserved.
        // Better to do nothing than to rot somebody's food they cannot replace.
        return PRESERVED;
    }

    /** Our own hot drinks must never spoil — they are the heat mechanic itself. */
    private static boolean isModDrink(ItemStack stack) {
        return stack.is(ModRegistry.COCOA_CUP.get())
                || stack.is(ModRegistry.CARAMEL_CUP.get())
                || stack.is(ModRegistry.CARAMEL.get());
    }

    /** Everything edible can be frozen, even preserved food — freezing is about temperature, not rot. */
    public static boolean canFreeze(ItemStack stack) {
        return stack.isEdible();
    }
}
