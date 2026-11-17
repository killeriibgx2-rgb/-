package com.eternalsnow.client;

import com.eternalsnow.block.FoodShelfBlock;
import com.eternalsnow.block.FoodShelfBlockEntity;
import com.eternalsnow.food.FoodShelfManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * v2.7 — draws whatever is hanging on the shelf.
 *
 * <p>The item is the <em>original</em> item model, rendered flat against the wall: that is the whole
 * point, the player must recognise their own steak. No tint, no overlay, no custom texture — the
 * tooltip is what tells you it is frozen.
 *
 * <p>Hook positions come from {@link FoodShelfManager#hookCenterAlong}, the same function the click
 * math uses, so what you see is exactly what you hit.
 */
public final class FoodShelfRenderer implements BlockEntityRenderer<FoodShelfBlockEntity> {

    private static final float ITEM_SCALE = 0.275F;

    public FoodShelfRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FoodShelfBlockEntity shelf, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = shelf.getBlockState();
        if (!(state.getBlock() instanceof FoodShelfBlock)) {
            return;
        }
        Direction facing = state.getValue(FoodShelfBlock.FACING);
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        for (int hook = 0; hook < FoodShelfBlock.HOOKS; hook++) {
            ItemStack stack = shelf.getHook(hook);
            if (stack.isEmpty()) {
                continue;
            }

            pose.pushPose();
            // Centre of the block, then offset along the shelf and drop onto the hook.
            pose.translate(0.5D, 0.0D, 0.5D);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));

            float along = FoodShelfManager.hookCenterAlong(hook) - 0.5F;
            // North model hooks sit at z=14.2..15.4px. Put the item just in front,
            // with its upper edge near the hook bottom (y=11px).
            pose.translate(along, 0.57D, 0.37D);

            // Face the item outward (the shelf hangs on the wall, the item sticks out of it).
            // FIXED item models already stand in XY. Only turn the face toward NORTH;
            // an X rotation here would lay the sprite horizontally.
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);

            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight,
                    OverlayTexture.NO_OVERLAY, pose, buffers, shelf.getLevel(), hook);
            pose.popPose();
        }
    }

    /** The shelf is thin, so it never needs a bigger render box than the block itself. */
    @Override
    public boolean shouldRenderOffScreen(FoodShelfBlockEntity shelf) {
        return false;
    }

    @Override
    public int getViewDistance() {
        return 32;
    }
}
