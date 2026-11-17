package com.eternalsnow.client;

import com.eternalsnow.mixin.SlotPositionAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.Slot;

/**
 * A skin of the vanilla furnace, not a new container protocol. Keeping the original menu and
 * Slot objects preserves shift-click, hotbar swaps, recipe placement and result XP on both sides.
 * The recipe book reads the relocated Slot coordinates, including its ghost ingredients.
 */
public final class CozyFurnaceScreen extends FurnaceScreen {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("eternalsnow", "textures/gui/cozy_furnace.png");

    public CozyFurnaceScreen(FurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        // Slot coordinates are the 16px ITEM interior, i.e. texture border + 1, not -8.
        move(0, 72, 27);
        move(1, 72, 56);
        // Non-food still smelts into slot 2. Never hide it: old saves and automation can
        // leave an output there even while food is in slot 0.
        move(2, 137, 44);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                move(3 + row * 9 + col, 9 + col * 18, 85 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            move(30 + col, 9 + col * 18, 143);
        }
        super.init(); // retains recipe book, narrow-screen handling and button positioning
    }

    private void move(int index, int x, int y) {
        SlotPositionAccessor slot = (SlotPositionAccessor) this.menu.getSlot(index);
        slot.eternalsnow$setX(x);
        slot.eternalsnow$setY(y);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // No inventory caption: its vanilla baseline overlaps the new inventory row.
        graphics.drawString(this.font, this.title, this.titleLabelX, 7, 0x483828, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        // The file is 176x166, NOT vanilla's 256x256 atlas. Explicit dimensions avoid UV stretch.
        graphics.blit(TEXTURE, x, y, 0, 0, 176, 166, 176, 166);
        Slot result = this.menu.getSlot(2);
        // Reuse the hand-drawn input frame for the vanilla output. No new artwork needed.
        graphics.blit(TEXTURE, x + result.x - 1, y + result.y - 1,
                71, 26, 18, 18, 176, 166);

        int progress = Mth.clamp(this.menu.getBurnProgress(), 0, 24);
        int height = progress * 46 / 24;
        if (height > 0) {
            graphics.fill(x + 110, y + 72 - height, x + 118, y + 72, 0xFFE08E3E);
            graphics.fill(x + 110, y + 72 - height, x + 112, y + 72, 0xFFFFD084);
        }
        // Static texture has coals; cover them when cold and use synced fuel data when lit.
        graphics.fill(x + 73, y + 46, x + 87, y + 53, 0xFF342822);
        if (this.menu.isLit()) {
            int fuel = Mth.clamp(this.menu.getLitProgress(), 0, 13);
            int width = Math.max(1, fuel);
            graphics.fill(x + 73, y + 50, x + 73 + width, y + 52, 0xFFE08E3E);
            graphics.fill(x + 73, y + 50, x + 73 + width, y + 51, 0xFFFFD084);
        }
    }
}
