package com.zzynes.fly_drone;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class RPGRocketRenderer extends ThrownItemRenderer<RPGRocketEntity> {
    public RPGRocketRenderer(EntityRendererProvider.Context context) {
        super(context, 1.0F, true);
    }
}
