package net.wizard.elemental_awakening.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.wizard.elemental_awakening.ElementalAwakening;

public class FireMenuScreen extends Screen {
    // Твоя новая пережатая картинка Огня
    private static final ResourceLocation TEXTURE = new ResourceLocation(ElementalAwakening.MOD_ID, "textures/gui/fire_menu.png");

    private final int menuWidth = 256;
    private final int menuHeight = 256;

    public FireMenuScreen() {
        super(Component.empty());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int x = (this.width - menuWidth) / 2;
        int y = (this.height - menuHeight) / 2;

        RenderSystem.setShaderTexture(0, TEXTURE);
        // Исправлено: рисуем пиксель в пиксель без сжатия под файл 256х256
        guiGraphics.blit(TEXTURE, x, y, 0, 0, menuWidth, menuHeight, 256, 256);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
