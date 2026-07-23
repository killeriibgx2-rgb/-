package net.wizard.elemental_awakening.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.wizard.elemental_awakening.ElementalAwakening;
import net.wizard.elemental_awakening.networking.ModMessages;
import net.wizard.elemental_awakening.networking.SelectSpellServerPacket;

public class EarthMenuScreen extends Screen {
    // Путь к твоей пережатой текстуре плиты Земли (теперь она родного размера 256х256)
    private static final ResourceLocation TEXTURE = new ResourceLocation(ElementalAwakening.MOD_ID, "textures/gui/earth_menu.png");

    // Твои иконки Стены Земли (40х40 исходники)
    private static final ResourceLocation ICON_NORMAL = new ResourceLocation(ElementalAwakening.MOD_ID, "textures/gui/stena.png");
    private static final ResourceLocation ICON_HOVER = new ResourceLocation(ElementalAwakening.MOD_ID, "textures/gui/stena_hover.png");

    // Родные размеры окна интерфейса
    private final int menuWidth = 256;
    private final int menuHeight = 256;

    public EarthMenuScreen() {
        super(Component.empty());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Затемняем фон игры за менюшкой
        this.renderBackground(guiGraphics);

        // Находим центр монитора игрока
        int x = (this.width - menuWidth) / 2;
        int y = (this.height - menuHeight) / 2;

        RenderSystem.setShaderTexture(0, TEXTURE);
        // Рисуем фон пиксель в пиксель, так как картинка на диске уже имеет размер 256х256
        guiGraphics.blit(TEXTURE, x, y, 0, 0, menuWidth, menuHeight, 256, 256);

        int relativeMouseX = mouseX - x;
        int relativeMouseY = mouseY - y;

        // Координаты кнопки Стены на плите (Центр сверху: X от 118 до 138, Y от 30 до 50)
        boolean isHovered = relativeMouseX >= 118 && relativeMouseX <= 138 && relativeMouseY >= 30 && relativeMouseY <= 50;
        ResourceLocation currentIcon = isHovered ? ICON_HOVER : ICON_NORMAL;

        RenderSystem.setShaderTexture(0, currentIcon);
        // Рисуем иконку Стены размером 20х20, сжимая исходный файл 40х40 под размер сетки
        guiGraphics.blit(currentIcon, x + 118, y + 30, 0, 0, 20, 20, 40, 40);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Если кликнули ЛКМ
            int x = (this.width - menuWidth) / 2;
            int y = (this.height - menuHeight) / 2;

            double clickX = mouseX - x;
            double clickY = mouseY - y;

            // Проверяем точный клик по кнопке Стены
            if (clickX >= 118 && clickX <= 138 && clickY >= 30 && clickY <= 50) {
                // Шлем на сервер наш новый пакет: записать в палочку заклинание "earth_stena"
                ModMessages.sendToServer(new SelectSpellServerPacket("earth_stena"));
                this.onClose(); // Закрываем меню прокачки
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
