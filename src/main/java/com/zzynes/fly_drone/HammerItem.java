package com.zzynes.fly_drone;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class HammerItem extends Item {
    public HammerItem(Properties properties) {
        // Устанавливаем максимальную прочность 300, как в ТЗ
        super(properties.durability(300).setNoRepair());
    }

    // Эта магия Forge заставляет предмет оставаться в верстаке после крафта
    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    // А этот метод говорит игре, ЧТО именно вернуть в сетку (копию молота с уроном +1)
    @Override
    public ItemStack getCraftingRemainingItem(ItemStack itemStack) {
        ItemStack copy = itemStack.copy();
        copy.setDamageValue(itemStack.getDamageValue() + 1);

        // Если молот полностью сломался (урон равен или больше 300), он исчезнет
        if (copy.getDamageValue() >= copy.getMaxDamage()) {
            return ItemStack.EMPTY;
        }

        return copy;
    }
}
