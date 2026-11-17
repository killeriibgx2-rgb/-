package com.eternalsnow.mixin;

import com.eternalsnow.food.FoodProfiles;
import com.eternalsnow.food.FoodState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Food cooks one portion at a time; frozen input adds thaw time to the vanilla cycle.
 * Since 2.7.5 the vanilla result slot retains cooked food, just like other smelting output.
 * Vanilla creates the recipe result rather than copying the frozen input NBT, and handles
 * output capacity, extraction, XP and automation. No per-tick output rewrite is necessary.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    /** Vanilla's own cook time, remembered so the thaw can be derived from it every time. */
    @Unique
    private int eternalsnow$baseCookTicks = 200;

    /**
     * One portion at a time: food cannot be stacked into the furnace slot, so hopper automation feeds
     * it one item per cycle instead of dumping a whole stack.
     */
    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void eternalsnow$singlePortionFood(int slot, ItemStack stack,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (slot != 0 || stack.isEmpty() || FoodProfiles.profileOf(stack) < 0) {
            return;
        }
        if (!com.eternalsnow.state.EternalSnowState.getServerSettings().enableCookingTweaks) {
            return;
        }
        if (stack.getCount() > 1) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Folds the thaw into the vanilla cook time at the exact place vanilla asks for it. Runs before
     * vanilla assigns {@code cookingTotalTime}, which is why the value is computed from scratch here
     * and never added to a previous total.
     */
    @Redirect(method = "setItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getTotalCookTime(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;)I"))
    private int eternalsnow$thawAwareCookTime(Level level, AbstractFurnaceBlockEntity furnace) {
        int vanilla;
        try {
            vanilla = FurnaceCookTimeInvoker.eternalsnow$getTotalCookTime(level, furnace);
        } catch (Throwable notApplied) {
            // The accessor is a convenience, never a requirement: a furnace must keep working even if a
            // future Minecraft version renames the private helper.
            vanilla = 200;
        }
        this.eternalsnow$baseCookTicks = vanilla;

        ItemStack input = furnace.getItem(0);
        if (input.isEmpty() || !FoodState.isFrozen(input)
                || !com.eternalsnow.state.EternalSnowState.getServerSettings().enableCookingTweaks) {
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

}
