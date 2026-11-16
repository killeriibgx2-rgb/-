package com.eternalsnow.recipe;

import com.eternalsnow.registry.ModRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.Set;

/**
 * Allows crafting Rainbow Candy Cane with the 4-Caramel cane shape + ANY 3 different dyes in the remaining slots!
 */
public final class RainbowCandyRecipe extends CustomRecipe {

    public RainbowCandyRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (container.getWidth() < 3 || container.getHeight() < 3) {
            return false;
        }
        ItemStack caramelItem = new ItemStack(ModRegistry.CARAMEL.get());
        // Cane shape in center+right columns: (1,0), (2,0), (1,1), (1,2)
        int[][] caneCoords = {{1, 0}, {2, 0}, {1, 1}, {1, 2}};
        boolean[][] isCane = new boolean[3][3];
        for (int[] c : caneCoords) {
            ItemStack st = container.getItem(c[0] + c[1] * container.getWidth());
            if (!ItemStack.isSameItem(st, caramelItem)) {
                return false;
            }
            isCane[c[0]][c[1]] = true;
        }

        int dyeCount = 0;
        Set<DyeColor> distinctColors = EnumSet.noneOf(DyeColor.class);

        for (int y = 0; y < container.getHeight(); y++) {
            for (int x = 0; x < container.getWidth(); x++) {
                if (x < 3 && y < 3 && isCane[x][y]) {
                    continue;
                }
                ItemStack st = container.getItem(x + y * container.getWidth());
                if (st.isEmpty()) {
                    continue;
                }
                if (st.getItem() instanceof DyeItem dye) {
                    dyeCount++;
                    distinctColors.add(dye.getDyeColor());
                } else {
                    return false;
                }
            }
        }
        return dyeCount == 3 && distinctColors.size() >= 3;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        return new ItemStack(ModRegistry.CANDY_RAINBOW.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.RAINBOW_CANDY_SERIALIZER.get();
    }
}
