package com.eternalsnow.client;

import com.eternalsnow.state.EternalSnowState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "eternalsnow", value = Dist.CLIENT)
public final class CozyFurnaceEvents {
    private CozyFurnaceEvents() {}

    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event) {
        // Exact class check deliberately leaves other mods' screens and our own replacement alone.
        // Smokers and blast furnaces also retain their own screens.
        if (event.getNewScreen() == null || event.getNewScreen().getClass() != FurnaceScreen.class
                || !EternalSnowState.isClientConnectedToServerMod()
                || !EternalSnowState.getClientSyncedSettings().enableCookingTweaks) {
            return;
        }
        FurnaceScreen original = (FurnaceScreen) event.getNewScreen();
        var player = Minecraft.getInstance().player;
        if (player == null || original.getMenu().slots.size() != 39) {
            return;
        }
        event.setNewScreen(new CozyFurnaceScreen(original.getMenu(), player.getInventory(), original.getTitle()));
    }
}
