package com.eternalsnow.item;

import com.eternalsnow.thermal.ThermalEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * v2.6.0: survival-craftable thermometer. Same texture as the admin one, but it only reports
 * the temperature of the block the player is standing in — no global "street" readout.
 */
public class LocalThermometerItem extends Item {
    public LocalThermometerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer sp) {
            float hereC = ThermalEngine.getLocalTemperatureC(serverLevel, sp.blockPosition().above());
            String hereStr = String.format(Locale.ROOT, "%+.1f°C", hereC);
            sp.displayClientMessage(Component.translatable("eternalsnow.local_thermometer.readout", hereStr), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("eternalsnow.local_thermometer.tooltip.desc").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("eternalsnow.local_thermometer.tooltip.craft").withStyle(ChatFormatting.DARK_GRAY));
        if (level != null && level.isClientSide) {
            String hereStr = String.format(Locale.ROOT, "%+.1f°C", com.eternalsnow.state.EternalSnowState.clientLocalTempC);
            tooltip.add(Component.translatable("eternalsnow.local_thermometer.tooltip.here", hereStr).withStyle(ChatFormatting.YELLOW));
        }
    }
}
