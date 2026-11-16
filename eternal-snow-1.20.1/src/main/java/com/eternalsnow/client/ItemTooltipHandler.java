package com.eternalsnow.client;

import com.eternalsnow.registry.ModRegistry;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.survival.ArmorInsulationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Locale;

public final class ItemTooltipHandler {
    private ItemTooltipHandler() {
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        List<Component> tooltip = event.getToolTip();

        // 1. Armor Cold-Protection & 3-Tier Leather Insulation Tooltips
        if (ArmorInsulationHelper.isArmor(stack)) {
            int tier = ArmorInsulationHelper.getInsulationTier(stack);
            boolean isLeather = ArmorInsulationHelper.isNaturallyWarmLeather(stack);
            float mult = EternalSnowState.getEffectiveSettings().armorInsulationMultiplier;
            float pts = ArmorInsulationHelper.getPieceWarmthPoints(stack, mult);

            if (tier > 0) {
                String roman = switch (tier) {
                    case 1 -> "I";
                    case 2 -> "II";
                    default -> "III (МАКС)";
                };
                tooltip.add(Component.translatable("eternalsnow.tooltip.armor.insulated", roman, String.format(Locale.ROOT, "%.1f", pts))
                        .withStyle(ChatFormatting.AQUA));
            } else if (isLeather) {
                tooltip.add(Component.translatable("eternalsnow.tooltip.armor.leather_base", String.format(Locale.ROOT, "%.1f", pts))
                        .withStyle(ChatFormatting.GOLD));
            } else {
                tooltip.add(Component.translatable("eternalsnow.tooltip.armor.cold_metal")
                        .withStyle(ChatFormatting.BLUE));
            }

            if (tier < ArmorInsulationHelper.MAX_INSULATION_LEVEL) {
                tooltip.add(Component.translatable("eternalsnow.tooltip.armor.craft_hint", tier + 1)
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            return;
        }

        // 2. Survival Crafting Hints on Existing Mod Items & Vanilla Sugar
        if (ModRegistry.THERMOMETER.isPresent() && stack.is(ModRegistry.THERMOMETER.get())) {
            tooltip.add(Component.translatable("eternalsnow.tooltip.thermometer.craft").withStyle(ChatFormatting.DARK_GRAY));
        } else if (ModRegistry.HEAT_SCANNER.isPresent() && stack.is(ModRegistry.HEAT_SCANNER.get())) {
            tooltip.add(Component.translatable("eternalsnow.tooltip.heat_scanner.craft").withStyle(ChatFormatting.DARK_GRAY));
        } else if (ModRegistry.SNOW_SHOVEL.isPresent() && stack.is(ModRegistry.SNOW_SHOVEL.get())) {
            tooltip.add(Component.translatable("eternalsnow.tooltip.snow_shovel.craft").withStyle(ChatFormatting.DARK_GRAY));
        } else if (ModRegistry.FESTIVE_COPPER_GOLEM_SPAWN_EGG.isPresent() && stack.is(ModRegistry.FESTIVE_COPPER_GOLEM_SPAWN_EGG.get())) {
            tooltip.add(Component.translatable("eternalsnow.tooltip.golem.build_hint").withStyle(ChatFormatting.YELLOW));
        } else if (stack.is(Items.SUGAR)) {
            tooltip.add(Component.translatable("eternalsnow.tooltip.sugar.caramel_hint").withStyle(ChatFormatting.DARK_GRAY));
        } else if (stack.is(Items.CAMPFIRE) || stack.is(Items.SOUL_CAMPFIRE)) {
            tooltip.add(Component.translatable("eternalsnow.tooltip.campfire.sticks_hint").withStyle(ChatFormatting.GOLD));
        } else if (stack.is(Items.STICK)) {
            tooltip.add(Component.translatable("eternalsnow.tooltip.stick.campfire_fuel").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
