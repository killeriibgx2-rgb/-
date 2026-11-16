package com.eternalsnow.client;

import com.eternalsnow.block.CocoaPotBlockEntity;
import com.eternalsnow.menu.CocoaPotMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class CocoaPotScreen extends AbstractContainerScreen<CocoaPotMenu> {

    public CocoaPotScreen(CocoaPotMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Clean winter-slate & warm-copper container window
        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xEE18202B);
        g.renderOutline(x, y, this.imageWidth, this.imageHeight, 0xFFC87D44);
        g.renderOutline(x + 1, y + 1, this.imageWidth - 2, this.imageHeight - 2, 0x66405A74);

        // Top cooking workspace panel
        g.fill(x + 6, y + 17, x + this.imageWidth - 6, y + 71, 0xCC0E151E);
        g.renderOutline(x + 6, y + 17, this.imageWidth - 12, 54, 0xFF354C63);

        // Render slot frames for all 39 slots (3 pot slots + 36 player inventory slots)
        for (int i = 0; i < this.menu.slots.size(); i++) {
            Slot s = this.menu.slots.get(i);
            int sx = x + s.x - 1;
            int sy = y + s.y - 1;
            int borderColor = (i < 3) ? 0xFFD98A48 : 0xFF3E556C;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF101822);
            g.renderOutline(sx, sy, 18, 18, borderColor);
        }

        // Mini slot labels above the 3 input slots: [Молоко] [Вода] [x4]
        g.drawCenteredString(this.font, Component.literal("М"), x + 34, y + 18, 0xFFEAF4FF);
        g.drawCenteredString(this.font, Component.literal("В"), x + 58, y + 18, 0xFF6CC6FF);
        g.drawCenteredString(this.font, Component.literal("×4"), x + 82, y + 18, 0xFFFFCC77);

        // Cooking progress bar (x+96..122, y+30..38)
        int progX = x + 96;
        int progY = y + 30;
        g.fill(progX, progY, progX + 26, progY + 8, 0xFF0A1016);
        g.renderOutline(progX, progY, 26, 8, 0xFF4C6882);
        int cookProgress = this.menu.getCookProgress();
        if (cookProgress > 0) {
            int fillW = Math.min(24, (cookProgress * 24) / CocoaPotBlockEntity.COOK_TIME_TICKS);
            g.fill(progX + 1, progY + 1, progX + 1 + fillW, progY + 7, 0xFFFFA838);
        }

        // Visual internal drink reservoir tank (0/3 .. 3/3 servings) at right side (x+128..162, y+21..53)
        int tankX = x + 128;
        int tankY = y + 21;
        int tankW = 34;
        int tankH = 30;
        g.fill(tankX, tankY, tankX + tankW, tankY + tankH, 0xFF090E14);
        g.renderOutline(tankX, tankY, tankW, tankH, 0xFF9E6B40);

        int servings = this.menu.getServings();
        int drinkType = this.menu.getDrinkType();
        if (servings > 0 || cookProgress > 0) {
            int liquidColor = (drinkType == CocoaPotBlockEntity.DRINK_CARAMEL) ? 0xFFE29434 : 0xFF6B3E23;
            int topHighlight = (drinkType == CocoaPotBlockEntity.DRINK_CARAMEL) ? 0xFFF7BC64 : 0xFF8C5534;
            int fillH = servings > 0
                    ? (servings * (tankH - 2)) / CocoaPotBlockEntity.MAX_SERVINGS
                    : Math.max(3, (cookProgress * (tankH - 2)) / CocoaPotBlockEntity.COOK_TIME_TICKS);
            int ly = tankY + tankH - 1 - fillH;
            g.fill(tankX + 1, ly, tankX + tankW - 1, tankY + tankH - 1, liquidColor);
            g.fill(tankX + 1, ly, tankX + tankW - 1, ly + 1, topHighlight);
        }

        // 3 division tick marks on tank
        g.fill(tankX + 1, tankY + 10, tankX + 5, tankY + 11, 0x88FFFFFF);
        g.fill(tankX + 1, tankY + 20, tankX + 5, tankY + 21, 0x88FFFFFF);

        // Status line at bottom of workspace: Lit Furnace indicator + servings readout
        boolean heated = this.menu.isHeatedBelow();
        Component heatLine = heated
                ? Component.translatable("eternalsnow.pot.gui.heated_ok")
                : Component.translatable("eternalsnow.pot.gui.need_furnace");
        g.drawString(this.font, heatLine, x + 10, y + 48, heated ? 0xFFFFB248 : 0xFFFF7262, false);

        Component recipeHint = (servings > 0)
                ? Component.translatable(
                        drinkType == CocoaPotBlockEntity.DRINK_CARAMEL
                                ? "eternalsnow.pot.gui.ready_caramel"
                                : "eternalsnow.pot.gui.ready_cocoa",
                        servings,
                        CocoaPotBlockEntity.MAX_SERVINGS
                )
                : Component.translatable("eternalsnow.pot.gui.recipe_hint");
        g.drawString(this.font, recipeHint, x + 10, y + 59, servings > 0 ? 0xFF88F0A8 : 0xFF9CC2DE, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFDEC0, false);
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFB8CFE4, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }
}
