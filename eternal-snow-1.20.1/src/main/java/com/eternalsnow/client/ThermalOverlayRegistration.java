package com.eternalsnow.client;

import com.eternalsnow.EternalSnowMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registers the player-body debug readout only on the physical client. */
@Mod.EventBusSubscriber(modid = EternalSnowMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ThermalOverlayRegistration {
    private ThermalOverlayRegistration() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "body_temperature", ThermalHud.OVERLAY);
    }
}
