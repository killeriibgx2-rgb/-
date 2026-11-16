package com.eternalsnow.block;

import com.eternalsnow.item.CopperThermosItem;
import com.eternalsnow.item.EmptyCupItem;
import com.eternalsnow.item.HeatScannerItem;
import com.eternalsnow.item.HotDrinkCupItem;
import com.eternalsnow.item.SnowShovelItem;
import com.eternalsnow.item.ThermometerItem;
import com.eternalsnow.item.WarmingPillItem;
import com.eternalsnow.item.WoodenFirestarterItem;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;

import java.util.Locale;

/**
 * Fixed 0..100% Fireplace Fuel Scale + Chimney Damper Draft Physics:
 * - Fixed Fuel Capacity = 48,000,000 milli-units (48,000 base ticks = 100% bar).
 * - 1 Log = +5% (20 Logs = 100%), 1 Coal Block = +25% (4 Coal Blocks = 100%).
 * - Fuel bar fill (%) NEVER jumps when adjusting the chimney damper — only the calculated burn time
 *   and thermal output dynamically change!
 * - Damper = 0% (closed): zero draft -> fireplace immediately extinguishes.
 * - Damper = 1%..25% (low draft): smolders barely, heat drops down to 20%, burn time stretches up to ~5.5x (3h 42m!).
 * - Damper = 100% (wide open): roaring maximum draft, 40:00 full burn time, surging 155% heat power!
 */
public final class FireplaceFuelHelper {
    public static final int MAX_FUEL_TICKS = 48000; // Base reference ticks at 100% open damper (40:00)
    public static final int MILLI_PER_TICK = 1000;
    public static final int MAX_FUEL_MILLI = MAX_FUEL_TICKS * MILLI_PER_TICK; // 48,000,000 = 100.0% capacity

    private FireplaceFuelHelper() {
    }

    /**
     * Fuel burn rate in milli-units per server tick for a given Chimney Damper opening (0..100%).
     * Returns 0 when damper is closed (0%).
     */
    public static int getBurnMilliPerTick(int damperPercent) {
        if (damperPercent <= 0) {
            return 0;
        }
        float t = Mth.clamp((damperPercent - 1) / 99.0F, 0.0F, 1.0F);
        // 1% damper -> 180 milli/tick (0.18x burn rate = 5.55x longer burn time: 3h 42m full load!)
        // 100% damper -> 1000 milli/tick (1.00x burn rate = 40:00 full load)
        return Math.round(Mth.lerp((float) Math.pow(t, 0.85D), 180.0F, 1000.0F));
    }

    /**
     * Thermal heat output multiplier for ThermalEngine based on Chimney Damper opening (0..100%).
     */
    public static float getHeatPowerMultiplier(int damperPercent) {
        if (damperPercent <= 0) {
            return 0.0F;
        }
        float t = Mth.clamp((damperPercent - 1) / 99.0F, 0.0F, 1.0F);
        // 1% damper -> 0.20x (barely warm smolder)
        // 60% damper -> ~0.95x (comfortable room warmth)
        // 100% damper -> 1.55x (very strong roaring blast of heat!)
        return Mth.lerp((float) Math.pow(t, 1.08D), 0.20F, 1.55F);
    }

    /**
     * Computes actual remaining ticks of burn time for `fuelMilli` at `damperPercent`.
     * If damperPercent == 0, previews burn time as if at 100% or returns 0 where needed.
     */
    public static int computeRemainingRealTicks(int fuelMilli, int damperPercent) {
        if (fuelMilli <= 0) {
            return 0;
        }
        int rate = getBurnMilliPerTick(Math.max(1, damperPercent));
        return (int) (((long) fuelMilli + rate - 1L) / rate);
    }

    public static float getFillRatio(int fuelMilli) {
        return Mth.clamp(fuelMilli / (float) MAX_FUEL_MILLI, 0.0F, 1.0F);
    }

    public static int getFillPercentRounded(int fuelMilli) {
        if (fuelMilli <= 0) {
            return 0;
        }
        return Mth.clamp(Math.max(1, Math.round(getFillRatio(fuelMilli) * 100.0F)), 1, 100);
    }

