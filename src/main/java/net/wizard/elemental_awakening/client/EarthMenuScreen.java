package net.wizard.elemental_awakening.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.wizard.elemental_awakening.ElementalAwakening;

public class EarthMenuScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(ElementalAwakening.MOD_ID, "textures/gui/earth_menu.png");
    private static final ResourceLocation BUTTON_NORMAL = new ResourceLocation(ElementalAwakening.MOD_ID, "textures/gui/stena.png");
    private static final ResourceLocation BUTTON_HOVER = new ResourceLocation(ElementalAwakening.MOD_ID, "textures/gui/stena_hover.png");

    private final int imageWidth = 256;
    private final int imageHeight = 166;

    public EarthMenuScreen() {
        super(Component.literal("Магия Земли"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Рисуем затемнение заднего фона игры
        this.renderBackground(guiGraphics);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Находим центр экрана
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 2. Рисуем твою широкую земляную плиту фона
        RenderSystem.setShaderTexture(0, BACKGROUND);
        guiGraphics.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

        // 3. Высчитываем координаты квадрата нашей кнопки стены (40x40 пикселей) по центру сверху плиты
        int btnX = x + (this.imageWidth / 2) - 20;
        int btnY = y + 20;
        int btnSize = 40;

        // Проверяем: находится ли курсор мыши в данный момент внутри нашей кнопки?
        boolean isHovered = mouseX >= btnX && mouseX <= btnX + btnSize && mouseY >= btnY && mouseY <= btnY + btnSize;

        // Если мышка наведена — рисуем stena_hover.png, если нет — обычную stena.png
        ResourceLocation texture = isHovered ? BUTTON_HOVER : BUTTON_NORMAL;
        RenderSystem.setShaderTexture(0, texture);
        guiGraphics.blit(texture, btnX, btnY, 0, 0, btnSize, btnSize, btnSize, btnSize);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    // 4. Отслеживаем клик левой кнопкой мыши по экрану
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int btnX = x + (this.imageWidth / 2) - 20;
        int btnY = y + 20;
        int btnSize = 40;

        // Если это клик ЛКМ (button == 0) и курсор попал ровно на нашу стену
        if (button == 0 && mouseX >= btnX && mouseX <= btnX + btnSize && mouseY >= btnY && mouseY <= btnY + btnSize) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("Способность 'Барьер Скаута I' успешно активирована!"));
                this.onClose(); // Закрываем меню
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
