package com.eternalsnow.client;

import com.eternalsnow.network.PacketHandler;
import com.eternalsnow.network.ServerboundConfigureGolemPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ForgeSlider;

public final class GolemConfigScreen extends Screen {
    private final int entityId;
    private final BlockPos anchorPos;
    private final boolean hasShovel;
    private final int sweepW;
    private final int sweepL;
    private final int sweepH;
    private int workRadius;

    public GolemConfigScreen(
            int entityId,
            BlockPos anchorPos,
            int initialRadius,
            boolean hasShovel,
            int sweepW,
            int sweepL,
            int sweepH
    ) {
        super(Component.translatable("eternalsnow.golem.screen.title"));
        this.entityId = entityId;
        this.anchorPos = anchorPos;
        this.workRadius = initialRadius;
        this.hasShovel = hasShovel;
        this.sweepW = sweepW;
        this.sweepL = sweepL;
        this.sweepH = sweepH;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int startY = this.height / 2 - 22;

        ForgeSlider radiusSlider = new ForgeSlider(
                centerX - 130,
                startY,
                260,
                20,
                Component.translatable("eternalsnow.golem.radius"),
                Component.translatable("eternalsnow.gui.unit.blocks"),
                2.0D,
                24.0D,
                this.workRadius,
                1.0D,
                0,
                true
        ) {
            @Override
            protected void applyValue() {
                GolemConfigScreen.this.workRadius = this.getValueInt();
            }
        };
        this.addRenderableWidget(radiusSlider);

        this.addRenderableWidget(new EternalSnowWidgets.ActionButton(
                centerX - 130, startY + 28, 260, 20,
                Component.translatable("eternalsnow.golem.preview_zone"),
                () -> PacketHandler.sendToServer(new ServerboundConfigureGolemPacket(this.entityId, this.workRadius, true))
        ));

        this.addRenderableWidget(new EternalSnowWidgets.ActionButton(
                centerX - 130, startY + 56, 126, 20,
                CommonComponents.GUI_DONE,
                () -> {
                    PacketHandler.sendToServer(new ServerboundConfigureGolemPacket(this.entityId, this.workRadius, true));
                    this.onClose();
                }
        ));

        this.addRenderableWidget(new EternalSnowWidgets.ActionButton(
                centerX + 4, startY + 56, 126, 20,
                CommonComponents.GUI_CANCEL,
                this::onClose
        ));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int centerX = this.width / 2;
        int panelTop = this.height / 2 - 86;
        int panelBottom = this.height / 2 + 82;

        EternalSnowWidgets.drawVanillaSnowPanel(graphics, centerX - 150, panelTop, 300, panelBottom - panelTop, 0xE6282D34, true);

        graphics.drawCenteredString(this.font, this.title, centerX, panelTop + 12, 0xFFF2F6FA);

        Component postLine = Component.translatable(
                "eternalsnow.golem.anchor_coords",
                this.anchorPos.getX(),
                this.anchorPos.getY(),
                this.anchorPos.getZ()
        );
        graphics.drawCenteredString(this.font, postLine, centerX, panelTop + 30, 0xFFD0D8E2);

        Component shovelLine = this.hasShovel
                ? Component.translatable("eternalsnow.golem.shovel_equipped", this.sweepW, this.sweepL, this.sweepH)
                : Component.translatable("eternalsnow.golem.shovel_missing");
        graphics.drawCenteredString(this.font, shovelLine, centerX, panelTop + 44, this.hasShovel ? 0xFFCDE6D2 : 0xFFE6B8B0);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
