package com.eternalsnow.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Clean, Vanilla-Minecraft & Frosty-Snow UI widgets (stone bevels, soft snow-cap trim, crisp neutral typography — zero neon!).
 */
public final class EternalSnowWidgets {
    private EternalSnowWidgets() {
    }

    public static void drawVanillaSnowPanel(GuiGraphics g, int x, int y, int w, int h, int fillArgb, boolean snowCap) {
        // Outer 1px charcoal stone border
        g.fill(x, y, x + w, y + h, 0xFF16181C);
        // Inner stone surface
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fillArgb);
        // Subtle vanilla 3D bevel (light top/left, dark bottom/right)
        g.fill(x + 1, y + 1, x + w - 1, y + 2, snowCap ? 0xFFEAF0F5 : 0xFF9EA6B0);
        g.fill(x + 1, y + 1, x + 2, y + h - 2, 0xFF8C949E);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF33373D);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, 0xFF33373D);

        if (snowCap && h >= 18) {
            // Soft 2px snow layer resting along top edge
            g.fill(x + 1, y + 1, x + w - 1, y + 3, 0xFFF0F5FA);
        }
    }

    /**
     * Top Tab button (Client | Admin): Vanilla stone button when inactive, crisp snow-dusted raised tab when active.
     */
    public static final class TabButton extends AbstractButton {
        private final BooleanSupplier selectedSupplier;
        private final Runnable onClick;

        public TabButton(int x, int y, int width, int height, Component message, BooleanSupplier selectedSupplier, Runnable onClick) {
            super(x, y, width, height, message);
            this.selectedSupplier = selectedSupplier;
            this.onClick = onClick;
        }

        @Override
        public void onPress() {
            if (this.active) {
                this.onClick.run();
            }
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            boolean selected = this.selectedSupplier.getAsBoolean();
            boolean hovered = this.isHoveredOrFocused() && this.active;

            int body = !this.active ? 0xFF34383E : (selected ? 0xFF6E7782 : (hovered ? 0xFF626A74 : 0xFF494F57));
            drawVanillaSnowPanel(g, this.getX(), this.getY(), this.width, this.height, body, selected);

            if (hovered && !selected) {
                g.renderOutline(this.getX(), this.getY(), this.width, this.height, 0xFFE2E8EE);
            }

            int textColor = !this.active ? 0xFF787E86 : (selected ? 0xFFFFFFFF : (hovered ? 0xFFF5F8FA : 0xFFD0D7DE));
            g.drawCenteredString(font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2 + (selected ? 1 : 0), textColor);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            this.defaultButtonNarrationText(narration);
        }
    }

    /**
     * Category Folder Card inside Admin Menu: Snow-capped dark stone plaque with clean subtitle & '›' chevron.
     */
    public static final class FolderButton extends AbstractButton {
        private final Component subtitle;
        private final Runnable onClick;

        public FolderButton(int x, int y, int width, int height, Component title, Component subtitle, Runnable onClick) {
            super(x, y, width, height, title);
            this.subtitle = subtitle;
            this.onClick = onClick;
        }

        @Override
        public void onPress() {
            if (this.active) {
                this.onClick.run();
            }
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            boolean hovered = this.isHoveredOrFocused() && this.active;

            int body = hovered ? 0xFF565F69 : 0xFF434A52;
            drawVanillaSnowPanel(g, this.getX(), this.getY(), this.width, this.height, body, true);
            if (hovered) {
                g.renderOutline(this.getX(), this.getY(), this.width, this.height, 0xFFE8EEF4);
            }

            int titleCol = hovered ? 0xFFFFFFFF : 0xFFF0F4F8;
            int subCol = hovered ? 0xFFD4DCE4 : 0xFFB0BAC4;

            if (this.subtitle != null && !this.subtitle.getString().isEmpty() && this.height >= 24) {
                g.drawString(font, this.getMessage(), this.getX() + 8, this.getY() + 5, titleCol, true);
                String trimmedSub = font.plainSubstrByWidth(this.subtitle.getString(), Math.max(40, this.width - 28));
                g.drawString(font, trimmedSub, this.getX() + 8, this.getY() + 15, subCol, false);
            } else {
                g.drawString(font, this.getMessage(), this.getX() + 8, this.getY() + (this.height - 8) / 2, titleCol, true);
            }

            g.drawString(font, "›", this.getX() + this.width - 10, this.getY() + (this.height - 8) / 2, titleCol, true);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            this.defaultButtonNarrationText(narration);
        }
    }

    /**
     * Standard Vanilla-Snowy Beveled Button (used on Pause Menu, Back buttons, Done/Cancel, Quick Weather).
     */
    public static final class ActionButton extends AbstractButton {
        private final Runnable onClick;
        private final boolean snowTrim;

        public ActionButton(int x, int y, int width, int height, Component label, Runnable onClick) {
            this(x, y, width, height, label, onClick, false);
        }

        public ActionButton(int x, int y, int width, int height, Component label, Runnable onClick, boolean snowTrim) {
            super(x, y, width, height, label);
            this.onClick = onClick;
            this.snowTrim = snowTrim;
        }

        @Override
        public void onPress() {
            if (this.active) {
                this.onClick.run();
            }
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            boolean hovered = this.isHoveredOrFocused() && this.active;

            int body = !this.active ? 0xFF32363B : (hovered ? 0xFF6B7480 : 0xFF555C65);
            drawVanillaSnowPanel(g, this.getX(), this.getY(), this.width, this.height, body, this.snowTrim);
            if (hovered) {
                g.renderOutline(this.getX(), this.getY(), this.width, this.height, 0xFFF0F5FA);
            }

            int txtCol = !this.active ? 0xFF828890 : (hovered ? 0xFFFFFFFF : 0xFFEAEFF5);
            g.drawCenteredString(font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, txtCol);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            this.defaultButtonNarrationText(narration);
        }
    }

    /**
     * Calm Vanilla-Snowy ON / OFF Switch Row: Stone plate with a clean recessed toggle switch (zero neon!).
     */
    public static final class ToggleSwitch extends AbstractButton {
        private boolean state;
        private final Consumer<Boolean> onToggle;

        public ToggleSwitch(int x, int y, int width, int height, Component label, boolean initialState, Consumer<Boolean> onToggle) {
            super(x, y, width, height, label);
            this.state = initialState;
            this.onToggle = onToggle;
        }

        @Override
        public void onPress() {
            if (this.active) {
                this.state = !this.state;
                this.onToggle.accept(this.state);
            }
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            boolean hovered = this.isHoveredOrFocused() && this.active;

            int rowBg = hovered ? 0xE6464D56 : 0xD8383E46;
            int rowBorder = hovered ? 0xFFD0D8E0 : 0xFF202428;
            g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, rowBg);
            g.renderOutline(this.getX(), this.getY(), this.width, this.height, rowBorder);

            int pillW = 46;
            int pillH = this.height - 6;
            int pillX = this.getX() + this.width - pillW - 4;
            int pillY = this.getY() + 3;

            // Calm snowy-pine muted green when ON, dark stone when OFF
            int pillBg = this.state ? 0xFF486352 : 0xFF282C32;
            int pillBorder = this.state ? 0xFFC8E0CE : 0xFF666E78;
            g.fill(pillX, pillY, pillX + pillW, pillY + pillH, pillBg);
            g.renderOutline(pillX, pillY, pillW, pillH, pillBorder);

            int knobX = this.state ? (pillX + pillW - 8) : (pillX + 3);
            int knobCol = this.state ? 0xFFF2F7FA : 0xFF969EA6;
            g.fill(knobX, pillY + 2, knobX + 5, pillY + pillH - 2, knobCol);

            String stateTxt = this.state ? "ВКЛ" : "ВЫКЛ";
            int stateTxtX = this.state ? (pillX + 6) : (pillX + 11);
            int stateTxtCol = this.state ? 0xFFF2F7FA : 0xFFB4BCC4;
            g.drawString(font, stateTxt, stateTxtX, pillY + (pillH - 8) / 2 + 1, stateTxtCol, false);

            int maxLabelW = pillX - this.getX() - 10;
            String lbl = font.plainSubstrByWidth(this.getMessage().getString(), Math.max(20, maxLabelW));
            g.drawString(font, lbl, this.getX() + 6, this.getY() + (this.height - 8) / 2, hovered ? 0xFFFFFFFF : 0xFFE6ECF2, true);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            this.defaultButtonNarrationText(narration);
        }
    }

    /**
     * Multi-Choice Mode Selector Row in calm vanilla stone & frost silver.
     */
    public static final class ModeSelectorButton extends AbstractButton {
        private final int[] values;
        private int index;
        private final Function<Integer, Component> valueLabeler;
        private final Consumer<Integer> onSelect;

        public ModeSelectorButton(
                int x, int y, int width, int height,
                Component label, int[] values, int initialValue,
                Function<Integer, Component> valueLabeler,
                Consumer<Integer> onSelect
        ) {
            super(x, y, width, height, label);
            this.values = values;
            this.valueLabeler = valueLabeler;
            this.onSelect = onSelect;
            this.index = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i] == initialValue) {
                    this.index = i;
                    break;
                }
            }
        }

        @Override
        public void onPress() {
            if (this.active) {
                this.index = (this.index + 1) % this.values.length;
                this.onSelect.accept(this.values[this.index]);
            }
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            boolean hovered = this.isHoveredOrFocused() && this.active;

            g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, hovered ? 0xE6464D56 : 0xD8383E46);
            g.renderOutline(this.getX(), this.getY(), this.width, this.height, hovered ? 0xFFD0D8E0 : 0xFF202428);

            Component modeComp = this.valueLabeler.apply(this.values[this.index]);
            int badgeW = Math.min(this.width / 2, font.width(modeComp) + 14);
            int badgeX = this.getX() + this.width - badgeW - 4;
            int badgeY = this.getY() + 3;
            int badgeH = this.height - 6;

            g.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 0xFF525A64);
            g.renderOutline(badgeX, badgeY, badgeW, badgeH, 0xFFC8D2DC);
            g.drawCenteredString(font, modeComp, badgeX + badgeW / 2, badgeY + (badgeH - 8) / 2 + 1, 0xFFF4F8FC);

            int maxLblW = badgeX - this.getX() - 10;
            String lbl = font.plainSubstrByWidth(this.getMessage().getString(), Math.max(20, maxLblW));
            g.drawString(font, lbl, this.getX() + 6, this.getY() + (this.height - 8) / 2, 0xFFE6ECF2, true);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            this.defaultButtonNarrationText(narration);
        }
    }

    /**
     * Calm Snowy-White Section Divider Header.
     */
    public static final class SectionHeader extends AbstractWidget {
        public SectionHeader(int x, int y, int width, Component title) {
            super(x, y, width, 16, title);
            this.active = false;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            int textW = font.width(this.getMessage());
            int yMid = this.getY() + 9;
            g.drawString(font, this.getMessage(), this.getX() + 2, this.getY() + 4, 0xFFEDF2F7, true);
            int lineStart = this.getX() + textW + 8;
            int lineEnd = this.getX() + this.width - 2;
            if (lineEnd > lineStart) {
                g.fill(lineStart, yMid, lineEnd, yMid + 1, 0x888E98A4);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }
}
