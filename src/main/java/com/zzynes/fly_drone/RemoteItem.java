package com.zzynes.fly_drone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.UUID;

public class RemoteItem extends Item {
    public RemoteItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof FPVDroneEntity && player.isShiftKeyDown()) {
            if (!player.level().isClientSide) {
                CompoundTag tag = stack.getOrCreateTag();
                tag.putUUID("ConnectedDroneUUID", target.getUUID());
                player.sendSystemMessage(Component.literal("§aПульт успешно подключен к дрону! ID записан."));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = stack.getTag();

        if (tag != null && tag.hasUUID("ConnectedDroneUUID")) {
            // Если мы находимся на КЛИЕНТЕ, отправляем сетевой пакет на сервер
            if (level.isClientSide) {
                UUID droneUuid = tag.getUUID("ConnectedDroneUUID");
                // ОТПРАВЛЯЕМ ПАКЕТ ПО СЕТИ С UUID НАШЕГО ДРОНА
                ModMessages.sendToServer(new ToggleFlightPacket(droneUuid));
            } else {
                player.sendSystemMessage(Component.literal("§eПодключение к беспилотнику..."));
            }
            return InteractionResultHolder.success(stack);
        } else {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.literal("§cОшибка: Пульт не привязан к дрону! Зажмите Shift+ЛКМ по дрону."));
            }
            return InteractionResultHolder.fail(stack);
        }
    }
}
