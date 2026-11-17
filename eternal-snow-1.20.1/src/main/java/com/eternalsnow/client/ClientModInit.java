package com.eternalsnow.client;

import com.eternalsnow.config.ClientSnowConfig;
import com.eternalsnow.registry.ModRegistry;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.state.HeatScanClientData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class ClientModInit {

    private ClientModInit() {
    }

    public static void init() {
        ClientSnowConfig.register();

        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new EternalSnowConfigScreen(parent))
        );

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(ClientModInit::onClientSetup);
        modBus.addListener(ClientModInit::onRegisterLayers);
        modBus.addListener(ClientModInit::onRegisterRenderers);
        modBus.addListener(ClientModInit::onRegisterGuiOverlays);

        MinecraftForge.EVENT_BUS.addListener(ClientGuiEvents::onScreenInit);
        MinecraftForge.EVENT_BUS.addListener(ClientModInit::onClientLogout);
        MinecraftForge.EVENT_BUS.addListener(WindSnowRenderer::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(WindSnowRenderer::onRenderLevelStage);
        MinecraftForge.EVENT_BUS.addListener(HeatVisionRenderer::onRenderLevelStage);
        MinecraftForge.EVENT_BUS.addListener(BlizzardFogHandler::onRenderFog);
        MinecraftForge.EVENT_BUS.addListener(BlizzardFogHandler::onComputeFogColor);
        MinecraftForge.EVENT_BUS.addListener(WinterSoundManager::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ClientModInit::onBreathVaporTick);
        MinecraftForge.EVENT_BUS.addListener(ItemTooltipHandler::onItemTooltip);
    }

    @SuppressWarnings({"deprecation", "removal"})
    private static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModRegistry.COCOA_POT_MENU.get(), CocoaPotScreen::new);
            ItemBlockRenderTypes.setRenderLayer(ModRegistry.WALL_THERMOMETER.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModRegistry.COCOA_POT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModRegistry.ICICLE.get(), RenderType.cutout());
        });
    }

    private static void onRegisterLayers(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FestiveCopperGolemModel.LAYER_LOCATION, FestiveCopperGolemModel::createBodyLayer);
    }

    private static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistry.FESTIVE_COPPER_GOLEM.get(), FestiveCopperGolemRenderer::new);
        event.registerBlockEntityRenderer(ModRegistry.COCOA_POT_BE.get(), CocoaPotRenderer::new);
        event.registerBlockEntityRenderer(ModRegistry.FIREPLACE_BE.get(), FireplaceRenderer::new);
    }

    private static void onRegisterGuiOverlays(final RegisterGuiOverlaysEvent event) {
        event.registerBelow(VanillaGuiOverlay.HOTBAR.id(), "face_snow", FaceSnowOverlay.OVERLAY);
        event.registerBelow(VanillaGuiOverlay.AIR_LEVEL.id(), "thirst_level", SurvivalHudOverlay.THIRST_OVERLAY);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "body_temperature", SurvivalHudOverlay.TEMPERATURE_OVERLAY);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "handheld_instruments", SurvivalHudOverlay.INSTRUMENT_OVERLAY);
    }

    private static void onClientLogout(final ClientPlayerNetworkEvent.LoggingOut event) {
        EternalSnowState.resetClientSession();
        HeatScanClientData.clear();
        WinterSoundManager.fadeOutAll();
        WindSnowRenderer.resetCache();
    }

    /**
     * В: Atmospheric Frost Breath Vapor when local air temperature is below -2°C (vanishes indoors automatically!).
     */
    private static void onBreathVaporTick(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused() || mc.player.isSpectator() || mc.player.isUnderWater()) {
            return;
        }
        if (!EternalSnowState.getEffectiveSettings().enabled || !EternalSnowState.isOverworldLike(mc.level)) {
            return;
        }
        if (EternalSnowState.clientLocalTempC > -2.0F) {
            return; // Indoors or warm — breath vapor vanishes naturally!
        }
        int interval = mc.player.isSprinting() ? 32 : 56;
        if (mc.player.tickCount % interval != 0) {
            return;
        }
        Vec3 eye = mc.player.getEyePosition(1.0F);
        Vec3 look = mc.player.getLookAngle();
        double px = eye.x + look.x * 0.34D;
        double py = eye.y - 0.11D + look.y * 0.22D;
        double pz = eye.z + look.z * 0.34D;
        double vx = look.x * 0.018D + EternalSnowState.getWindDirX() * EternalSnowState.windSpeed * 0.012D;
        double vy = 0.008D + Math.max(0.0D, look.y * 0.01D);
        double vz = look.z * 0.018D + EternalSnowState.getWindDirZ() * EternalSnowState.windSpeed * 0.012D;
        mc.level.addParticle(ParticleTypes.CLOUD, px, py, pz, vx, vy, vz);
    }
}
