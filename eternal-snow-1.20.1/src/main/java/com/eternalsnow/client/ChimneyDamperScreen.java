package com.eternalsnow.client;

import com.eternalsnow.block.FireplaceBlockEntity;
import com.eternalsnow.block.FireplaceFuelHelper;
import com.eternalsnow.network.PacketHandler;
import com.eternalsnow.network.ServerboundSetChimneyDamperPacket;
import com.eternalsnow.state.EternalSnowState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.widget.ForgeSlider;

import java.util.Locale;

/**
 * Chimney Damper Control Screen (Шибер / Задвижка дымохода):
 * - Opens on Right-Click anywhere along the Brick Chimney column.
 * - Adjusts draft 0% .. 100%:
 *   * 0% = Closed -> zero draft, fireplace immediately extinguishes.
 *   * 1%..25% = Barely open -> smoldering embers, low heat, dramatically extended burn time (up to 3h 42m!).
 *   * 100% = Wide open -> maximum draft, roaring heat, 40:00 full-load burn time.
 * - Fuel Bar (0..100%) stays fixed while calculated burn time & heat dynamically update!
 */
public final class ChimneyDamperScreen extends Screen {
    private final BlockPos fireplaceMasterPos;
    private int damperPercent;
    private int lastSentPercent;
    private int fallbackFuelMilli;
    private boolean fallbackLit;
    private final boolean chimneySkyValid;
    private ForgeSlider damperSlider;

    public ChimneyDamperScreen(
            BlockPos fireplaceMasterPos,
            int initialDamperPercent,
            int initialFuelMilli,
            boolean initialLit,
            boolean chimneySkyValid
    ) {
        super(Component.translatable("eternalsnow.chimney.screen.title"));
        this.fireplaceMasterPos = fireplaceMasterPos;
        this.damperPercent = Mth.clamp(initialDamperPercent, 0, 100);
        this.lastSentPercent = this.damperPercent;
        this.fallbackFuelMilli = initialFuelMilli;
        this.fallbackLit = initialLit;
        this.chimneySkyValid = chimneySkyValid;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int panelTop = this.height / 2 - 96;
        int sliderY = panelTop + 98;

        this.damperSlider = new ForgeSlider(
                centerX - 140,
                sliderY,
                280,
                20,
                Component.translatable("eternalsnow.chimney.slider.prefix"),
                Component.literal("%"),
                0.0D,
                100.0D,
                this.damperPercent,
                1.0D,
                0,
                true
        ) {
            @Override
            protected void applyValue() {
                int val = this.getValueInt();
                ChimneyDamperScreen.this.damperPercent = val;
                if (val == 0) {
                    ChimneyDamperScreen.this.fallbackLit = false;
                }
                ChimneyDamperScreen.this.syncIfChanged();
            }
        };
        this.addRenderableWidget(this.damperSlider);

        // 4 Quick Preset Buttons: [0% Закрыть] [15% Тление] [60% Уют] [100% Макс]
        int presetY = sliderY + 24;
        int pW = 67;
        int gap = 4;
        int startX = centerX - 140;

        this.addRenderableWidget(new EternalSnowWidgets.ActionButton(
                startX, presetY, pW, 18,
                Component.translatable("eternalsnow.chimney.preset.close"),
                () -> setPreset(0)
        ));
        this.addRenderableWidget(new EternalSnowWidgets.ActionButton(
                startX + (pW + gap), presetY, pW, 18,
                Component.translatable("eternalsnow.chimney.preset.smolder"),
                () -> setPreset(15)
        ));
        this.addRenderableWidget(new EternalSnowWidgets.ActionButton(
                startX + (pW + gap) * 2, presetY, pW, 18,
                Component.translatable("eternalsnow.chimney.preset.cozy"),
                () -> setPreset(60)
        ));
        this.addRenderableWidget(new EternalSnowWidgets.ActionButton(
                startX + (pW + gap) * 3, presetY, pW, 18,
                Component.translatable("eternalsnow.chimney.preset.max"),
                () -> setPreset(100)
        ));

        this.addRenderableWidget(new EternalSnowWidgets.ActionButton(
                centerX - 70, presetY + 24, 140, 20,
                CommonComponents.GUI_DONE,
                () -> {
                    syncIfChanged();
                    this.onClose();
                },
                true
        ));
    }

    private void setPreset(int pct) {
        this.damperPercent = Mth.clamp(pct, 0, 100);
        if (pct == 0) {
            this.fallbackLit = false;
        }
        if (this.damperSlider != null) {
            this.damperSlider.setValue(this.damperPercent);
        }
        syncIfChanged();
    }

