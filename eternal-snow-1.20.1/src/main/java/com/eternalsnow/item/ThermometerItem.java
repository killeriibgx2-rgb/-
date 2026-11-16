package com.eternalsnow.item;

import com.eternalsnow.registry.ModRegistry;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.thermal.ThermalEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.Locale;

public class ThermometerItem extends Item {
    public ThermometerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer sp) {
            float hereC = ThermalEngine.getLocalTemperatureC(serverLevel, sp.blockPosition().above());
            float streetC = EternalSnowState.worldAmbientTempC;
            float diff = hereC - streetC;

            String hereStr = String.format(Locale.ROOT, "%+.1f°C", hereC);
            String streetStr = String.format(Locale.ROOT, "%+.1f°C", streetC);
            String diffStr = String.format(Locale.ROOT, "%+.1f°C", diff);

            sp.displayClientMessage(
                    Component.translatable("eternalsnow.thermometer.chat_readout", hereStr, streetStr, diffStr),
                    true
            );
            if (ModRegistry.SCANNER_TOGGLE.isPresent()) {
                serverLevel.playSound(null, sp.blockPosition(), ModRegistry.SCANNER_TOGGLE.get(), SoundSource.PLAYERS, 0.55F, 1.2F);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("eternalsnow.thermometer.tooltip.desc").withStyle(ChatFormatting.GRAY));
        if (level != null && level.isClientSide) {
            String hereStr = String.format(Locale.ROOT, "%+.1f°C", EternalSnowState.clientLocalTempC);
            String streetStr = String.format(Locale.ROOT, "%+.1f°C", EternalSnowState.worldAmbientTempC);
            tooltip.add(Component.translatable("eternalsnow.thermometer.tooltip.here", hereStr).withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("eternalsnow.thermometer.tooltip.street", streetStr).withStyle(ChatFormatting.AQUA));
        }
    }
}
