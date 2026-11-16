package com.eternalsnow.client;

import com.eternalsnow.EternalSnowMod;
import com.eternalsnow.block.ChimneyBlock;
import com.eternalsnow.block.FireplaceBlock;
import com.eternalsnow.block.FireplaceBlockEntity;
import com.eternalsnow.block.FireplaceFuelHelper;
import com.eternalsnow.block.FireplacePartBlock;
import com.eternalsnow.block.WallThermometerBlockEntity;
import com.eternalsnow.config.ClientSnowConfig;
import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.item.HeatScannerItem;
import com.eternalsnow.item.LocalThermometerItem;
import com.eternalsnow.item.ThermometerItem;
import com.eternalsnow.item.WoodenFirestarterItem;
import com.eternalsnow.registry.ModRegistry;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.state.HeatScanClientData;
import com.eternalsnow.world.CampfireFuelManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Locale;

public final class SurvivalHudOverlay {

    private static final ResourceLocation HUD_ICONS =
            new ResourceLocation(EternalSnowMod.MODID, "textures/gui/hud_icons.png");

    public static final IGuiOverlay THIRST_OVERLAY = SurvivalHudOverlay::renderThirstBar;
    public static final IGuiOverlay TEMPERATURE_OVERLAY = SurvivalHudOverlay::renderTemperatureIcon;
    public static final IGuiOverlay INSTRUMENT_OVERLAY = SurvivalHudOverlay::renderHandheldInstrumentHud;

    private SurvivalHudOverlay() {
    }

