package com.eternalsnow.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v2.6.6: knitted wool armour. Poor protection, but the warmest thing you can wear — it counts as
 * "naturally warm" for the insulation maths, so a full set beats plain leather on cold nights.
 */
public class WoolArmorItem extends ArmorItem {
    public WoolArmorItem(ArmorMaterial material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("eternalsnow.tooltip.wool_armor").withStyle(ChatFormatting.AQUA));
    }
}
