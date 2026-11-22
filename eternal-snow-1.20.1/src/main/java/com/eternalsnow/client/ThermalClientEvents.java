package com.eternalsnow.client;

import com.eternalsnow.EternalSnowMod;
import com.eternalsnow.network.ThermalNetwork;
import com.eternalsnow.network.ThermalSettingsRequestPacket;
import com.eternalsnow.thermal.world.ClientBodyState;
import com.eternalsnow.thermal.world.ClientThermalSettings;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EternalSnowMod.MOD_ID, value = Dist.CLIENT)
public final class ThermalClientEvents {
    private ThermalClientEvents() {
    }

    public static void openSettingsScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        ClientThermalSettings.clear();
        minecraft.setScreen(new ThermalSettingsScreen(minecraft.screen));
        ThermalNetwork.sendToServer(new ThermalSettingsRequestPacket());
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientBodyState.clear();
        ClientThermalSettings.clear();
    }
}