    private void syncIfChanged() {
        if (this.damperPercent != this.lastSentPercent) {
            this.lastSentPercent = this.damperPercent;
            PacketHandler.sendToServer(new ServerboundSetChimneyDamperPacket(this.fireplaceMasterPos, this.damperPercent));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        Minecraft mc = Minecraft.getInstance();
        int liveFuelMilli = this.fallbackFuelMilli;
        boolean liveLit = this.fallbackLit;
        if (mc.level != null && mc.level.getBlockEntity(this.fireplaceMasterPos) instanceof FireplaceBlockEntity be) {
            liveFuelMilli = be.getFuelMilli();
            liveLit = be.isLit() && this.damperPercent > 0;
        }

        int centerX = this.width / 2;
        int panelTop = this.height / 2 - 96;
        int panelH = 192;

        EternalSnowWidgets.drawVanillaSnowPanel(g, centerX - 156, panelTop, 312, panelH, 0xEC222830, true);
        g.drawCenteredString(this.font, this.title, centerX, panelTop + 10, 0xFFF2F6FA);

        // 1. Draft Status Line
        Component statusText;
        int statusCol;
        if (!this.chimneySkyValid) {
            statusText = Component.translatable("eternalsnow.chimney.status.blocked_sky");
            statusCol = 0xFFFF6655;
        } else if (this.damperPercent <= 0) {
            statusText = Component.translatable("eternalsnow.chimney.status.closed");
            statusCol = 0xFFFF7755;
        } else if (this.damperPercent <= 25) {
            statusText = Component.translatable("eternalsnow.chimney.status.smolder");
            statusCol = 0xFFFFCC77;
        } else if (this.damperPercent <= 75) {
            statusText = Component.translatable("eternalsnow.chimney.status.medium");
            statusCol = 0xFFA8F0B0;
        } else {
            statusText = Component.translatable("eternalsnow.chimney.status.max");
            statusCol = 0xFFFF9638;
        }
        g.drawCenteredString(this.font, statusText, centerX, panelTop + 26, statusCol);

        // 2. Fixed 0..100% Fuel Load Bar (never shifts when moving the damper slider!)
        float fillRatio = FireplaceFuelHelper.getFillRatio(liveFuelMilli);
        int fillPct = FireplaceFuelHelper.getFillPercentRounded(liveFuelMilli);
        Component fuelBarLabel = Component.translatable("eternalsnow.chimney.fuel_scale", fillPct);
        g.drawString(this.font, fuelBarLabel, centerX - 140, panelTop + 42, 0xFFE2ECF6, true);

        int barX = centerX - 140;
        int barY = panelTop + 53;
        int barW = 280;
        int barH = 8;
        g.fill(barX, barY, barX + barW, barY + barH, 0xCC10161E);
        g.renderOutline(barX, barY, barW, barH, 0xFF6B8299);
        if (fillRatio > 0.0F) {
            int filledW = Math.max(2, Math.round((barW - 2) * fillRatio));
            int fillCol = liveLit ? 0xFFFF9233 : 0xFF5CB8FF;
            g.fill(barX + 1, barY + 1, barX + 1 + filledW, barY + barH - 1, fillCol);
        }

        // 3. Dynamically Recalculated Burn Time & Thermal Output!
        Component timeLine;
        if (this.damperPercent <= 0) {
            timeLine = Component.translatable("eternalsnow.chimney.time_closed");
        } else {
            int curTicks = FireplaceFuelHelper.computeRemainingRealTicks(liveFuelMilli, this.damperPercent);
            int fullTicks = FireplaceFuelHelper.computeRemainingRealTicks(FireplaceFuelHelper.MAX_FUEL_MILLI, this.damperPercent);
            timeLine = Component.translatable(
                    "eternalsnow.chimney.time_calc",
                    FireplaceFuelHelper.formatTicksMMSS(curTicks),
                    FireplaceFuelHelper.formatTicksMMSS(fullTicks)
            );
        }
        g.drawCenteredString(this.font, timeLine, centerX, panelTop + 67, 0xFFD6E8FA);

        float baseHeat = EternalSnowState.getEffectiveSettings().heatFireplace;
        float mult = FireplaceFuelHelper.getHeatPowerMultiplier(this.damperPercent);
        float effPowerC = baseHeat * mult;
        String powerStr = (this.damperPercent <= 0)
                ? "0°C (0%)"
                : String.format(Locale.ROOT, "+%.0f°C (%d%%)", effPowerC, Math.round(mult * 100.0F));
        Component heatLine = Component.translatable("eternalsnow.chimney.heat_calc", powerStr);
        g.drawCenteredString(this.font, heatLine, centerX, panelTop + 80, this.damperPercent <= 0 ? 0xFF9AA8B8 : 0xFFFFC278);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        syncIfChanged();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
