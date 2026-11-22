package com.eternalsnow.client;

import com.eternalsnow.network.ThermalNetwork;
import com.eternalsnow.network.ThermalSettingsUpdatePacket;
import com.eternalsnow.thermal.world.ClientThermalSettings;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A compact, server-authoritative admin panel for the first single-player milestone. */
public final class ThermalSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 430;
    private static final int PANEL_HEIGHT = 350;

    private final Screen parent;
    private EditBox ambientInput;
    private EditBox speedInput;
    private EditBox radiusInput;
    private Button enabledButton;
    private Button bodyButton;
    private Button doorsButton;
    private Button applyButton;

    private boolean settingsLoaded;
    private boolean canEdit;
    private boolean waitingForSave;
    private int lastRevision = -1;
    private boolean enabled = true;
    private boolean bodyEnabled = true;
    private boolean doorInfiltration = true;
    private Component feedback;

    public ThermalSettingsScreen(Screen parent) {
        super(Component.translatable("eternalsnow.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        ambientInput = new EditBox(this.font, left + 306, top + 57, 78, 20, Component.empty());
        ambientInput.setMaxLength(12);
        ambientInput.setFilter(value -> value.matches("-?\\d{0,3}(?:[.,]\\d{0,2})?"));
        ambientInput.setHint(Component.literal("−30"));
        addRenderableWidget(ambientInput);

        speedInput = new EditBox(this.font, left + 306, top + 94, 78, 20, Component.empty());
        speedInput.setMaxLength(12);
        speedInput.setFilter(value -> value.matches("\\d{0,4}(?:[.,]\\d{0,2})?"));
        speedInput.setHint(Component.literal("1.00"));
        addRenderableWidget(speedInput);

        radiusInput = new EditBox(this.font, left + 306, top + 131, 78, 20, Component.empty());
        radiusInput.setMaxLength(2);
        radiusInput.setFilter(value -> value.matches("\\d{0,2}"));
        radiusInput.setHint(Component.literal("8"));
        addRenderableWidget(radiusInput);

        enabledButton = addRenderableWidget(Button.builder(toggleMessage("enabled", enabled), button -> {
            enabled = !enabled;
            refreshToggleMessages();
        }).bounds(left + 20, top + 171, 190, 22).build());

        bodyButton = addRenderableWidget(Button.builder(toggleMessage("body", bodyEnabled), button -> {
            bodyEnabled = !bodyEnabled;
            refreshToggleMessages();
        }).bounds(left + 220, top + 171, 190, 22).build());

        doorsButton = addRenderableWidget(Button.builder(toggleMessage("doors", doorInfiltration), button -> {
            doorInfiltration = !doorInfiltration;
            refreshToggleMessages();
        }).bounds(left + 20, top + 201, 390, 22).build());

        applyButton = addRenderableWidget(Button.builder(
                Component.translatable("eternalsnow.settings.apply"), button -> applySettings())
                .bounds(left + 20, top + 305, 188, 24).build());
        addRenderableWidget(Button.builder(Component.translatable("eternalsnow.settings.close"),
                button -> onClose()).bounds(left + 222, top + 305, 188, 24).build());

        if (settingsLoaded && canEdit) {
            ClientThermalSettings.Snapshot snapshot = ClientThermalSettings.snapshot();
            if (snapshot != null) populate(snapshot);
        }
        setInitialFocus(ambientInput);
        updateControlState();
    }

    @Override
    public void tick() {
        super.tick();
        ClientThermalSettings.Snapshot snapshot = ClientThermalSettings.snapshot();
        if (snapshot == null || snapshot.revision() == lastRevision) return;

        lastRevision = snapshot.revision();
        if (!settingsLoaded) {
            settingsLoaded = true;
            canEdit = snapshot.allowed();
            if (canEdit) {
                populate(snapshot);
            } else {
                feedback = Component.translatable("eternalsnow.settings.denied");
            }
        } else if (waitingForSave) {
            waitingForSave = false;
            canEdit = snapshot.allowed();
            if (snapshot.applied()) {
                populate(snapshot);
                feedback = Component.translatable("eternalsnow.settings.saved");
            } else if (!snapshot.allowed()) {
                feedback = Component.translatable("eternalsnow.settings.denied");
            } else {
                feedback = Component.translatable("eternalsnow.settings.invalid");
            }
        }
        updateControlState();
    }

    private void populate(ClientThermalSettings.Snapshot snapshot) {
        enabled = snapshot.enabled();
        bodyEnabled = snapshot.bodyEnabled();
        doorInfiltration = snapshot.doorInfiltration();
        ambientInput.setValue(String.format(Locale.ROOT, "%.1f", snapshot.ambientCelsius()));
        speedInput.setValue(String.format(Locale.ROOT, "%.2f", snapshot.speedMultiplier()));
        radiusInput.setValue(Integer.toString(snapshot.zoneRadius()));
        refreshToggleMessages();
    }

    private void applySettings() {
        if (!settingsLoaded || !canEdit || waitingForSave) return;
        try {
            double ambient = Double.parseDouble(ambientInput.getValue().trim().replace(',', '.'));
            double speed = Double.parseDouble(speedInput.getValue().trim().replace(',', '.'));
            int radius = Integer.parseInt(radiusInput.getValue().trim());
            if (!Double.isFinite(ambient) || ambient < -273.0 || ambient > 200.0
                    || !Double.isFinite(speed) || speed < 0.0 || speed > 1000.0
                    || radius < 4 || radius > 16) {
                feedback = Component.translatable("eternalsnow.settings.invalid");
                return;
            }

            waitingForSave = true;
            feedback = Component.translatable("eternalsnow.settings.saving");
            updateControlState();
            ThermalNetwork.sendToServer(new ThermalSettingsUpdatePacket(enabled, ambient, speed,
                    radius, bodyEnabled, doorInfiltration));
        } catch (NumberFormatException invalidInput) {
            feedback = Component.translatable("eternalsnow.settings.invalid");
        }
    }

    private Component toggleMessage(String setting, boolean value) {
        return Component.translatable("eternalsnow.settings.toggle." + setting,
                Component.translatable(value ? "eternalsnow.settings.on" : "eternalsnow.settings.off"));
    }

    private void refreshToggleMessages() {
        if (enabledButton != null) enabledButton.setMessage(toggleMessage("enabled", enabled));
        if (bodyButton != null) bodyButton.setMessage(toggleMessage("body", bodyEnabled));
        if (doorsButton != null) doorsButton.setMessage(toggleMessage("doors", doorInfiltration));
    }

    private void updateControlState() {
        boolean active = settingsLoaded && canEdit && !waitingForSave;
        if (ambientInput != null) ambientInput.active = active;
        if (speedInput != null) speedInput.active = active;
        if (radiusInput != null) radiusInput.active = active;
        if (enabledButton != null) enabledButton.active = active;
        if (bodyButton != null) bodyButton.active = active;
        if (doorsButton != null) doorsButton.active = active;
        if (applyButton != null) applyButton.active = active;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;

        graphics.fill(left - 5, top - 5, right + 5, bottom + 5, 0x70000000);
        graphics.fill(left, top, right, bottom, 0xF0151C24);
        graphics.fill(left, top, right, top + 3, 0xFF65D7E8);
        graphics.fill(left, top + 3, left + 4, bottom, 0xFF2F7D91);
        graphics.fill(left + 20, top + 49, right - 20, top + 50, 0xFF34444F);
        graphics.fill(left + 20, top + 161, right - 20, top + 162, 0xFF34444F);

        graphics.drawString(font, Component.translatable("eternalsnow.settings.title"),
                left + 22, top + 15, 0xFFF0F5F7, true);
        graphics.drawString(font, Component.translatable("eternalsnow.settings.subtitle"),
                left + 22, top + 33, 0xFF8FA8B5, false);

        drawFieldLabel(graphics, "ambient", left + 22, top + 64);
        drawFieldLabel(graphics, "speed", left + 22, top + 101);
        drawFieldLabel(graphics, "radius", left + 22, top + 138);
        graphics.drawString(font, "°C", left + 390, top + 64, 0xFFB3C5CF, false);
        graphics.drawString(font, "×", left + 390, top + 101, 0xFFB3C5CF, false);
        graphics.drawString(font, Component.translatable("eternalsnow.settings.blocks"),
                left + 390, top + 138, 0xFFB3C5CF, false);

        graphics.drawString(font, Component.translatable("eternalsnow.settings.speed_note"),
                left + 22, top + 239, 0xFF8FA8B5, false);
        if (feedback != null) {
            graphics.drawCenteredString(font, feedback, this.width / 2, top + 270,
                    waitingForSave ? 0xFF84DDEE : 0xFFFFC66D);
        } else if (!settingsLoaded) {
            graphics.drawCenteredString(font, Component.translatable("eternalsnow.settings.loading"),
                    this.width / 2, top + 270, 0xFFB3C5CF);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawFieldLabel(GuiGraphics graphics, String name, int x, int y) {
        graphics.drawString(font, Component.translatable("eternalsnow.settings." + name),
                x, y, 0xFFE5EDF1, false);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
