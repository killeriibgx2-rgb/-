package com.eternalsnow.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CaramelItem extends Item {
    private static final FoodProperties CARAMEL_FOOD = new FoodProperties.Builder()
            .nutrition(2)
            .saturationMod(0.25F)
            .fast()
            .build();

    public CaramelItem(Properties properties) {
        super(properties.food(CARAMEL_FOOD));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("eternalsnow.tooltip.caramel.use").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("eternalsnow.tooltip.caramel.smelt").withStyle(ChatFormatting.DARK_GRAY));
    }
}
