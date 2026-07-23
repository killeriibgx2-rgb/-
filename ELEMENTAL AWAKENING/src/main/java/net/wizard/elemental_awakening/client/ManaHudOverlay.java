package net.wizard.elemental_awakening.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.wizard.elemental_awakening.ElementalAwakening;
import net.wizard.elemental_awakening.mana.PlayerManaProvider;

public class ManaHudOverlay {
    // Пути к твоим двум отдельным файлам полосок
    private static final ResourceLocation MANA_EMPTY = new ResourceLocation(ElementalAwakening.MOD_ID, "textures/gui/mana_empty.png");
    private static final ResourceLocation MANA_FULL = new ResourceLocation(ElementalAwakening.MOD_ID, "textures/gui/mana_full.png");

    public static final IGuiOverlay HUD_MANA = (gui, guiGraphics, partialTick, width, height) -> {
        Minecraft minecraft = Minecraft.getInstance();

        // Показываем ману только в выживании или приключении
        if (minecraft.player != null && !minecraft.player.isSpectator() && !minecraft.player.isCreative()) {
            int x = width / 2;
            int y = height - 55; // Позиция по вертикали над хотбаром

            // Твои новые размеры текстуры
            int barWidth = 192;
            int barHeight = 6;
            int startX = x - (barWidth / 2); // Центрируем по горизонтали

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            // 1. Рисуем пустую темную подложку целиком
            RenderSystem.setShaderTexture(0, MANA_EMPTY);
            guiGraphics.blit(MANA_EMPTY, startX, y, 0, 0, barWidth, barHeight, barWidth, barHeight);

            // Достаем реальное количество маны игрока из нашей системы
            minecraft.player.getCapability(PlayerManaProvider.PLAYER_MANA).ifPresent(mana -> {
                // Высчитываем сколько пикселей синей полоски нужно нарисовать
                int activeBarWidth = (int) ((mana.getMana() / mana.getMaxMana()) * barWidth);

                // 2. Рисуем синюю полоску поверх пустой, обрезая её по ширине
                if (activeBarWidth > 0) {
                    RenderSystem.setShaderTexture(0, MANA_FULL);
                    guiGraphics.blit(MANA_FULL, startX, y, 0, 0, activeBarWidth, barHeight, barWidth, barHeight);
                }
            });
        }
    };
}
