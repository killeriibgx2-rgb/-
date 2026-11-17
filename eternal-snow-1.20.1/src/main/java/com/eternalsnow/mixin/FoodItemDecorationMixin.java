package com.eternalsnow.mixin;

import com.eternalsnow.food.FoodState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Small pixel corner markers in inventories and hotbar; original item artwork is unchanged. */
@Mixin(GuiGraphics.class)
public abstract class FoodItemDecorationMixin {
    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("TAIL"))
    private void eternalsnow$stateCorners(Font font, ItemStack stack, int x, int y,
                                          String text, CallbackInfo ci) {
        if (stack.isEmpty()) return;
        boolean frozen = FoodState.isFrozen(stack);
        int spoil = stack.is(Items.ROTTEN_FLESH) ? 4 : FoodState.spoil(stack);
        if (!frozen && spoil == 0) return;
        GuiGraphics g = (GuiGraphics) (Object) this;
        g.pose().pushPose();
        g.pose().translate(0, 0, 250);
        if (frozen) {
            g.fill(x, y, x + 5, y + 1, 0xFF75E6FF);
            g.fill(x, y, x + 1, y + 5, 0xFF75E6FF);
        }
        if (spoil > 0) {
            int color = spoil >= 3 ? 0xFF759442 : spoil == 2 ? 0xFFE5B84C : 0xFFB7AA8B;
            g.fill(x + 11, y, x + 16, y + 1, color);
            g.fill(x + 15, y, x + 16, y + 5, color);
        }
        g.pose().popPose();
    }
}
