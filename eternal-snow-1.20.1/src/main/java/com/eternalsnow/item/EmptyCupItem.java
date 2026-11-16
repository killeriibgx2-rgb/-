package com.eternalsnow.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class EmptyCupItem extends Item {
    public EmptyCupItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("eternalsnow.tooltip.empty_cup.use").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("eternalsnow.tooltip.empty_cup.craft").withStyle(ChatFormatting.DARK_GRAY));
    }
}
