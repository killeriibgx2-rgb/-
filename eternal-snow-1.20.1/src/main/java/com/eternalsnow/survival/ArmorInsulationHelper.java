package com.eternalsnow.survival;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;

public final class ArmorInsulationHelper {
    public static final String TAG_INSULATION_LEVEL = "EternalSnowInsulation";
    public static final int MAX_INSULATION_LEVEL = 3;

    private ArmorInsulationHelper() {
    }

    public static boolean isArmor(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ArmorItem;
    }

    public static boolean isNaturallyWarmLeather(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem armor)) {
            return false;
        }
        if (armor.getMaterial() == ArmorMaterials.LEATHER) {
            return true;
        }
        String matName = armor.getMaterial().getName();
        return matName != null && matName.toLowerCase().contains("leather");
    }

    public static int getInsulationTier(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return 0;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_INSULATION_LEVEL)) {
            return 0;
        }
        return Mth.clamp(tag.getInt(TAG_INSULATION_LEVEL), 0, MAX_INSULATION_LEVEL);
    }

    public static void setInsulationTier(ItemStack stack, int tier) {
        stack.getOrCreateTag().putInt(TAG_INSULATION_LEVEL, Mth.clamp(tier, 0, MAX_INSULATION_LEVEL));
    }

    /**
     * Warmth points for a single armor piece.
     * Uninsulated metal/diamond/netherite = 0.0 (cold metal does not protect!).
     * Plain leather piece = 1.25 (full set = 5.0).
     * Each crafted insulation tier (I, II, III) adds +1.50 per piece (full Tier III set = +18.0, or 23.0 on leather).
     */
    public static float getPieceWarmthPoints(ItemStack stack, float globalMult) {
        if (!isArmor(stack) || globalMult <= 0.001F) {
            return 0.0F;
        }
        float base = isNaturallyWarmLeather(stack) ? 1.25F : 0.0F;
        int tier = getInsulationTier(stack);
        float tierBonus = tier * 1.50F;
        return (base + tierBonus) * globalMult;
    }

    public static float getTotalPlayerWarmthPoints(Player player, float globalMult) {
        if (player == null || globalMult <= 0.001F) {
            return 0.0F;
        }
        float sum = 0.0F;
        for (ItemStack armorStack : player.getArmorSlots()) {
            sum += getPieceWarmthPoints(armorStack, globalMult);
        }
        return sum;
    }

    /**
     * Computes fraction [0.0 .. 0.76] by which heat loss speed is reduced.
     * Capped at 76% so even maxed Tier III armor CANNOT make a player immortal to freezing forever without shelter/fire!
     */
    public static float computeColdDrainReduction(float totalWarmthPoints) {
        if (totalWarmthPoints <= 0.0F) {
            return 0.0F;
        }
        // Smooth saturating curve: 5 pts (leather) -> ~25% slower freeze; 18 pts (iron Tier III) -> ~64%; 23 pts (leather Tier III) -> ~70%
        return Mth.clamp(totalWarmthPoints / (totalWarmthPoints + 10.0F), 0.0F, 0.76F);
    }

    /**
     * Multiplier [0.24 .. 1.00] applied to cold hypothermia drain rate.
     */
    public static float getColdDrainMultiplier(Player player, float globalMult) {
        float pts = getTotalPlayerWarmthPoints(player, globalMult);
        return 1.0F - computeColdDrainReduction(pts);
    }
}
