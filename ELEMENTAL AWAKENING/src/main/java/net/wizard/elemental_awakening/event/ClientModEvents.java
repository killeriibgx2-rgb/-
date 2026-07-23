package net.wizard.elemental_awakening.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wizard.elemental_awakening.ElementalAwakening;
import net.wizard.elemental_awakening.client.ManaHudOverlay;

// Аннотация указывает движку Forge, что этот класс работает СТРОГО на стороне клиента
@Mod.EventBusSubscriber(modid = ElementalAwakening.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    // Регистрируем нашу полоску маны над стандартной полоской здоровья
    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "mana_bar", ManaHudOverlay.HUD_MANA);
    }
}