    public static String formatTicksMMSS(int ticks) {
        int totalSec = Math.max(0, (ticks + 19) / 20);
        int hours = totalSec / 3600;
        int min = (totalSec % 3600) / 60;
        int sec = totalSec % 60;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, min, sec);
        }
        return String.format(Locale.ROOT, "%02d:%02d", min, sec);
    }

    public static int getFuelMilliUnits(ItemStack stack) {
        return getFuelTicks(stack) * MILLI_PER_TICK;
    }

    public static int getFuelTicks(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Item item = stack.getItem();

        // Protect Enchanted Books & Eternal Snow instruments/drinks
        if (item instanceof EnchantedBookItem
                || item instanceof WoodenFirestarterItem
                || item instanceof CopperThermosItem
                || item instanceof EmptyCupItem
                || item instanceof HotDrinkCupItem
                || item instanceof ThermometerItem
                || item instanceof HeatScannerItem
                || item instanceof SnowShovelItem
                || item instanceof WarmingPillItem) {
            return 0;
        }

        // 1. Top-tier dense fuels (calibrated: 4 Coal Blocks = 100%, 20 Logs = 100%)
        if (stack.is(Items.COAL_BLOCK)) {
            return 12000; // 25.0% of scale
        }
        if (stack.is(Items.LAVA_BUCKET)) {
            return 24000; // 50.0% of scale (returns empty bucket)
        }
        if (stack.is(ItemTags.LOGS) || stack.is(ItemTags.LOGS_THAT_BURN) || stack.is(Items.BAMBOO_BLOCK)) {
            return 2400; // 5.0% of scale (20 logs = 100%!)
        }
        if (stack.is(Items.DRIED_KELP_BLOCK)) {
            return 3600; // 7.5% of scale
        }
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.is(Items.BLAZE_ROD)) {
            return 1400; // ~2.9% of scale
        }

        // 2. Processed wood (planks, fences, gates, doors, trapdoors, stairs, signs, ladders, boats, chests)
        if (stack.is(ItemTags.PLANKS)
                || stack.is(ItemTags.WOODEN_FENCES)
                || stack.is(ItemTags.FENCE_GATES)
                || stack.is(ItemTags.WOODEN_DOORS)
                || stack.is(ItemTags.WOODEN_TRAPDOORS)
                || stack.is(ItemTags.WOODEN_STAIRS)
                || stack.is(ItemTags.SIGNS)
                || stack.is(ItemTags.BOATS)
                || stack.is(Items.LADDER)
                || stack.is(Items.CHEST)
                || stack.is(Items.BARREL)
                || stack.is(Items.CRAFTING_TABLE)) {
            return 400;
        }

        // 3. Old gear & mob junk (bows, crossbows, rods, wooden tools, leather armor, rotten flesh)
        if (item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof FishingRodItem
                || (item instanceof TieredItem ti && ti.getTier() == Tiers.WOOD)
                || (item instanceof ArmorItem ai && ai.getMaterial() == ArmorMaterials.LEATHER)
                || stack.is(Items.ROTTEN_FLESH)
                || stack.is(Items.SPIDER_EYE)
                || stack.is(Items.LEATHER)
                || stack.is(Items.RABBIT_HIDE)) {
            return 600;
        }

        // 4. Wool, carpets, wooden slabs, pressure plates, buttons, plain books
        if (stack.is(ItemTags.WOOL)
                || stack.is(ItemTags.WOOL_CARPETS)
                || stack.is(ItemTags.WOODEN_SLABS)
                || stack.is(ItemTags.WOODEN_PRESSURE_PLATES)
                || stack.is(ItemTags.WOODEN_BUTTONS)
                || stack.is(ItemTags.BANNERS)
                || stack.is(Items.BOOK)
                || stack.is(Items.WRITABLE_BOOK)
                || stack.is(Items.WRITTEN_BOOK)) {
            return 200;
        }

        // 5. Sticks, saplings, bamboo, bowls, arrows, dead bushes
        if (stack.is(Items.STICK)
                || stack.is(ItemTags.SAPLINGS)
                || stack.is(Items.BAMBOO)
                || stack.is(Items.BOWL)
                || stack.is(ItemTags.ARROWS)
                || stack.is(Items.DEAD_BUSH)
                || stack.is(Items.SCAFFOLDING)
                || stack.is(Items.PAINTING)
                || stack.is(Items.ITEM_FRAME)) {
            return 120;
        }

        // 6. Small organic/chest clutter (seeds, leaves, grass, flowers, vines, paper, feathers, string, cobweb)
        if (stack.is(ItemTags.LEAVES)
                || stack.is(ItemTags.FLOWERS)
                || stack.is(Items.WHEAT_SEEDS)
                || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.MELON_SEEDS)
                || stack.is(Items.PUMPKIN_SEEDS)
                || stack.is(Items.TORCHFLOWER_SEEDS)
                || stack.is(Items.PITCHER_POD)
                || stack.is(Items.GRASS)
                || stack.is(Items.TALL_GRASS)
                || stack.is(Items.FERN)
                || stack.is(Items.LARGE_FERN)
                || stack.is(Items.VINE)
                || stack.is(Items.SUGAR_CANE)
                || stack.is(Items.PAPER)
                || stack.is(Items.FEATHER)
                || stack.is(Items.STRING)
                || stack.is(Items.COBWEB)
                || stack.is(Items.KELP)
                || stack.is(Items.DRIED_KELP)
                || stack.is(Items.HANGING_ROOTS)
                || stack.is(Items.MOSS_CARPET)) {
            return 60;
        }

        // 7. Modded burnable items fallback
        int vanillaBurn = ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
        if (vanillaBurn > 0) {
            return Mth.clamp(Math.round(vanillaBurn * 0.70F), 60, 12000);
        }

        return 0;
    }
}
