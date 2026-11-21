package com.eternalsnow.item;

import com.eternalsnow.config.ThermalConfig;
import com.eternalsnow.thermal.world.ThermalWorldEvents;
import com.eternalsnow.thermal.world.ZoneManager;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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

/**
 * The debug panel as an item: right-click reads the thermal state around you.
 *
 * <p>Temporary by intent — the user plans to delete it before release. It
 * exists so the physics can be inspected in game instead of inferred from chat
 * messages.
 */
public final class ThermalScannerItem extends Item {
    public ThermalScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel
                && player instanceof ServerPlayer serverPlayer) {
            report(serverLevel, serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void report(ServerLevel level, ServerPlayer player) {
        ZoneManager manager = ThermalWorldEvents.manager(level);
        BlockPos pos = player.blockPosition();
        double ambient = ThermalWorldEvents.ambientField().celsiusAt(level);
        double air = manager.airCelsiusAt(pos, ambient);

        player.displayClientMessage(Component.literal("— Eternal Snow —")
                .withStyle(ChatFormatting.GOLD), false);
        player.displayClientMessage(Component.literal(String.format(
                "Снаружи: %.1f °C", ambient)), false);
        player.displayClientMessage(Component.literal(String.format(
                "Воздух здесь: %.1f °C", air)), false);
        player.displayClientMessage(Component.literal(String.format(
                "Скорость физики: x%.0f", ThermalConfig.SPEED_MULTIPLIER.get())), false);
        player.displayClientMessage(Component.literal(String.format(
                "Зон: %d, радиус %d", manager.zones().size(),
                ThermalConfig.ZONE_RADIUS.get())), false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("ПКМ: состояние тепла").withStyle(ChatFormatting.GRAY));
    }
}
