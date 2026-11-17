package com.eternalsnow.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * v2.7 — five item hooks, one per {@link FoodShelfBlock#HOOKS} slot.
 *
 * <p>Deliberately <em>not</em> a {@link net.minecraft.world.Container}: no hoppers, no automation, no
 * GUI. It exists so food can hang in the open air and age there.
 */
public final class FoodShelfBlockEntity extends BlockEntity {

    private final NonNullList<ItemStack> hooks =
            NonNullList.withSize(FoodShelfBlock.HOOKS, ItemStack.EMPTY);

    public FoodShelfBlockEntity(BlockPos pos, BlockState state) {
        super(com.eternalsnow.registry.ModRegistry.FOOD_SHELF_BE.get(), pos, state);
    }

    public ItemStack getHook(int hook) {
        return hook >= 0 && hook < this.hooks.size() ? this.hooks.get(hook) : ItemStack.EMPTY;
    }

    public void setHook(int hook, ItemStack stack) {
        if (hook < 0 || hook >= this.hooks.size()) {
            return;
        }
        this.hooks.set(hook, stack);
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            // Renderer needs the new stack immediately; without this the food appears only after a
            // chunk reload.
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public boolean isEmpty() {
        for (ItemStack stack : this.hooks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Called when the shelf is broken: everything hanging on it lands on the floor. */
    public void dropContents() {
        if (this.level == null) {
            return;
        }
        for (ItemStack stack : this.hooks) {
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(this.level,
                        this.worldPosition.getX() + 0.5D,
                        this.worldPosition.getY() + 0.5D,
                        this.worldPosition.getZ() + 0.5D,
                        stack);
            }
        }
        this.hooks.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.hooks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.hooks.clear();
        ContainerHelper.loadAllItems(tag, this.hooks);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, this.hooks);
        return tag;
    }
}
