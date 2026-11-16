package com.eternalsnow.enchantment;

import com.eternalsnow.item.SnowShovelItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class SnowShovelEnchantment extends Enchantment {
    public static final EnchantmentCategory SNOW_SHOVEL_CATEGORY = EnchantmentCategory.create(
            "eternalsnow_shovel",
            item -> item instanceof SnowShovelItem
    );

    public SnowShovelEnchantment() {
        super(Rarity.UNCOMMON, SNOW_SHOVEL_CATEGORY, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 6 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 25;
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof SnowShovelItem || super.canEnchant(stack);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return stack.getItem() instanceof SnowShovelItem;
    }
}
