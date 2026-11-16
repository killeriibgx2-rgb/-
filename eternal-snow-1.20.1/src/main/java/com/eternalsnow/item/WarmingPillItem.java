package com.eternalsnow.item;

import com.eternalsnow.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class WarmingPillItem extends Item {
    public static final int IMMUNITY_TICKS = 20 * 60 * 10; // 10 minutes!

    public WarmingPillItem(Properties properties) {
        super(properties.food(new FoodProperties.Builder().nutrition(0).saturationMod(0.0F).alwaysEat().fast().build()));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 18;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && ModRegistry.COLD_IMMUNITY.isPresent()) {
            entity.addEffect(new MobEffectInstance(ModRegistry.COLD_IMMUNITY.get(), IMMUNITY_TICKS, 0, false, true, true));
            level.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.15F);
        }
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("eternalsnow.tooltip.warming_pill.effect").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("eternalsnow.tooltip.warming_pill.starter").withStyle(ChatFormatting.GRAY));
    }
}
