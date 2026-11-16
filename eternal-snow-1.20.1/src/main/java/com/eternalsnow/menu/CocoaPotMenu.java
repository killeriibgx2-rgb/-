package com.eternalsnow.menu;

import com.eternalsnow.block.CocoaPotBlockEntity;
import com.eternalsnow.registry.ModRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CocoaPotMenu extends AbstractContainerMenu {
    private final Container potContainer;
    private final ContainerData data;

    // Client constructor via IForgeMenuType
    public CocoaPotMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(
                containerId,
                playerInv,
                extraData != null && playerInv.player.level().getBlockEntity(extraData.readBlockPos()) instanceof CocoaPotBlockEntity be
                        ? be
                        : new SimpleContainer(3),
                new SimpleContainerData(4)
        );
    }

    public CocoaPotMenu(int containerId, Inventory playerInv, Container potContainer, ContainerData data) {
        super(ModRegistry.COCOA_POT_MENU.get(), containerId);
        checkContainerSize(potContainer, 3);
        checkContainerDataCount(data, 4);
        this.potContainer = potContainer;
        this.data = data;

        // Slot 0: Milk Bucket (or Water Bucket)
        this.addSlot(new Slot(potContainer, 0, 26, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.MILK_BUCKET) || stack.is(Items.WATER_BUCKET);
            }
        });

        // Slot 1: Water Bucket (or Milk Bucket)
        this.addSlot(new Slot(potContainer, 1, 50, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.WATER_BUCKET) || stack.is(Items.MILK_BUCKET);
            }
        });

        // Slot 2: 4x Cocoa Beans OR 4x Caramel
        this.addSlot(new Slot(potContainer, 2, 74, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.COCOA_BEANS)
                        || (ModRegistry.CARAMEL.isPresent() && stack.is(ModRegistry.CARAMEL.get()));
            }
        });

        // Player Inventory (3 rows x 9)
        int invStartY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, invStartY + row * 18));
            }
        }

        // Player Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, invStartY + 58));
        }

        this.addDataSlots(data);
    }

    public int getDrinkType() {
        return this.data.get(0);
    }

    public int getServings() {
        return this.data.get(1);
    }

    public int getCookProgress() {
        return this.data.get(2);
    }

    public boolean isHeatedBelow() {
        return this.data.get(3) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack raw = slot.getItem();
            result = raw.copy();

            if (index < 3) {
                if (!this.moveItemStackTo(raw, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (raw.is(Items.MILK_BUCKET)) {
                    if (!this.moveItemStackTo(raw, 0, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (raw.is(Items.WATER_BUCKET)) {
                    if (!this.moveItemStackTo(raw, 0, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (raw.is(Items.COCOA_BEANS) || (ModRegistry.CARAMEL.isPresent() && raw.is(ModRegistry.CARAMEL.get()))) {
                    if (!this.moveItemStackTo(raw, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 30) {
                    if (!this.moveItemStackTo(raw, 30, 39, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(raw, 3, 30, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (raw.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.potContainer.stillValid(player);
    }
}
