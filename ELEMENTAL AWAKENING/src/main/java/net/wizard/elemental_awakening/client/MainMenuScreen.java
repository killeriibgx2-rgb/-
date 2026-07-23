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
    private static final ResourceLocation TEXTURE = new ResourceLocation(ElementalAwakening.MOD_ID, "textures/gui/main_menu.png");
    private static final ResourceLocation EARTH_ICON = new ResourceLocation("minecraft", "textures/block/dirt.png");
    private static final ResourceLocation FIRE_ICON = new ResourceLocation("minecraft", "textures/block/magma.png");
    private static final ResourceLocation WATER_ICON = new ResourceLocation("minecraft", "textures/block/water_still.png");

    private final int menuWidth = 512;
    private final int menuHeight = 512;

    public MainMenuScreen() {
        super(Component.empty());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int x = (this.width - menuWidth) / 2;
        int y = (this.height - menuHeight) / 2;

        RenderSystem.setShaderTexture(0, TEXTURE);
        // Правильный метод blit для Forge 1.20.1, который принудительно сжимает текстуру 512х512 в рамки экрана
        guiGraphics.blit(TEXTURE, x, y, 0, 0, menuWidth, menuHeight, 512, 512);

        // Кнопки (отрисовываем ванильные блоки поверх сжатого фона)
        guiGraphics.blit(FIRE_ICON, x + 364, y + 108, 0, 0, 40, 40, 16, 16);
        guiGraphics.blit(EARTH_ICON, x + 108, y + 364, 0, 0, 40, 40, 16, 16);
        guiGraphics.blit(WATER_ICON, x + 364, y + 364, 0, 0, 40, 40, 16, 16);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = (this.width - menuWidth) / 2;
            int y = (this.height - menuHeight) / 2;

            double clickX = mouseX - x;
            double clickY = mouseY - y;

            if (clickX >= 108 && clickX <= 148 && clickY >= 108 && clickY <= 148) {
                ModMessages.sendToServer(new SelectElementServerPacket(4)); // Воздух
                this.onClose();
                return true;
            }
            if (clickX >= 364 && clickX <= 404 && clickY >= 108 && clickY <= 148) {
                ModMessages.sendToServer(new SelectElementServerPacket(2)); // Огонь
                this.onClose();
                return true;
            }
            if (clickX >= 108 && clickX <= 148 && clickY >= 364 && clickY <= 404) {
                ModMessages.sendToServer(new SelectElementServerPacket(1)); // Земля
                this.onClose();
                return true;
            }
            if (clickX >= 364 && clickX <= 404 && clickY >= 364 && clickY <= 404) {
                ModMessages.sendToServer(new SelectElementServerPacket(3)); // Вода
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
