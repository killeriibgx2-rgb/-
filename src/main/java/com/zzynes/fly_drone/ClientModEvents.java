package com.zzynes.fly_drone;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FlyDroneMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FPV_DRONE.get(), FPVDroneRenderer::new);
        event.registerEntityRenderer(ModEntities.RPG_ROCKET.get(), context -> new ThrownItemRenderer<>(context));
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(new ModelLayerLocation(new ResourceLocation(FlyDroneMod.MOD_ID, "fpv_drone"), "main"), () -> fpv_drone.createBodyLayer());
        event.registerLayerDefinition(new ModelLayerLocation(new ResourceLocation(FlyDroneMod.MOD_ID, "ci4"), "main"), () -> ci4.createBodyLayer());
        event.registerLayerDefinition(new ModelLayerLocation(new ResourceLocation(FlyDroneMod.MOD_ID, "rpg_snar"), "main"), () -> rpg_snar.createBodyLayer());
    }
}
