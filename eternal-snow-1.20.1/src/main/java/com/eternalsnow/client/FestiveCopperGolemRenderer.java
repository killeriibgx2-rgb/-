package com.eternalsnow.client;

import com.eternalsnow.EternalSnowMod;
import com.eternalsnow.entity.FestiveCopperGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public final class FestiveCopperGolemRenderer extends MobRenderer<FestiveCopperGolemEntity, FestiveCopperGolemModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            EternalSnowMod.MODID,
            "textures/entity/festive_copper_golem.png"
    );

    public FestiveCopperGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new FestiveCopperGolemModel(context.bakeLayer(FestiveCopperGolemModel.LAYER_LOCATION)), 0.48F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(FestiveCopperGolemEntity entity) {
        return TEXTURE;
    }
}
