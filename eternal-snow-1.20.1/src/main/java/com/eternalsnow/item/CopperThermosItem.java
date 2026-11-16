package com.eternalsnow.item;

import com.eternalsnow.block.CocoaPotBlockEntity;
import com.eternalsnow.registry.ModRegistry;
import com.eternalsnow.survival.PlayerSurvivalManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Insulated Copper Thermos:
 * Holds up to 6 hot servings of Cocoa or Caramel Drink scooped directly from the Cocoa Pot in 1 inventory slot!
 */
public final class CopperThermosItem extends Item {
    public static final int MAX_SIPS = 6;
    private static final String TAG_SIPS = "ThermosSips";
    private static final String TAG_DRINK = "ThermosDrink";

    public CopperThermosItem(Properties properties) {
        super(properties);
    }

    public static int getSips(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Mth.clamp(tag.getInt(TAG_SIPS), 0, MAX_SIPS);
    }

    public static int getDrinkType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? CocoaPotBlockEntity.DRINK_NONE : tag.getInt(TAG_DRINK);
    }

    public static void setContents(ItemStack stack, int drinkType, int sips) {
        int clamped = Mth.clamp(sips, 0, MAX_SIPS);
        CompoundTag tag = stack.getOrCreateTag();
        if (clamped <= 0) {
            tag.putInt(TAG_SIPS, 0);
            tag.putInt(TAG_DRINK, CocoaPotBlockEntity.DRINK_NONE);
        } else {
            tag.putInt(TAG_SIPS, clamped);
            tag.putInt(TAG_DRINK, drinkType);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getSips(stack) <= 0) {
            return InteractionResultHolder.pass(stack);
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        int sips = getSips(stack);
        int drink = getDrinkType(stack);
        if (sips > 0) {
            if (!level.isClientSide && entity instanceof ServerPlayer sp) {
                PlayerSurvivalManager.addThirstDirect(sp, 7, 4.5F);
                sp.addEffect(new MobEffectInstance(ModRegistry.COLD_IMMUNITY.get(), 400, 0, false, true, true));
                sp.addEffect(new MobEffectInstance(ModRegistry.WARM_RECOVERY.get(), 400, 0, false, true, true));
            }
            if (!(entity instanceof Player p && p.getAbilities().instabuild)) {
                setContents(stack, drink, sips - 1);
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 28;
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
    public boolean isBarVisible(ItemStack stack) {
        return getSips(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getSips(stack) / (float) MAX_SIPS);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return getDrinkType(stack) == CocoaPotBlockEntity.DRINK_CARAMEL ? 0xF0A842 : 0xA86B43;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int sips = getSips(stack);
        int drink = getDrinkType(stack);
        if (sips <= 0) {
            tooltip.add(Component.translatable("eternalsnow.tooltip.thermos.empty").withStyle(ChatFormatting.GRAY));
        } else {
            Component drinkName = Component.translatable(drink == CocoaPotBlockEntity.DRINK_CARAMEL
                    ? "eternalsnow.tooltip.thermos.caramel"
                    : "eternalsnow.tooltip.thermos.cocoa");
            tooltip.add(Component.translatable("eternalsnow.tooltip.thermos.filled", drinkName, sips, MAX_SIPS)
                    .withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("eternalsnow.tooltip.hot_cup.effects").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("eternalsnow.tooltip.thermos.how_to_fill").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("eternalsnow.tooltip.thermos.craft").withStyle(ChatFormatting.DARK_GRAY));
    }
}
