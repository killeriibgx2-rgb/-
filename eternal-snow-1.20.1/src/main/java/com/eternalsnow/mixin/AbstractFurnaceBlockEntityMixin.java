package com.eternalsnow.mixin;

import com.eternalsnow.food.FoodProfiles;
import com.eternalsnow.food.FoodState;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * v2.7 — food gets its own rhythm inside a furnace: one item at a time, and a FROZEN item must thaw
 * before it can cook.
 *
 * <p>The thaw is not a second state machine: it is folded into {@code cookingTotalTime}, which vanilla
 * streams to the client as {@code ContainerData} slot 3. The result is that the vanilla arrow advances
 * at {@code cook / (cook + thaw)} of its speed — slow, honest, and with no numbers on screen, exactly
 * as requested.
 *
 * <p>The stack keeps its frozen tag while it thaws, so the player can pull it out mid-way and it stays
 * frozen. Once the recipe completes, the cooked item is written back INTO the food slot (there is no
 * food result slot), and everything that is not food keeps vanilla behaviour.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    /**
     * One portion at a time: food cannot be stacked into the furnace slot. Hopper automation therefore
     * feeds it one item per cycle instead of dumping a whole stack.
     */
    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void eternalsnow$singlePortionFood(int slot, ItemStack stack,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (slot != 0 || stack.isEmpty() || FoodProfiles.profileOf(stack) < 0) {
            return;
        }
        if (stack.getCount() > 1) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Folds the thaw into the total cook time at the exact point vanilla asks for it.
     *
     * <p>{@code setItem} is the only place that assigns {@code cookingTotalTime} when a fresh stack
     * arrives, and it does so through this private static helper. Redirecting the CALL means the value
     * is computed from the recipe plus the thaw every time, so it can never accumulate on repeated
     * clicks (an "add at TAIL" hook would compound, which is why this is a redirect and not an inject).
     *
     * <p>Because the furnace streams {@code cookingTotalTime} to the client as {@code ContainerData}
     * slot 3, the vanilla progress arrow simply advances slower — no custom GUI, no numbers on screen.
     */
    @Redirect(method = "setItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getTotalCookTime(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;)I"))
    private int eternalsnow$thawAwareCookTime(net.minecraft.world.level.Level level,
                                              AbstractFurnaceBlockEntity furnace) {
        int vanilla = AbstractFurnaceBlockEntity.getTotalCookTime(level, furnace);
        ItemStack input = furnace.getItem(0);
        if (input.isEmpty() || !FoodState.isFrozen(input)) {
            return vanilla;
        }
        float furnaceC = level instanceof ServerLevel serverLevel
                ? com.eternalsnow.thermal.ThermalEngine.getLocalTemperatureC(serverLevel, furnace.getBlockPos())
                : FoodState.THAWED_AT_C + FoodState.TEMP_STEP;
        int thaw = FoodState.ticksToThaw(input, furnaceC);
        if (thaw == Integer.MAX_VALUE) {
            thaw = 20 * 60 * 5; // this oven cannot thaw it: stall rather than cook it frozen
        }
        return Math.min(vanilla + thaw, 20 * 60 * 10);
    }

    /**
     * Food is cooked IN PLACE. Vanilla puts the result into slot 2 and shrinks the input; for edible
     * items the cooked stack goes straight back into the food slot instead, so the furnace shows one
     * slot changing state (raw → thawed → cooked) and never needs a food output slot.
     *
     * <p>Frozen food never reaches this point with a frozen tag: its thaw is folded into
     * {@code cookingTotalTime}, so by the time the recipe completes the stack is at oven temperature.
     */
    @Inject(method = "burn(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/item/crafting/Recipe;Lnet/minecraft/core/NonNullList;I)Z",
            at = @At("HEAD"), cancellable = true)
    private static void eternalsnow$cookFoodInPlace(net.minecraft.core.RegistryAccess registryAccess,
                                                    Recipe<?> recipe,
                                                    NonNullList<ItemStack> items,
                                                    int maxStackSize,
                                                    CallbackInfoReturnable<Boolean> cir) {
        ItemStack input = items.get(0);
        if (input.isEmpty() || FoodProfiles.profileOf(input) < 0) {
            return; // not food: vanilla path, result into slot 2
        }
        ItemStack result = recipe.getResultItem(registryAccess);
        if (result.isEmpty()) {
            return;
        }
        // One portion at a time; the oven is far above freezing, so the result is never frozen.
        ItemStack cooked = result.copy();
        cooked.setCount(1);
        FoodState.markThawed(cooked, FoodState.BODY_C, 0L);
        items.set(0, cooked);
        cir.setReturnValue(Boolean.TRUE);
    }
}
