package com.zzynes.flydrone.DRONE;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HandheldSirenModel extends GeoModel<HandheldSirenItem> {
    @Override
    public ResourceLocation getModelResource(HandheldSirenItem object) {
        return new ResourceLocation(FlyDroneMod.MOD_ID, "geo/handheld_siren.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HandheldSirenItem object) {
        return new ResourceLocation(FlyDroneMod.MOD_ID, "textures/item/handheld_siren.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HandheldSirenItem animatable) {
        return new ResourceLocation(FlyDroneMod.MOD_ID, "animations/handheld_siren.animation.json");
    }
}