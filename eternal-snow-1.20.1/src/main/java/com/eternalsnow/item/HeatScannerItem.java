package com.eternalsnow.item;

import com.eternalsnow.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class HeatScannerItem extends Item {
    private static final String TAG_WARM_ONLY = "WarmOnlyMode";

    public HeatScannerItem(Properties properties) {
        super(properties);
    }

    public static boolean isWarmOnlyMode(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false; // Default: show ALL air blocks in radius 10!
        }
        return stack.getTag().getBoolean(TAG_WARM_ONLY);
    }

    public static boolean isHoldingScanner(Player player) {
        if (player == null) {
            return false;
        }
        return player.getMainHandItem().getItem() instanceof HeatScannerItem
                || player.getOffhandItem().getItem() instanceof HeatScannerItem;
    }

    public static ItemStack getHeldScanner(Player player) {
        if (player.getMainHandItem().getItem() instanceof HeatScannerItem) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() instanceof HeatScannerItem) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            CompoundTag tag = stack.getOrCreateTag();
            boolean nextWarmOnly = !tag.getBoolean(TAG_WARM_ONLY);
            tag.putBoolean(TAG_WARM_ONLY, nextWarmOnly);

            Component modeText = Component.translatable(
                    nextWarmOnly ? "eternalsnow.scanner.mode.warm_only" : "eternalsnow.scanner.mode.all_air"
            );
            player.displayClientMessage(modeText, true);
            if (ModRegistry.SCANNER_TOGGLE.isPresent()) {
                level.playSound(null, player.blockPosition(), ModRegistry.SCANNER_TOGGLE.get(), SoundSource.PLAYERS, 0.6F, nextWarmOnly ? 0.95F : 1.25F);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("eternalsnow.scanner.tooltip.desc").withStyle(ChatFormatting.AQUA));
        boolean warmOnly = isWarmOnlyMode(stack);
        tooltip.add(Component.translatable(
                warmOnly ? "eternalsnow.scanner.mode.warm_only" : "eternalsnow.scanner.mode.all_air"
        ).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("eternalsnow.scanner.tooltip.hint").withStyle(ChatFormatting.GRAY));
    }
}
