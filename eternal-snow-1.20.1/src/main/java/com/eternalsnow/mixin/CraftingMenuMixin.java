package com.eternalsnow.mixin;

import com.eternalsnow.food.FoodProfiles;
import com.eternalsnow.food.FoodState;
import com.eternalsnow.state.EternalSnowState;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * v2.7.2 — frozen food cannot be used in crafting, which until now was simply not true.
 *
 * <h2>Why the obvious hook does not work</h2>
 * {@code Ingredient.test} compares item identity only:
 * {@code for (ItemStack candidate : getItems()) if (candidate.is(stack.getItem())) return true;}
 * — no tag, no NBT. Our frozen state lives in NBT, so a frozen loaf of bread matches the bread
 * ingredient exactly like a fresh one and the recipe happily assembles. Blocking {@code test} would
 * mean a mixin on every ingredient of every recipe in the game, including other mods'.
 *
 * <p>So the gate sits where the result is actually produced. {@code slotChangedCraftingGrid} runs on
 * the server every time the grid changes (including after a craft consumes the ingredients), and it is
 * the only place that writes the result slot. We let vanilla compute it as usual and, if any grid slot
 * holds frozen food, empty the result again — with the same three calls vanilla uses at its own tail
 * (result container, remote slot, packet), so client and server never disagree.
 *
 * <h2>Static, on purpose</h2>
 * {@code slotChangedCraftingGrid} is {@code protected static}, and Mixin 0.8.5 compares staticness of
 * an {@code @Inject} handler exactly. The handler must therefore be static too — it gets everything it
 * needs from the target's own parameters, unlike the furnace case in 2.7.1 where the block entity was
 * not passed at all.
 *
 * <p>This covers the crafting table and the 2x2 player grid (including modded recipes).
 * Custom machines that bypass these menus need their own integration; block interaction is
 * deliberately allowed so containers and shelves remain usable with frozen items in hand.
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
    private static void eternalsnow$refuseFrozenIngredients(AbstractContainerMenu menu, Level level,
                                                            net.minecraft.world.entity.player.Player player,
                                                            CraftingContainer grid,
                                                            ResultContainer result,
                                                            CallbackInfo ci) {
        if (result.getItem(0).isEmpty()) {
            return;
        }
        if (!EternalSnowState.getServerSettings().enableFoodFreezing) {
            return;
        }

        boolean frozen = false;
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack inGrid = grid.getItem(i);
            if (!inGrid.isEmpty()
                    && FoodState.isFrozen(inGrid)
                    && FoodProfiles.profileOf(inGrid) >= 0) {
                frozen = true;
                break;
            }
        }
        if (!frozen) {
            return;
        }

        // Mirror vanilla's own tail exactly, otherwise the client keeps showing the ghost result and
        // the player clicks a slot the server considers empty.
        result.setItem(0, ItemStack.EMPTY);
        menu.setRemoteSlot(0, ItemStack.EMPTY);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                    menu.containerId, menu.incrementStateId(), 0, ItemStack.EMPTY));
        }
    }
}
