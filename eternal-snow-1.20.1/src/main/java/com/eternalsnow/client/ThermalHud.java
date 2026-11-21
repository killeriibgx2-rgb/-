package com.eternalsnow.client;

import com.eternalsnow.thermal.engine.BodyModel;
import com.eternalsnow.thermal.world.ClientBodyState;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Numeric body/air readout; deliberately plain because it is a debug HUD. */
public final class ThermalHud {
    public static final IGuiOverlay OVERLAY = ThermalHud::render;

    private ThermalHud() {
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
                               int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || !gui.shouldDrawSurvivalElements()
                || minecraft.player == null || !ClientBodyState.enabled()) return;

        double core = ClientBodyState.coreCelsius();
        double air = ClientBodyState.airCelsius();
        int coreColor = core < BodyModel.HYPOTHERMIA_CELSIUS ? 0x66CFFF
                : core > BodyModel.HYPERTHERMIA_CELSIUS ? 0xFF7043 : 0xFFFFFF;
        Component coreText = Component.translatable("eternalsnow.hud.body_core",
                String.format(Locale.ROOT, "%.1f °C", core));
        Component airText = Component.translatable("eternalsnow.hud.air",
                String.format(Locale.ROOT, "%.1f °C", air));

        gui.setupOverlayRenderState(true, false);
        graphics.drawString(minecraft.font, coreText, 8, 8, coreColor, true);
        graphics.drawString(minecraft.font, airText, 8, 18, 0xB7E7FF, true);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
