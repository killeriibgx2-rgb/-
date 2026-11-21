package com.eternalsnow.client;

import com.eternalsnow.EternalSnowMod;
import com.eternalsnow.thermal.world.ClientBodyState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EternalSnowMod.MOD_ID, value = Dist.CLIENT)
public final class ThermalClientEvents {
    private ThermalClientEvents() {
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientBodyState.clear();
    }
}