    private static void renderThirstBar(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !gui.shouldDrawSurvivalElements()) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || !ClientSnowConfig.showSurvivalHud) {
            return;
        }
        ServerSnowSettings settings = EternalSnowState.getEffectiveSettings();
        if (!settings.enabled || !settings.enableThirst) {
            return;
        }

        gui.setupOverlayRenderState(true, false);
        int thirst = Mth.clamp(EternalSnowState.clientThirstLevel, 0, 20);
        int rightBaseX = screenWidth / 2 + 91;
        int topY = screenHeight - gui.rightHeight;
        long ticks = gui.getGuiTicks();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        for (int i = 0; i < 10; i++) {
            int dropX = rightBaseX - i * 8 - 9;
            int dropY = topY;
            if (thirst <= 4 && ticks % (Math.max(1, thirst) * 3L + 1L) == 0L) {
                dropY += (int) ((ticks + i * 3L) % 3L - 1L);
            }
            guiGraphics.blit(HUD_ICONS, dropX, dropY, 0.0F, 16.0F, 9, 9, 128, 64);
            int idx = i * 2 + 1;
            if (idx < thirst) {
                guiGraphics.blit(HUD_ICONS, dropX, dropY, 18.0F, 16.0F, 9, 9, 128, 64);
            } else if (idx == thirst) {
                guiGraphics.blit(HUD_ICONS, dropX, dropY, 9.0F, 16.0F, 9, 9, 128, 64);
            }
        }
        gui.rightHeight += 10;
    }

    private static void renderTemperatureIcon(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !gui.shouldDrawSurvivalElements()) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || !ClientSnowConfig.showSurvivalHud) {
            return;
        }
        ServerSnowSettings settings = EternalSnowState.getEffectiveSettings();
        if (!settings.enabled || !settings.enablePlayerTemperature) {
            return;
        }

        gui.setupOverlayRenderState(true, false);
        float bodyState = EternalSnowState.clientBodyTempState;
        int iconX = screenWidth / 2 - 8;
        int iconY = screenHeight - 53;
        if (player.experienceLevel > 0) {
            iconY -= 4;
        }

        float pulse = 1.0F;
        if (Math.abs(bodyState) >= 75.0F) {
            float phase = (gui.getGuiTicks() + partialTick) * 0.32F;
            pulse = 0.78F + 0.22F * Mth.sin(phase);
            if (Math.abs(bodyState) >= 88.0F) {
                iconX += (gui.getGuiTicks() % 2 == 0) ? 0 : ((gui.getGuiTicks() / 2 % 2 == 0) ? -1 : 1);
            }
        }

        if (bodyState < -22.0F) {
            float t = Mth.clamp((-bodyState - 22.0F) / 78.0F, 0.0F, 1.0F);
            int u = (t > 0.5F) ? 32 : 16;
            float r = Mth.lerp(t, 0.80F, 0.45F) * pulse;
            float g = Mth.lerp(t, 0.95F, 0.88F) * pulse;
            RenderSystem.setShaderColor(r, g, 1.0F, 0.98F);
            guiGraphics.blit(HUD_ICONS, iconX, iconY, (float) u, 0.0F, 16, 16, 128, 64);
        } else if (bodyState > 24.0F) {
            float t = Mth.clamp((bodyState - 24.0F) / 76.0F, 0.0F, 1.0F);
            int u = (t > 0.5F) ? 64 : 48;
            float g = Mth.lerp(t, 0.94F, 0.42F) * pulse;
            float b = Mth.lerp(t, 0.55F, 0.20F) * pulse;
            RenderSystem.setShaderColor(1.0F, g, b, 0.98F);
            guiGraphics.blit(HUD_ICONS, iconX, iconY, (float) u, 0.0F, 16, 16, 128, 64);
        } else {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.85F);
            guiGraphics.blit(HUD_ICONS, iconX, iconY, 0.0F, 0.0F, 16, 16, 128, 64);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (ModRegistry.COZY_HEARTH.isPresent() && player.hasEffect(ModRegistry.COZY_HEARTH.get())) {
            guiGraphics.renderOutline(iconX - 1, iconY - 1, 18, 18, 0x99FF9E44);
        }
    }

    private static void renderHandheldInstrumentHud(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (mc.options.hideGui || player == null || mc.level == null) {
            return;
        }

        // 0. Live 10-Second Friction Progress Bar when holding ПКМ with WoodenFirestarterItem!
        boolean rubbingFirestarter = mc.screen == null
                && player.isUsingItem()
                && player.getUseItem().getItem() instanceof WoodenFirestarterItem;
        if (rubbingFirestarter) {
            renderFirestarterRubHud(mc, guiGraphics, partialTick, screenWidth, screenHeight, player);
        }

        // 1. Live Look-At HUD (Wall Thermometer, 3D Fireplace, Chimney, and Stick-Fueled Campfire!)
        if (!rubbingFirestarter && mc.screen == null && mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK
                && mc.hitResult instanceof BlockHitResult bhr) {
            BlockPos hitPos = bhr.getBlockPos();
            if (mc.level.getBlockEntity(hitPos) instanceof WallThermometerBlockEntity wallBe) {
                renderWallThermometerHud(mc, guiGraphics, screenWidth, screenHeight, wallBe);
            } else if (player.getEyePosition(1.0F).distanceToSqr(Vec3.atCenterOf(hitPos)) <= 4.0D) {
                // v2.6.0: fireplace/chimney indicator only point-blank (<= 2 blocks)
                renderLookAtFireHud(mc, guiGraphics, screenWidth, screenHeight, hitPos);
            }
        }

        // 2. Handheld Thermometer & Heat Scanner Readout Panels
        boolean holdingAdmin = player.getMainHandItem().getItem() instanceof ThermometerItem
                || player.getOffhandItem().getItem() instanceof ThermometerItem;
        boolean holdingLocal = player.getMainHandItem().getItem() instanceof LocalThermometerItem
                || player.getOffhandItem().getItem() instanceof LocalThermometerItem;
        ItemStack scannerStack = HeatScannerItem.getHeldScanner(player);
        boolean holdingScanner = !scannerStack.isEmpty();
        int scannerMode = holdingScanner ? HeatScannerItem.getMode(scannerStack) : HeatScannerItem.MODE_ALL_AIR;

        // v2.6.0: full-screen thermal pass + two world-anchored markers (cheap 2D GUI work only)
        if (scannerMode == HeatScannerItem.MODE_THERMAL) {
            renderThermalScreen(guiGraphics, screenWidth, screenHeight);
        }
        if (holdingScanner && HeatScanClientData.markersFresh()) {
            renderThermalMarkers(guiGraphics, screenWidth, screenHeight, mc.font);
        }

        boolean holdingThermometer = holdingAdmin || holdingLocal;
        if (!holdingThermometer && !holdingScanner) {
            return;
        }

        Font font = mc.font;
        int panelX = 8;
        int panelY = 8;

        if (holdingThermometer) {
            String hereStr = String.format(Locale.ROOT, "%+.1f°C", EternalSnowState.clientLocalTempC);
            String streetStr = String.format(Locale.ROOT, "%+.1f°C", EternalSnowState.clientStreetTempC);
            Component title = Component.translatable(holdingAdmin
                    ? "eternalsnow.hud.thermometer.title"
                    : "eternalsnow.hud.local_thermometer.title");
            Component hereLine = Component.translatable("eternalsnow.hud.thermometer.here", hereStr);
            Component streetLine = holdingAdmin
                    ? Component.translatable("eternalsnow.hud.thermometer.street", streetStr)
                    : null;

            int textW = Math.max(font.width(title), font.width(hereLine));
            if (streetLine != null) textW = Math.max(textW, font.width(streetLine));
            int boxW = textW + 14;
            int boxH = holdingAdmin ? 38 : 27;
            guiGraphics.fill(panelX, panelY, panelX + boxW, panelY + boxH, 0xCC121822);
            guiGraphics.renderOutline(panelX, panelY, boxW, boxH, 0xFFD9823B);
            guiGraphics.drawString(font, title, panelX + 6, panelY + 4, 0xFFF2E6D8, true);

            int hereCol = 0xFF000000 | HeatVisionRenderer.temperatureToRgb(EternalSnowState.clientLocalTempC);
            guiGraphics.drawString(font, hereLine, panelX + 6, panelY + 15, hereCol, true);
            if (streetLine != null) {
                int streetCol = 0xFF000000 | HeatVisionRenderer.temperatureToRgb(EternalSnowState.clientStreetTempC);
                guiGraphics.drawString(font, streetLine, panelX + 6, panelY + 26, streetCol, true);
            }
            panelY += boxH + 4;
        }

        if (holdingScanner) {
            boolean relative = scannerMode == HeatScannerItem.MODE_WALLS
                    || scannerMode == HeatScannerItem.MODE_THERMAL;
            Component modeTitle = Component.translatable(HeatScannerItem.getModeHudTranslationKey(scannerMode));
            String localStr = String.format(Locale.ROOT, "%+.1f°C", EternalSnowState.clientLocalTempC);
            Component localLine = relative
                    ? Component.translatable("eternalsnow.scanner.hud.local_rel",
                            String.format(Locale.ROOT, "%+.1f°C", HeatScanClientData.minC),
                            String.format(Locale.ROOT, "%+.1f°C", HeatScanClientData.maxC))
                    : Component.translatable("eternalsnow.scanner.hud.local", localStr);

            int boxW = Math.max(156, Math.max(font.width(modeTitle), font.width(localLine)) + 14);
            int boxH = 38;
            guiGraphics.fill(panelX, panelY, panelX + boxW, panelY + boxH, 0xCC0F1624);
            guiGraphics.renderOutline(panelX, panelY, boxW, boxH, 0xFF45C0FF);
            guiGraphics.drawString(font, modeTitle, panelX + 6, panelY + 4, 0xFF88E4FF, true);
            guiGraphics.drawString(font, localLine, panelX + 6, panelY + 14, 0xFFEAF7FF, true);

            int barLeft = panelX + 6;
            int barRight = panelX + boxW - 6;
            int barTop = panelY + 26;
            int steps = 32;
            float stepW = (barRight - barLeft) / (float) steps;
            for (int i = 0; i < steps; i++) {
                float barLo = relative ? HeatScanClientData.minC : -25.0F;
                float barHi = relative ? HeatScanClientData.maxC : 55.0F;
                float t = Mth.lerp(i / (float) (steps - 1), barLo, barHi);
                int col = 0xFF000000 | (relative
                        ? HeatVisionRenderer.relativeToRgb((t - barLo) / Math.max(0.001F, barHi - barLo))
                        : HeatVisionRenderer.temperatureToRgb(t));
                int x0 = barLeft + Math.round(i * stepW);
                int x1 = Math.max(x0 + 1, barLeft + Math.round((i + 1) * stepW));
                guiGraphics.fill(x0, barTop, x1, barTop + 5, col);
            }
            guiGraphics.renderOutline(barLeft - 1, barTop - 1, (barRight - barLeft) + 2, 7, 0xAA101820);
        }
    }

    /** v2.6.0: wall thermometer readout ONLY on crosshair hover, with min/max and trend arrow. */
    private static void renderWallThermometerHud(Minecraft mc, GuiGraphics g, int screenW, int screenH,
                                                 WallThermometerBlockEntity be) {
        Font font = mc.font;
        float cur = be.getLocalTempC();
        float trend = be.getTrendC();
        String arrow = trend > 0.30F ? "▲" : (trend < -0.30F ? "▼" : "▬");
        String range = String.format(Locale.ROOT, "%+.1f ... %+.1f°C", be.getMinTempC(), be.getMaxTempC());
        String trendStr = String.format(Locale.ROOT, "%+.1f°C", trend);

        Component l1 = Component.translatable("eternalsnow.wall_thermometer.hud",
                String.format(Locale.ROOT, "%+.1f°C", cur));
        Component l2 = Component.translatable("eternalsnow.wall_thermometer.range", range);
        Component l3 = Component.translatable("eternalsnow.wall_thermometer.trend", arrow, trendStr);

        int boxW = Math.max(font.width(l1), Math.max(font.width(l2), font.width(l3))) + 14;
        int boxH = 44;
        int boxX = (screenW - boxW) / 2;
        int boxY = screenH / 2 + 12;
        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xCC101822);
        g.renderOutline(boxX, boxY, boxW, boxH, 0xFFD9823B);
        int rgb = 0xFF000000 | HeatVisionRenderer.temperatureToRgb(cur);
        g.drawString(font, l1, boxX + 6, boxY + 4, rgb, true);
        g.drawString(font, l2, boxX + 6, boxY + 16, 0xFFC6D6E6, true);
        g.drawString(font, l3, boxX + 6, boxY + 28,
                trend > 0.30F ? 0xFFFF9A4A : (trend < -0.30F ? 0xFF69B6FF : 0xFFB8C4CE), true);
    }

    /**
     * Cheap "shader" mode: one coarse 12x8 translucent colour pass over the screen.
     * Colours are precomputed ONCE per 2-second server packet in HeatScanClientData — no per-frame
     * block iteration, no chunk rebuilds and no GLSL program for the weak client.
     */
    private static void renderThermalScreen(GuiGraphics g, int screenW, int screenH) {
        int[] bins = HeatScanClientData.thermalBins;
        int cols = HeatScanClientData.THERMAL_COLS;
        int rows = HeatScanClientData.THERMAL_ROWS;
        float cw = screenW / (float) cols;
        float ch = screenH / (float) rows;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int argb = bins[r * cols + c];
                if (argb == 0) {
                    continue;
                }
                int x0 = Math.round(c * cw);
                int y0 = Math.round(r * ch);
                int x1 = Math.max(x0 + 1, Math.round((c + 1) * cw));
                int y1 = Math.max(y0 + 1, Math.round((r + 1) * ch));
                g.fill(x0, y0, x1, y1, argb);
            }
        }
    }

    /** Two HUD dots: hottest and coldest. World-anchored, so turning away leaves them behind. */
    private static void renderThermalMarkers(GuiGraphics g, int screenW, int screenH, Font font) {
        if (HeatScanClientData.hotVisible) {
            drawThermalMarker(g, font, HeatScanClientData.hotScreenX, HeatScanClientData.hotScreenY,
                    0xFFFF4538, "▲", HeatScanClientData.hotC, screenW, screenH);
        }
        if (HeatScanClientData.coldVisible) {
            drawThermalMarker(g, font, HeatScanClientData.coldScreenX, HeatScanClientData.coldScreenY,
                    0xFF4B9BFF, "▼", HeatScanClientData.coldC, screenW, screenH);
        }
    }

    private static void drawThermalMarker(GuiGraphics g, Font font, float sx, float sy, int color,
                                          String glyph, float tempC, int screenW, int screenH) {
        int x = Math.round(sx);
        int y = Math.round(sy);
        if (x < 2 || y < 2 || x > screenW - 2 || y > screenH - 2) {
            return; // behind the camera / off-screen — stays anchored to its world position
        }
        g.fill(x - 4, y - 4, x + 4, y + 4, 0xDD101820);
        g.renderOutline(x - 4, y - 4, 8, 8, color);
        g.fill(x - 2, y - 2, x + 2, y + 2, color);
        String label = glyph + " " + String.format(Locale.ROOT, "%+.1f°C", tempC);
        int lw = font.width(label);
        int lx = Mth.clamp(x + 8, 2, screenW - lw - 2);
        int ly = Mth.clamp(y - 12, 2, screenH - 12);
        g.fill(lx - 2, ly - 1, lx + lw + 2, ly + 10, 0xAA0B111A);
        g.drawString(font, label, lx, ly, color, true);
    }

    private static void renderFirestarterRubHud(
            Minecraft mc,
            GuiGraphics g,
            float partialTick,
            int screenW,
            int screenH,
            LocalPlayer player
    ) {
        float remTicks = Math.max(0.0F, player.getUseItemRemainingTicks() - partialTick);
        float elapsedTicks = Mth.clamp(WoodenFirestarterItem.USE_DURATION_TICKS - remTicks, 0.0F, WoodenFirestarterItem.USE_DURATION_TICKS);
        float progress = elapsedTicks / (float) WoodenFirestarterItem.USE_DURATION_TICKS;
        String secStr = String.format(Locale.ROOT, "%.1f", elapsedTicks / 20.0F);

        Component title = Component.translatable("eternalsnow.hud.firestarter.rubbing", secStr);
        int textW = mc.font.width(title);
        int boxW = Math.max(textW + 14, 154);
        int boxH = 22;
        int x = (screenW - boxW) / 2;
        int y = screenH / 2 + 12;

        g.fill(x, y, x + boxW, y + boxH, 0xC812161E);
        int borderCol = progress > 0.75F ? 0xFFFFB84D : 0xFFD9823B;
        g.renderOutline(x, y, boxW, boxH, borderCol);
        g.drawString(mc.font, title, (screenW - textW) / 2, y + 3, 0xFFFFE6B8, true);

        int barX = x + 6;
        int barY = y + 14;
        int barW = boxW - 12;
        g.fill(barX, barY, barX + barW, barY + 4, 0xAA202834);
        int filledW = Math.max(1, Math.round(barW * progress));
        int fillCol = progress > 0.80F ? 0xFFFFDF59 : (progress > 0.45F ? 0xFFFF9A3C : 0xFFD97336);
        g.fill(barX, barY, barX + filledW, barY + 4, fillCol);
    }

    private static void renderLookAtFireHud(Minecraft mc, GuiGraphics g, int screenW, int screenH, BlockPos hitPos) {
        if (mc.level == null) {
            return;
        }
        BlockState hitState = mc.level.getBlockState(hitPos);

        // A0. Looking at a Brick Chimney block -> show connected damper % & click hint!
        if (hitState.getBlock() instanceof ChimneyBlock) {
            BlockPos connectedMaster = FireplaceBlockEntity.findConnectedFireplaceMaster(mc.level, hitPos);
            if (connectedMaster != null && mc.level.getBlockEntity(connectedMaster) instanceof FireplaceBlockEntity be) {
                int damper = be.getDamperPercent();
                Component line1 = (damper <= 0)
                        ? Component.translatable("eternalsnow.hud.chimney.closed")
                        : Component.translatable("eternalsnow.hud.chimney.open", damper);
                Component line2 = Component.translatable("eternalsnow.hud.chimney.click_hint");
                drawCenteredFireTooltip(mc, g, screenW, screenH, line1, line2, damper / 100.0F, damper > 0 && be.isLit(),
                        damper <= 0 ? 0xCCFF6655 : 0xCC8CD4FF);
            }
            return;
        }

        // A. 3D Fireplace (master or any of its 3x2x2 parts)
        BlockPos fireplaceMaster = null;
        if (hitState.getBlock() instanceof FireplaceBlock) {
            fireplaceMaster = hitPos;
        } else if (hitState.getBlock() instanceof FireplacePartBlock) {
            fireplaceMaster = FireplacePartBlock.getMasterPos(hitPos, hitState);
        }

        if (fireplaceMaster != null && mc.level.getBlockEntity(fireplaceMaster) instanceof FireplaceBlockEntity be) {
            BlockState ms = mc.level.getBlockState(fireplaceMaster);
            boolean lit = ms.getBlock() instanceof FireplaceBlock && ms.getValue(FireplaceBlock.LIT);
            int fuelMilli = be.getFuelMilli();
            int damper = be.getDamperPercent();
            boolean chimneyOk = be.isChimneyValid();

            // Fixed 0..100% fuel bar fill — NEVER shifts when moving damper!
            float fillRatio = FireplaceFuelHelper.getFillRatio(fuelMilli);
            int fillPct = FireplaceFuelHelper.getFillPercentRounded(fuelMilli);
            int calcTicks = FireplaceFuelHelper.computeRemainingRealTicks(fuelMilli, Math.max(1, damper));

            Component line1;
            Component line2 = null;
            int borderCol;

            if (!chimneyOk) {
                line1 = Component.translatable("eternalsnow.hud.fireplace.no_chimney");
                if (fuelMilli > 0) {
                    line2 = Component.translatable("eternalsnow.hud.fireplace.fuel_stored_pct", fillPct);
                }
                borderCol = 0xCCFF6655;
            } else if (damper <= 0) {
                line1 = Component.translatable("eternalsnow.hud.fireplace.damper_closed");
                if (fuelMilli > 0) {
                    line2 = Component.translatable("eternalsnow.hud.fireplace.fuel_stored_pct", fillPct);
                }
                borderCol = 0xCCFF7755;
            } else if (lit) {
                line1 = Component.translatable(
                        "eternalsnow.hud.fireplace.burning_damper",
                        FireplaceFuelHelper.formatTicksMMSS(calcTicks),
                        damper,
                        fillPct
                );
                line2 = Component.translatable("eternalsnow.hud.fireplace.extinguish_hint");
                borderCol = 0xCCFFA64D;
            } else if (fuelMilli > 0) {
                line1 = Component.translatable(
                        "eternalsnow.hud.fireplace.ready_damper",
                        fillPct,
                        FireplaceFuelHelper.formatTicksMMSS(calcTicks),
                        damper
                );
                borderCol = 0xCC8CD4FF;
            } else {
                line1 = Component.translatable("eternalsnow.hud.fireplace.empty");
                borderCol = 0x997B90A8;
            }

            drawCenteredFireTooltip(mc, g, screenW, screenH, line1, line2, fillRatio, lit, borderCol);
            return;
        }

        // B. Stick-Fueled Campfire / Soul Campfire
        if (hitState.getBlock() instanceof CampfireBlock) {
            boolean lit = hitState.getValue(CampfireBlock.LIT);
            int syncedTicks = (EternalSnowState.clientLookedCampfirePos == hitPos.asLong())
                    ? EternalSnowState.clientLookedCampfireTicks
                    : 0;
            boolean stormWind = (EternalSnowState.clientLookedCampfirePos == hitPos.asLong())
                    && EternalSnowState.clientLookedCampfireWind;
            int sticksApprox = Mth.clamp((syncedTicks + CampfireFuelManager.TICKS_PER_STICK - 1) / CampfireFuelManager.TICKS_PER_STICK, 0, CampfireFuelManager.MAX_STICKS);
            float fillRatio = Mth.clamp(syncedTicks / (float) CampfireFuelManager.MAX_FUEL_TICKS, 0.0F, 1.0F);

            Component line1;
            Component line2 = null;
            int borderCol;

            if (lit) {
                line1 = Component.translatable("eternalsnow.hud.campfire.burning",
                        FireplaceFuelHelper.formatTicksMMSS(syncedTicks), sticksApprox, CampfireFuelManager.MAX_STICKS);
                if (stormWind) {
                    line2 = Component.translatable("eternalsnow.hud.campfire.wind_warning");
                    borderCol = 0xCCFF7755;
                } else {
                    borderCol = 0xCCFFA64D;
                }
            } else if (syncedTicks > 0) {
                line1 = Component.translatable("eternalsnow.hud.campfire.ready",
                        sticksApprox, CampfireFuelManager.MAX_STICKS, FireplaceFuelHelper.formatTicksMMSS(syncedTicks));
                borderCol = 0xCC8CD4FF;
            } else {
                line1 = Component.translatable("eternalsnow.hud.campfire.empty", CampfireFuelManager.MAX_STICKS);
                borderCol = 0x997B90A8;
            }

            drawCenteredFireTooltip(mc, g, screenW, screenH, line1, line2, fillRatio, lit, borderCol);
        }
    }

    private static void drawCenteredFireTooltip(
            Minecraft mc,
            GuiGraphics g,
            int screenW,
            int screenH,
            Component line1,
            Component line2,
            float fillRatio,
            boolean lit,
            int borderColor
    ) {
        int w1 = mc.font.width(line1);
        int w2 = (line2 != null) ? mc.font.width(line2) : 0;
        int boxW = Math.max(Math.max(w1, w2) + 12, 110);
        int boxH = (line2 != null ? 24 : 14) + (fillRatio > 0.0F ? 5 : 0);

        int x = (screenW - boxW) / 2;
        int y = screenH / 2 + 12;

        g.fill(x, y, x + boxW, y + boxH, 0xB8101620);
        g.renderOutline(x, y, boxW, boxH, borderColor);

        int textY = y + 3;
        g.drawString(mc.font, line1, (screenW - w1) / 2, textY, lit ? 0xFFFFE3A8 : 0xFFE2F1FF, true);
        if (line2 != null) {
            textY += 10;
            g.drawString(mc.font, line2, (screenW - w2) / 2, textY, 0xFFB8C8D8, true);
        }
        if (fillRatio > 0.0F) {
            int barX = x + 5;
            int barY = y + boxH - 4;
            int barW = boxW - 10;
            g.fill(barX, barY, barX + barW, barY + 2, 0x80222C38);
            int filledW = Math.max(1, Math.round(barW * fillRatio));
            g.fill(barX, barY, barX + filledW, barY + 2, lit ? 0xFFFF9638 : 0xFF6CC4FF);
        }
    }
}
