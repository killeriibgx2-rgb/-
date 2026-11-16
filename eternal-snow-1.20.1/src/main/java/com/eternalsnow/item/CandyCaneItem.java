package com.eternalsnow.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CandyCaneItem extends Item {
    private final String craftHintKey;
    private final boolean rainbowBoost;

    public CandyCaneItem(Properties properties, int nutrition, float saturation, String craftHintKey, boolean rainbowBoost) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationMod(saturation)
                .alwaysEat()
                .fast()
                .build()));
        this.craftHintKey = craftHintKey;
        this.rainbowBoost = rainbowBoost;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            if (this.rainbowBoost) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 12, 1, false, true, true));
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 5, 0, false, true, true));
            } else {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 8, 0, false, true, true));
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (this.rainbowBoost) {
            tooltip.add(Component.translatable("eternalsnow.tooltip.candy.rainbow_effect").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            tooltip.add(Component.translatable("eternalsnow.tooltip.candy.effect").withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable(this.craftHintKey).withStyle(ChatFormatting.DARK_GRAY));
    }
}
