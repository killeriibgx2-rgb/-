package com.zzynes.flydrone.DRONE;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HandheldSirenRenderer extends GeoItemRenderer<HandheldSirenItem> {
    public static final HandheldSirenRenderer INSTANCE = new HandheldSirenRenderer();

    public static final net.minecraftforge.client.extensions.common.IClientItemExtensions EXTENSIONS =
            new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return INSTANCE;
                }
            };

    public HandheldSirenRenderer() {
        super(new HandheldSirenModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        if (displayContext == ItemDisplayContext.GUI) {
            poseStack.translate(0.61D, 0.30D, 0.5D);
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(31.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
            poseStack.scale(1.76F, 1.76F, 1.76F);
            poseStack.translate(-0.5D, -0.5D, -0.5D);
        }

        super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.popPose();
    }
}