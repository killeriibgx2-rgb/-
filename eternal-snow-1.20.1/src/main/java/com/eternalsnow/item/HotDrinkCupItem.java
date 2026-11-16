package com.eternalsnow.item;

import com.eternalsnow.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class HotDrinkCupItem extends Item {
    public static final int DRINK_EFFECT_TICKS = 20 * 20; // 20s Cold Immunity + 20s gradual Warmth Recovery!
    private final boolean isCaramel;

    public HotDrinkCupItem(Properties properties, boolean isCaramel) {
        super(properties.stacksTo(16).food(new FoodProperties.Builder().nutrition(3).saturationMod(0.4F).alwaysEat().build()));
        this.isCaramel = isCaramel;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        super.finishUsingItem(stack, level, entity);

        if (!level.isClientSide) {
            // Exact design: 20s Cold Immunity (stops freezing immediately) + 20s slow Warmth Recovery!
            if (ModRegistry.COLD_IMMUNITY.isPresent()) {
                entity.addEffect(new MobEffectInstance(ModRegistry.COLD_IMMUNITY.get(), DRINK_EFFECT_TICKS, 0, false, true, true));
            }
            if (ModRegistry.WARM_RECOVERY.isPresent()) {
                entity.addEffect(new MobEffectInstance(ModRegistry.WARM_RECOVERY.get(), DRINK_EFFECT_TICKS, 0, false, true, true));
            }
        }

        if (entity instanceof Player player) {
            if (player.getAbilities().instabuild) {
                return stack;
            }
            ItemStack emptyCup = new ItemStack(ModRegistry.EMPTY_CUP.get());
            if (stack.isEmpty()) {
                return emptyCup;
            }
            if (!player.getInventory().add(emptyCup)) {
                player.drop(emptyCup, false);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String descKey = this.isCaramel
                ? "eternalsnow.tooltip.hot_cup.caramel_desc"
                : "eternalsnow.tooltip.hot_cup.cocoa_desc";
        tooltip.add(Component.translatable(descKey).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("eternalsnow.tooltip.hot_cup.effects").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("eternalsnow.tooltip.hot_cup.return_cup").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("eternalsnow.tooltip.hot_cup.how_to_brew").withStyle(ChatFormatting.DARK_GRAY));
    }
}
