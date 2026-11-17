package com.eternalsnow.food;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Synchronise compatible stacks AFTER a container scan. No global ItemStack equality patch:
 * other mods' tags and capabilities still participate in vanilla comparison. We never mix
 * different spoilage stages or displayed temperatures. Within a group the oldest progress
 * wins, so adding fresh food cannot rejuvenate an old stack. Slot counts never change.
 */
public final class FoodStackNormalizer {
    private FoodStackNormalizer() {}

    private static ItemStack withoutState(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.removeTagKey(FoodState.TAG_ROOT);
        if (copy.hasTag() && copy.getTag().isEmpty()) copy.setTag(null);
        return copy;
    }

    public static boolean normalize(Container container) {
        boolean changed = false;
        boolean[] seen = new boolean[container.getContainerSize()];
        for (int i = 0; i < seen.length; i++) {
            ItemStack first = container.getItem(i);
            if (seen[i] || !FoodState.hasState(first)) continue;
            ItemStack identity = withoutState(first);
            CompoundTag canonical = first.getTag().getCompound(FoodState.TAG_ROOT).copy();
            java.util.List<Integer> group = new java.util.ArrayList<>();
            for (int j = i; j < seen.length; j++) {
                ItemStack next = container.getItem(j);
                if (seen[j] || !FoodState.hasState(next)
                        || FoodState.tempC(first, 0) != FoodState.tempC(next, 0)
                        || FoodState.spoil(first) != FoodState.spoil(next)
                        || !ItemStack.isSameItemSameTags(identity, withoutState(next))) continue;
                seen[j] = true;
                group.add(j);
                CompoundTag state = next.getTag().getCompound(FoodState.TAG_ROOT);
                if (state.getInt("p") * 1000 + state.getInt("pr")
                        > canonical.getInt("p") * 1000 + canonical.getInt("pr")) canonical = state.copy();
            }
            for (int slot : group) {
                ItemStack current = container.getItem(slot);
                if (!canonical.equals(current.getTag().getCompound(FoodState.TAG_ROOT))) {
                    ItemStack copy = current.copy();
                    copy.getOrCreateTag().put(FoodState.TAG_ROOT, canonical.copy());
                    container.setItem(slot, copy);
                    changed = true;
                }
            }
        }
        return changed;
    }
}
