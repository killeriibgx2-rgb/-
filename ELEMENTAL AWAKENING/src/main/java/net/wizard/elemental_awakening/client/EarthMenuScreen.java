package net.wizard.elemental_awakening.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.wizard.elemental_awakening.ElementalAwakening;

public class EarthMenuScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation(ElementalAwakening.MOD_ID, "textures/gui/earth_menu.png");
    private static final ResourceLocation ICON_NORMAL = new ResourceLocation(ElementalAwakening.MOD_ID, "textures/gui/stena.png");
    private static final ResourceLocation ICON_HOVER = new ResourceLocation(ElementalAwakening.MOD_ID, "textures/gui/stena_hover.png");

    private final int menuWidth = 512;
    private final int menuHeight = 512;

    public EarthMenuScreen() {
        super(Component.empty());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int x = (this.width - menuWidth) / 2;
        int y = (this.height - menuHeight) / 2;

        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, x, y, 0, 0, menuWidth, menuHeight, 512, 512);

        int relativeMouseX = mouseX - x;
        int relativeMouseY = mouseY - y;

        boolean isHovered = relativeMouseX >= 236 && relativeMouseX <= 276 && relativeMouseY >= 60 && relativeMouseY <= 100;
        ResourceLocation currentIcon = isHovered ? ICON_HOVER : ICON_NORMAL;

        RenderSystem.setShaderTexture(0, currentIcon);
        guiGraphics.blit(currentIcon, x + 236, y + 60, 0, 0, 40, 40, 40, 40);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = (this.width - menuWidth) / 2;
            int y = (this.height - menuHeight) / 2;

            double clickX = mouseX - x;
            double clickY = mouseY - y;

            if (clickX >= 236 && clickX <= 276 && clickY >= 60 && clickY <= 100) {
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
