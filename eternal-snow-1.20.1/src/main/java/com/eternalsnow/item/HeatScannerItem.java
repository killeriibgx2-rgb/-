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
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.List;

public class HeatScannerItem extends Item {
    private static final String TAG_WARM_ONLY = "WarmOnlyMode";
    private static final String TAG_MODE = "ScanMode";

    // 0 = all air, 1 = warm air only, 2 = wall outline (relative palette), 3 = full-screen thermal
    public static final int MODE_ALL_AIR = 0;
    public static final int MODE_WARM_ONLY = 1;
    public static final int MODE_WALLS = 2;
    public static final int MODE_THERMAL = 3;
    public static final int MODE_COUNT = 4;

    public HeatScannerItem(Properties properties) {
        super(properties);
    }

    public static int getMode(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return MODE_ALL_AIR;
        }
        CompoundTag tag = stack.getTag();
        if (tag.contains(TAG_MODE)) {
            return Mth.clamp(tag.getInt(TAG_MODE), 0, MODE_COUNT - 1);
        }
        // Backwards compatibility with v2.5.x stacks
        return tag.getBoolean(TAG_WARM_ONLY) ? MODE_WARM_ONLY : MODE_ALL_AIR;
    }

    public static boolean isWarmOnlyMode(ItemStack stack) {
        return getMode(stack) == MODE_WARM_ONLY;
    }

    public static boolean isWallMode(ItemStack stack) {
        int m = getMode(stack);
        return m == MODE_WALLS || m == MODE_THERMAL;
    }

    public static String getModeTranslationKey(int mode) {
        return switch (mode) {
            case MODE_WARM_ONLY -> "eternalsnow.scanner.mode.warm_only";
            case MODE_WALLS -> "eternalsnow.scanner.mode.walls";
            case MODE_THERMAL -> "eternalsnow.scanner.mode.thermal";
            default -> "eternalsnow.scanner.mode.all_air";
        };
    }

    public static String getModeHudTranslationKey(int mode) {
        return switch (mode) {
            case MODE_WARM_ONLY -> "eternalsnow.scanner.hud.warm_only";
            case MODE_WALLS -> "eternalsnow.scanner.hud.walls";
            case MODE_THERMAL -> "eternalsnow.scanner.hud.thermal";
            default -> "eternalsnow.scanner.hud.all_air";
        };
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
            int next = (getMode(stack) + 1) % MODE_COUNT;
            tag.putInt(TAG_MODE, next);
            tag.putBoolean(TAG_WARM_ONLY, next == MODE_WARM_ONLY);

            player.displayClientMessage(Component.translatable(getModeTranslationKey(next)), true);
            if (ModRegistry.SCANNER_TOGGLE.isPresent()) {
                level.playSound(null, player.blockPosition(), ModRegistry.SCANNER_TOGGLE.get(),
                        SoundSource.PLAYERS, 0.6F, 0.90F + next * 0.12F);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("eternalsnow.scanner.tooltip.desc").withStyle(ChatFormatting.AQUA));
        int mode = getMode(stack);
        tooltip.add(Component.translatable(getModeTranslationKey(mode)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("eternalsnow.scanner.tooltip.hint").withStyle(ChatFormatting.GRAY));
    }
}
