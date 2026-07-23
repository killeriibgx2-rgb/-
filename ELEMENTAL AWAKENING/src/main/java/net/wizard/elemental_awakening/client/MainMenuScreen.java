package net.wizard.elemental_awakening.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.wizard.elemental_awakening.ElementalAwakening;
import net.wizard.elemental_awakening.networking.ModMessages;
import net.wizard.elemental_awakening.networking.SelectElementServerPacket;

public class MainMenuScreen extends Screen {
    // Твой крутой пиксель-арт файл фона
    private static final ResourceLocation TEXTURE = new ResourceLocation(ElementalAwakening.MOD_ID, "textures/gui/main_menu.png");

    // Жестко фиксируем размер окна 256х256
    private final int menuWidth = 256;
    private final int menuHeight = 256;

    public MainMenuScreen() {
        super(Component.empty());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Затемняем задний фон игры
        this.renderBackground(guiGraphics);

        // Находим центр экрана для нашей плиты
        int x = (this.width - menuWidth) / 2;
        int y = (this.height - menuHeight) / 2;

        RenderSystem.setShaderTexture(0, TEXTURE);
        // Майнкрафт принудительно сжимает твой исходник 512х512 в аккуратный квадрат 256х256
        guiGraphics.blit(TEXTURE, x, y, 0, 0, menuWidth, menuHeight, 512, 512);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Если нажали ЛКМ
            int x = (this.width - menuWidth) / 2;
            int y = (this.height - menuHeight) / 2;

            double clickX = mouseX - x;
            double clickY = mouseY - y;

            // Проверяем, что клик пришелся строго внутрь нашего меню 256х256
            if (clickX >= 0 && clickX <= 256 && clickY >= 0 && clickY <= 256) {

                // ВЕРХНЯЯ ПОЛОВИНА ЭКРАНА (Y < 128)
                if (clickY < 128) {
                    if (clickX < 128) {
                        // 1. Вверху слева — ОГОНЬ (Элемент 2)
                        ModMessages.sendToServer(new SelectElementServerPacket(2));
                    } else {
                        // 2. Вверху справа — ВОЗДУХ (Элемент 4)
                        ModMessages.sendToServer(new SelectElementServerPacket(4));
                    }
                }
                // НИЖНЯЯ ПОЛОВИНА ЭКРАНА (Y >= 128)
                else {
                    if (clickX < 128) {
                        // 3. Внизу слева — ЗЕМЛЯ (Элемент 1)
                        ModMessages.sendToServer(new SelectElementServerPacket(1));
                    } else {
                        // 4. Внизу справа — ВОДА (Элемент 3)
                        ModMessages.sendToServer(new SelectElementServerPacket(3));
                    }
                }

                this.onClose(); // Закрываем экран выбора
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // Запрещаем закрывать меню по кнопке ESC, пока выбор не сделан
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
