package com.eternalsnow.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;

public final class ClientGuiEvents {
    private ClientGuiEvents() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();

        // 1. Primary placement: right on the ESC Pause Menu (PauseScreen)!
        if (screen instanceof PauseScreen) {
            placeOnPauseScreen(event, screen);
            return;
        }

        // 2. Convenient access on TitleScreen & OptionsScreen as well
        if (screen instanceof TitleScreen) {
            placeOnTitleScreen(event, screen);
        } else if (screen instanceof OptionsScreen) {
            placeOnOptionsScreen(event, screen);
        }
    }

    private static void placeOnPauseScreen(ScreenEvent.Init.Post event, Screen screen) {
        int btnW = 204;
        int btnH = 20;
        int centerX = screen.width / 2 - btnW / 2;

        // Find top-most and bottom-most centered column buttons on PauseScreen
        int minColY = Integer.MAX_VALUE;
        int maxColBottom = Integer.MIN_VALUE;
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof AbstractWidget w && w.visible) {
                int mid = w.getX() + w.getWidth() / 2;
                if (Math.abs(mid - screen.width / 2) <= 60) {
                    minColY = Math.min(minColY, w.getY());
                    maxColBottom = Math.max(maxColBottom, w.getY() + w.getHeight());
                }
            }
        }

        // Prefer placing cleanly right below "Disconnect / Save and Quit" (204px full-width button),
        // or right above "Back to Game" if near bottom of small window, or top-right corner fallback!
        int targetX = centerX;
        int targetY = (maxColBottom != Integer.MIN_VALUE) ? (maxColBottom + 4) : (screen.height / 4 + 132);

        if (targetY + btnH > screen.height - 6 || overlapsAnyWidget(event, targetX, targetY, btnW, btnH)) {
            int aboveTop = (minColY != Integer.MAX_VALUE) ? (minColY - 24) : 16;
            if (aboveTop >= 6 && !overlapsAnyWidget(event, centerX, aboveTop, btnW, btnH)) {
                targetX = centerX;
                targetY = aboveTop;
            } else {
                btnW = 128;
                targetX = screen.width - btnW - 6;
                targetY = 6;
            }
        }

        EternalSnowWidgets.ActionButton pauseBtn = new EternalSnowWidgets.ActionButton(
                targetX,
                targetY,
                btnW,
                btnH,
                Component.translatable("eternalsnow.options.button"),
                () -> Minecraft.getInstance().setScreen(new EternalSnowConfigScreen(screen)),
                true
        );
        pauseBtn.setTooltip(Tooltip.create(Component.translatable("eternalsnow.options.button.tooltip")));
        event.addListener(pauseBtn);
    }

    private static void placeOnTitleScreen(ScreenEvent.Init.Post event, Screen screen) {
        int btnW = 24;
        int btnH = 20;
        int x = screen.width / 2 + 104;
        int y = screen.height / 4 + 48;

        if (overlapsAnyWidget(event, x, y, btnW, btnH)) {
            x = screen.width - 116;
            y = 6;
            btnW = 110;
        }

        Component label = (btnW <= 28)
                ? Component.literal("❄")
                : Component.translatable("eternalsnow.options.button");

        EternalSnowWidgets.ActionButton titleBtn = new EternalSnowWidgets.ActionButton(
                x, y, btnW, btnH, label,
                () -> Minecraft.getInstance().setScreen(new EternalSnowConfigScreen(screen)),
                true
        );
        titleBtn.setTooltip(Tooltip.create(Component.translatable("eternalsnow.options.button")));
        event.addListener(titleBtn);
    }

    private static void placeOnOptionsScreen(ScreenEvent.Init.Post event, Screen screen) {
        int btnW = 200;
        int btnH = 20;
        int x = screen.width / 2 - btnW / 2;
        int y = screen.height / 6 + 15;

        if (overlapsAnyWidget(event, x, y, btnW, btnH)) {
            return; // PauseScreen already has the main button, so never crowd OptionsScreen if occupied!
        }

        EternalSnowWidgets.ActionButton optBtn = new EternalSnowWidgets.ActionButton(
                x, y, btnW, btnH,
                Component.translatable("eternalsnow.options.button"),
                () -> Minecraft.getInstance().setScreen(new EternalSnowConfigScreen(screen)),
                true
        );
        event.addListener(optBtn);
    }

    private static boolean overlapsAnyWidget(ScreenEvent.Init.Post event, int x, int y, int width, int height) {
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof AbstractWidget w && w.visible) {
                boolean overlapX = x < (w.getX() + w.getWidth()) && (x + width) > w.getX();
                boolean overlapY = y < (w.getY() + w.getHeight()) && (y + height) > w.getY();
                if (overlapX && overlapY) {
                    return true;
                }
            }
        }
        return false;
    }
}
