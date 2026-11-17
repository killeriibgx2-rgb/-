package com.eternalsnow.mixin;

import com.eternalsnow.food.FoodProfiles;
import com.eternalsnow.food.FoodState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * v2.7.1 — food gets its own rhythm inside a furnace: one portion at a time, a frozen portion must
 * thaw first, and cooking happens IN the food slot instead of a result slot.
 *
 * <h2>Why cooking is not hooked in {@code burn}</h2>
 * {@code burn} is one of the very few private STATIC methods on the vanilla class. Mixin 0.8.5 checks
 * staticness of {@code @Inject} handlers with {@code exactMatch = true}, so a handler for a static
 * target must itself be static — but a static handler cannot see {@code this}, and {@code burn} never
 * receives the block entity at all. Writing the cooked item back therefore cannot happen there.
 *
 * <p>It is moved to the tick instead: when the vanilla cook progress overflows, {@code serverTick}
 * calls {@code burn}, which puts the cooked item into slot 2. One tick later we take it from slot 2 and
 * put it back into slot 0. Same result, no static-injection trap, and no second copy of vanilla's
 * recipe logic to keep in sync.
 *
 * <h2>Thaw</h2>
 * The thaw is folded into {@code cookingTotalTime}, which vanilla streams to the client as
 * {@code ContainerData} slot 3, so the vanilla progress arrow slows down honestly — no custom GUI and
 * no numbers on screen. The constant itself is computed once per stack by a {@code @Redirect} on the
 * exact {@code getTotalCookTime} call inside {@code setItem}; being a redirect (not an "add at TAIL"
 * hook) it can never accumulate on repeated clicks.
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

    /**
     * Moves a finished food portion from the result slot back into the food slot.
     *
     * <p>Runs at the tail of every furnace tick, which is after {@code burn} has deposited the cooked
     * item. Non-food never matches, so ores, glass and stone keep the vanilla behaviour untouched.
     */
    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void eternalsnow$finishFoodInPlace(Level level, BlockPos pos,
                                                      net.minecraft.world.level.block.state.BlockState state,
                                                      AbstractFurnaceBlockEntity furnace,
                                                      CallbackInfo ci) {
        if (!com.eternalsnow.state.EternalSnowState.getServerSettings().enableCookingTweaks) {
            return;
        }
        ItemStack result = furnace.getItem(2);
        if (result.isEmpty() || FoodProfiles.profileOf(result) < 0) {
            return;
        }
        ItemStack food = furnace.getItem(0);
        if (food.isEmpty()) {
            food = result.copy();
        } else if (ItemStack.isSameItemSameTags(food, result)
                && food.getCount() < food.getMaxStackSize()) {
            food.grow(result.getCount());
        } else {
            return; // the player already put something else in: leave the finished portion alone
        }

        // The oven is far above freezing, so what comes out is never frozen. The stamp is the current
        // game time so the next ageing pass integrates from now, not from when it went in.
        FoodState.markThawed(food, FoodState.BODY_C, level.getGameTime());
        furnace.setItem(0, food);
        furnace.setItem(2, ItemStack.EMPTY);
    }
}
