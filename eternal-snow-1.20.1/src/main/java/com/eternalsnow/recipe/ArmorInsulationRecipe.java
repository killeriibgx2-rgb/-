package com.eternalsnow.recipe;

import com.eternalsnow.registry.ModRegistry;
import com.eternalsnow.survival.ArmorInsulationHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class ArmorInsulationRecipe extends CustomRecipe {

    public ArmorInsulationRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (container.getWidth() < 3 || container.getHeight() < 3) {
            return false;
        }
        // Center of 3x3 grid must be an ArmorItem with insulation tier < 3, surrounded by 8 Leather!
        ItemStack center = container.getItem(1 + 1 * container.getWidth());
        if (!ArmorInsulationHelper.isArmor(center)) {
            return false;
        }
        if (ArmorInsulationHelper.getInsulationTier(center) >= ArmorInsulationHelper.MAX_INSULATION_LEVEL) {
            return false;
        }

        for (int y = 0; y < container.getHeight(); y++) {
            for (int x = 0; x < container.getWidth(); x++) {
                ItemStack slot = container.getItem(x + y * container.getWidth());
                if (x == 1 && y == 1) {
                    continue;
                }
                if (x < 3 && y < 3) {
                    if (!slot.is(Items.LEATHER)) {
                        return false;
                    }
                } else if (!slot.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack center = container.getItem(1 + 1 * container.getWidth());
        if (!ArmorInsulationHelper.isArmor(center)) {
            return ItemStack.EMPTY;
        }
        int currentTier = ArmorInsulationHelper.getInsulationTier(center);
        if (currentTier >= ArmorInsulationHelper.MAX_INSULATION_LEVEL) {
            return ItemStack.EMPTY;
        }
        ItemStack upgraded = center.copyWithCount(1);
        ArmorInsulationHelper.setInsulationTier(upgraded, currentTier + 1);
        return upgraded;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.ARMOR_INSULATION_SERIALIZER.get();
    }
}
