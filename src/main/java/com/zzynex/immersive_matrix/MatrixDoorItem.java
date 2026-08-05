package com.zzynex.immersive_matrix;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class MatrixDoorItem extends DoubleHighBlockItem {

    public MatrixDoorItem(Block block, Properties properties) {
        super(block, properties);
    }

    // Срабатывает каждый тик, пока предмет лежит в инвентаре игрока
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!level.isClientSide && entity instanceof Player player) {
            // Если у предмета ещё нет кастомного имени, принудительно переписываем его под ник текущего игрока
            if (!stack.hasCustomHoverName()) {
                stack.setHoverName(Component.literal("ДВЕРЬ " + player.getGameProfile().getName().toUpperCase()));
            }
        }
    }
}
