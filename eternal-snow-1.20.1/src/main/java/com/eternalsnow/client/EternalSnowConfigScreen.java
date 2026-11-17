package com.eternalsnow.client;

import com.eternalsnow.config.ClientSnowConfig;
import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.network.PacketHandler;
import com.eternalsnow.network.ServerboundForceWeatherPacket;
import com.eternalsnow.network.ServerboundUpdateSettingsPacket;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.thermal.BlockThermalCategory;
import com.eternalsnow.weather.WeatherEventType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.widget.ForgeSlider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Clean 2-Tab Snowy-Vanilla Configuration Menu:
 * Top Tabs: [ ❄ Клиент ]  |  [ ⚙ Админ ]
 * Inside Admin: Quick Weather Controls + 4 tidy category folders (Weather/Drifts, Surfaces, Thermal Physics, Survival & Armor).
 */
public final class EternalSnowConfigScreen extends Screen {
    private static final int TAB_CLIENT = 0;
    private static final int TAB_ADMIN = 1;

    // Admin pages (0 = main Admin hub)
    private static final int ADMIN_HUB = 0;
    private static final int ADMIN_WEATHER = 1;
    private static final int ADMIN_SURFACES = 2;
    private static final int ADMIN_THERMAL = 3;
    private static final int ADMIN_THERM_SOURCES = 31;
    private static final int ADMIN_THERM_COND = 32;
    private static final int ADMIN_THERM_CAP = 33;
    private static final int ADMIN_SURVIVAL = 4;

    private final Screen lastScreen;
    private int activeTab = TAB_CLIENT;
    private int adminPage = ADMIN_HUB;

    // Client working copy
    private boolean clientVisibleSnow;
    private boolean clientAutoWeatherParticles;
    private int clientParticleCount;
    private int clientSnowRadius;
    private int clientFlakeSizePct;
    private int clientFlakeOpacityPct;
    private int clientWindVolumePct;
    private boolean clientFaceSnow;
    private boolean clientClearSky;
    private boolean clientShowHud;

    // Admin working copy
    private final ServerSnowSettings workingServer;
    private final boolean canEditAdmin;

    // Scrollable viewport
    private final List<ScrollEntry> scrollEntries = new ArrayList<>();
    private double scrollOffset = 0.0D;
    private int contentHeight = 0;
    private int viewportTop;
    private int viewportBottom;
    private int panelLeft;
    private int panelRight;

    public EternalSnowConfigScreen(Screen lastScreen) {
        super(Component.translatable("eternalsnow.gui.title"));
        this.lastScreen = lastScreen;

        this.clientVisibleSnow = ClientSnowConfig.visibleSnowfall;
        this.clientAutoWeatherParticles = ClientSnowConfig.autoWeatherParticles;
        this.clientParticleCount = ClientSnowConfig.snowParticleCount;
        this.clientSnowRadius = ClientSnowConfig.snowfallRadius;
        this.clientFlakeSizePct = ClientSnowConfig.snowflakeSizePercent;
        this.clientFlakeOpacityPct = ClientSnowConfig.snowflakeOpacityPercent;
        this.clientWindVolumePct = ClientSnowConfig.windSoundVolumePercent;
        this.clientFaceSnow = ClientSnowConfig.faceSnowOverlay;
        this.clientClearSky = ClientSnowConfig.clearSkyAndLight;
        this.clientShowHud = ClientSnowConfig.showSurvivalHud;

        this.workingServer = EternalSnowState.getEffectiveSettings().copy();
        this.canEditAdmin = computeAdminPermission();
    }

    private static boolean computeAdminPermission() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.hasSingleplayerServer()) {
            return true;
        }
        return EternalSnowState.canClientEditAdmin()
                || (mc.player != null && mc.player.hasPermissions(2));
    }

    @Override
    protected void init() {
        super.init();
        this.scrollEntries.clear();

        int centerX = this.width / 2;
        int panelW = Math.min(368, this.width - 16);
        this.panelLeft = centerX - panelW / 2;
        this.panelRight = centerX + panelW / 2;

        // 2 Big Clean Top Tabs: [ ❄ Клиент ]  |  [ ⚙ Админ ]
        int tabY = 20;
        int tabGap = 6;
        int tabW = (panelW - tabGap) / 2;

        this.addRenderableWidget(new EternalSnowWidgets.TabButton(
                this.panelLeft, tabY, tabW, 20,
                Component.translatable("eternalsnow.gui.tab.client_main"),
                () -> this.activeTab == TAB_CLIENT,
                () -> {
                    this.activeTab = TAB_CLIENT;
                    this.scrollOffset = 0.0D;
                    this.rebuildWidgets();
                }
        ));

        EternalSnowWidgets.TabButton adminTabBtn = new EternalSnowWidgets.TabButton(
                this.panelLeft + tabW + tabGap, tabY, tabW, 20,
                Component.translatable("eternalsnow.gui.tab.admin_main"),
                () -> this.activeTab == TAB_ADMIN,
                () -> {
                    this.activeTab = TAB_ADMIN;
                    this.adminPage = ADMIN_HUB;
                    this.scrollOffset = 0.0D;
                    this.rebuildWidgets();
                }
        );
        adminTabBtn.active = this.canEditAdmin;
        this.addRenderableWidget(adminTabBtn);

        this.viewportTop = (this.activeTab == TAB_CLIENT) ? 45 : 68;
        this.viewportBottom = this.height - 31;

        int colFullW = panelW - 16;
        int colHalfW = (colFullW - 6) / 2;
        int leftX = this.panelLeft + 6;
        int rightX = leftX + colHalfW + 6;

        if (this.activeTab == TAB_CLIENT) {
            buildClientTab(leftX, rightX, colFullW, colHalfW);
        } else {
            buildAdminTab(leftX, rightX, colFullW, colHalfW);
        }

        // Footer Action Buttons
        int footerY = this.height - 25;
        int btnW = (panelW - 8) / 3;

        this.addRenderableWidget(new EternalSnowWidgets.ActionButton(
                this.panelLeft, footerY, btnW, 20,
                Component.translatable("eternalsnow.gui.reset_defaults"),
                () -> {
                    resetCurrentTabDefaults();
                    this.rebuildWidgets();
                }
        ));

        this.addRenderableWidget(new EternalSnowWidgets.ActionButton(
                this.panelLeft + btnW + 4, footerY, btnW, 20,
                CommonComponents.GUI_DONE,
                this::saveAndClose,
                true
        ));

        this.addRenderableWidget(new EternalSnowWidgets.ActionButton(
                this.panelLeft + (btnW + 4) * 2, footerY, btnW, 20,
                CommonComponents.GUI_CANCEL,
                () -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(this.lastScreen);
                    }
                }
        ));

        applyScrollPositions();
    }

    private <T extends AbstractWidget> T addScrolled(T widget, int localY) {
        this.scrollEntries.add(new ScrollEntry(widget, localY));
        this.contentHeight = Math.max(this.contentHeight, localY + widget.getHeight() + 8);
        return this.addRenderableWidget(widget);
    }

    private void openAdminPage(int page) {
        this.adminPage = page;
        this.scrollOffset = 0.0D;
        this.rebuildWidgets();
    }

    private int addAdminBackHeader(int x, int fullW, int targetBackPage, String parentKey, String currentTitleKey) {
        int y = 4;
        Component backLabel = Component.translatable("eternalsnow.gui.nav.back_to", Component.translatable(parentKey));
        addScrolled(new EternalSnowWidgets.ActionButton(
                x, 0, fullW, 18,
                backLabel,
                () -> openAdminPage(targetBackPage)
        ), y);
        y += 22;
        addScrolled(new EternalSnowWidgets.SectionHeader(x, 0, fullW, Component.translatable(currentTitleKey)), y);
        return y + 18;
    }

    // =========================================================================
    // TAB 1: КЛИЕНТ (Визуал снега, небо, звук ветра и HUD)
    // =========================================================================
    private void buildClientTab(int leftX, int rightX, int fullW, int halfW) {
        this.contentHeight = 0;
        int y = 4;

        addScrolled(new EternalSnowWidgets.SectionHeader(leftX, 0, fullW, Component.translatable("eternalsnow.gui.sec.client_snow")), y);
        y += 18;

        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.client.visible_snow"),
                this.clientVisibleSnow,
                v -> this.clientVisibleSnow = v
        ), y);
        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                rightX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.client.auto_particles"),
                this.clientAutoWeatherParticles,
                v -> this.clientAutoWeatherParticles = v
        ), y);
        y += 24;

        addScrolled(makeIntSlider(
                leftX, fullW, "eternalsnow.gui.client.particle_count", " шт.",
                500, 20000, this.clientParticleCount, 250,
                v -> this.clientParticleCount = v
        ), y);
        y += 24;

        addScrolled(makeIntSlider(
                leftX, halfW, "eternalsnow.gui.client.snow_radius", " бл.",
                8, 40, this.clientSnowRadius, 1,
                v -> this.clientSnowRadius = v
        ), y);
        addScrolled(makeIntSlider(
                rightX, halfW, "eternalsnow.gui.client.flake_size", "%",
                50, 250, this.clientFlakeSizePct, 5,
                v -> this.clientFlakeSizePct = v
        ), y);
        y += 24;

        addScrolled(makeIntSlider(
                leftX, halfW, "eternalsnow.gui.client.flake_opacity", "%",
                20, 100, this.clientFlakeOpacityPct, 5,
                v -> this.clientFlakeOpacityPct = v
        ), y);
        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                rightX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.client.face_snow"),
                this.clientFaceSnow,
                v -> this.clientFaceSnow = v
        ), y);
        y += 24;

        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, fullW, 20,
                Component.translatable("eternalsnow.gui.client.clear_sky"),
                this.clientClearSky,
                v -> this.clientClearSky = v
        ), y);
        y += 26;

        addScrolled(new EternalSnowWidgets.SectionHeader(leftX, 0, fullW, Component.translatable("eternalsnow.gui.sec.client_audio_hud")), y);
        y += 18;

        addScrolled(makeIntSlider(
                leftX, fullW, "eternalsnow.gui.client.wind_volume", "%",
                0, 200, this.clientWindVolumePct, 5,
                v -> this.clientWindVolumePct = v
        ), y);
        y += 24;

        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, fullW, 20,
                Component.translatable("eternalsnow.gui.client.show_hud"),
                this.clientShowHud,
                v -> this.clientShowHud = v
        ), y);
    }

    // =========================================================================
    // TAB 2: АДМИН (Все серверные и мировые разделы внутри одной вкладки!)
    // =========================================================================
    private void buildAdminTab(int leftX, int rightX, int fullW, int halfW) {
        this.contentHeight = 0;

        switch (this.adminPage) {
            case ADMIN_HUB -> buildAdminHub(leftX, fullW);
            case ADMIN_WEATHER -> buildAdminWeather(leftX, rightX, fullW, halfW);
            case ADMIN_SURFACES -> buildAdminSurfaces(leftX, rightX, fullW, halfW);
            case ADMIN_THERMAL -> buildAdminThermalRoot(leftX, fullW);
            case ADMIN_THERM_SOURCES -> buildAdminThermalSources(leftX, fullW);
            case ADMIN_THERM_COND -> buildAdminThermalCond(leftX, fullW);
            case ADMIN_THERM_CAP -> buildAdminThermalCap(leftX, fullW);
            case ADMIN_SURVIVAL -> buildAdminSurvival(leftX, rightX, fullW, halfW);
        }
    }

    private void buildAdminHub(int leftX, int fullW) {
        int y = 4;

        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, fullW, 20,
                Component.translatable("eternalsnow.gui.admin.enabled"),
                this.workingServer.enabled,
                v -> this.workingServer.enabled = v
        ), y);
        y += 24;

        addScrolled(new EternalSnowWidgets.SectionHeader(leftX, 0, fullW, Component.translatable("eternalsnow.gui.sec.quick_weather")), y);
        y += 18;

        boolean inWorld = Minecraft.getInstance().level != null && this.canEditAdmin;
        int wBtn = (fullW - 8) / 5;
        WeatherEventType[] types = WeatherEventType.values();
        for (int i = 0; i < types.length; i++) {
            final int id = types[i].getId();
            EternalSnowWidgets.ActionButton wb = new EternalSnowWidgets.ActionButton(
                    leftX + i * (wBtn + 2), 0, wBtn, 18,
                    Component.translatable(types[i].getShortButtonKey()),
                    () -> PacketHandler.sendToServer(new ServerboundForceWeatherPacket(id))
            );
            wb.active = inWorld;
            addScrolled(wb, y);
        }
        y += 21;

        EternalSnowWidgets.ActionButton clearBtn = new EternalSnowWidgets.ActionButton(
                leftX, 0, fullW, 18,
                Component.translatable("eternalsnow.gui.admin.clear_snow_now"),
                () -> PacketHandler.sendToServer(new ServerboundForceWeatherPacket(100))
        );
        clearBtn.active = inWorld;
        addScrolled(clearBtn, y);
        y += 23;

        addScrolled(new EternalSnowWidgets.SectionHeader(leftX, 0, fullW, Component.translatable("eternalsnow.gui.sec.admin_categories")), y);
        y += 18;

        addScrolled(new EternalSnowWidgets.FolderButton(
                leftX, 0, fullW, 26,
                Component.translatable("eternalsnow.gui.cat.weather"),
                Component.translatable("eternalsnow.gui.cat.weather.sub"),
                () -> openAdminPage(ADMIN_WEATHER)
        ), y);
        y += 29;

        addScrolled(new EternalSnowWidgets.FolderButton(
                leftX, 0, fullW, 26,
                Component.translatable("eternalsnow.gui.cat.surfaces"),
                Component.translatable("eternalsnow.gui.cat.surfaces.sub"),
                () -> openAdminPage(ADMIN_SURFACES)
        ), y);
        y += 29;

        addScrolled(new EternalSnowWidgets.FolderButton(
                leftX, 0, fullW, 26,
                Component.translatable("eternalsnow.gui.cat.thermal"),
                Component.translatable("eternalsnow.gui.cat.thermal.sub"),
                () -> openAdminPage(ADMIN_THERMAL)
        ), y);
        y += 29;

        addScrolled(new EternalSnowWidgets.FolderButton(
                leftX, 0, fullW, 26,
                Component.translatable("eternalsnow.gui.cat.survival"),
                Component.translatable("eternalsnow.gui.cat.survival.sub"),
                () -> openAdminPage(ADMIN_SURVIVAL)
        ), y);
    }

    private void buildAdminWeather(int leftX, int rightX, int fullW, int halfW) {
        int y = addAdminBackHeader(leftX, fullW, ADMIN_HUB, "eternalsnow.gui.tab.admin_main", "eternalsnow.gui.cat.weather");

        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, fullW, 20,
                Component.translatable("eternalsnow.gui.admin.auto_cycle"),
                this.workingServer.autoWeatherEvents,
                v -> this.workingServer.autoWeatherEvents = v
        ), y);
        y += 24;

        final ForgeSlider[] windAngleRef = new ForgeSlider[1];
        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.admin.auto_wind"),
                this.workingServer.autoWind,
                v -> {
                    this.workingServer.autoWind = v;
                    if (windAngleRef[0] != null) windAngleRef[0].active = !v;
                }
        ), y);
        ForgeSlider manualAngle = makeIntSlider(
                rightX, halfW, "eternalsnow.gui.admin.wind_angle", "°",
                0, 359, this.workingServer.manualWindAngleDeg, 5,
                v -> this.workingServer.manualWindAngleDeg = v
        );
        manualAngle.active = !this.workingServer.autoWind;
        windAngleRef[0] = manualAngle;
        addScrolled(manualAngle, y);
        y += 24;

        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.admin.blizzard_push"),
                this.workingServer.blizzardWindPush,
                v -> this.workingServer.blizzardWindPush = v
        ), y);
        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                rightX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.admin.keep_sky_light"),
                this.workingServer.keepClearSkyLight,
                v -> this.workingServer.keepClearSkyLight = v
        ), y);
        y += 26;

        addScrolled(new EternalSnowWidgets.SectionHeader(leftX, 0, fullW, Component.translatable("eternalsnow.gui.folder.weather_drifts")), y);
        y += 18;

        final ForgeSlider[] heightRef = new ForgeSlider[1];
        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.admin.infinite_height"),
                this.workingServer.infiniteHeight,
                v -> {
                    this.workingServer.infiniteHeight = v;
                    if (heightRef[0] != null) heightRef[0].active = !v;
                }
        ), y);

        ForgeSlider avgHeight = makeDoubleSlider(
                rightX, halfW, "eternalsnow.gui.admin.avg_height", "eternalsnow.gui.unit.blocks",
                0.1D, 15.0D, this.workingServer.averagePillarHeight, 0.1D, 1,
                val -> this.workingServer.averagePillarHeight = Math.round(val * 10.0D) / 10.0D
        );
        avgHeight.active = !this.workingServer.infiniteHeight;
        heightRef[0] = avgHeight;
        addScrolled(avgHeight, y);
        y += 24;

        addScrolled(makeIntSlider(
                leftX, halfW, "eternalsnow.gui.admin.min_precip_speed", "x",
                0, 100, this.workingServer.minAccumulationSpeed, 1,
                v -> this.workingServer.minAccumulationSpeed = v
        ), y);
        addScrolled(makeIntSlider(
                rightX, halfW, "eternalsnow.gui.admin.max_precip_speed", "x",
                0, 150, this.workingServer.maxAccumulationSpeed, 1,
                v -> this.workingServer.maxAccumulationSpeed = v
        ), y);
        y += 24;

        // v2.6.5: how many snow blocks may be placed per second (10 .. 1200)
        addScrolled(makeIntSlider(
                leftX, halfW, "eternalsnow.gui.admin.snow_rate", "eternalsnow.gui.unit.blocks_per_sec",
                10, 1200, this.workingServer.maxBlockUpdatesPerTick, 10,
                v -> this.workingServer.maxBlockUpdatesPerTick = v
        ), y);
        y += 24;

        final ForgeSlider[] windDriftRef = new ForgeSlider[1];
        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.admin.auto_wind_drifts"),
                this.workingServer.autoWindDrift,
                v -> {
                    this.workingServer.autoWindDrift = v;
                    if (windDriftRef[0] != null) windDriftRef[0].active = !v;
                }
        ), y);
        ForgeSlider manualWindDrifts = makeIntSlider(
                rightX, halfW, "eternalsnow.gui.admin.wind_drifts", "%",
                0, 200, this.workingServer.windDriftStrengthPercent, 5,
                v -> this.workingServer.windDriftStrengthPercent = v
        );
        manualWindDrifts.active = !this.workingServer.autoWindDrift;
        windDriftRef[0] = manualWindDrifts;
        addScrolled(manualWindDrifts, y);
        y += 24;

        addScrolled(makeIntSlider(
                leftX, fullW, "eternalsnow.gui.admin.trample_chance", "%",
                0, 100, this.workingServer.trampleChancePercent, 5,
                v -> this.workingServer.trampleChancePercent = v
        ), y);
        y += 24;

        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.admin.smooth_drifts"),
                this.workingServer.smoothDrifts,
                v -> this.workingServer.smoothDrifts = v
        ), y);
        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                rightX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.admin.auto_thaw"),
                this.workingServer.autoThawExcess,
                v -> this.workingServer.autoThawExcess = v
        ), y);
    }

    private void buildAdminSurfaces(int leftX, int rightX, int fullW, int halfW) {
        int y = addAdminBackHeader(leftX, fullW, ADMIN_HUB, "eternalsnow.gui.tab.admin_main", "eternalsnow.gui.cat.surfaces");

        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.surf.freeze_water"),
                this.workingServer.freezeWater,
                v -> this.workingServer.freezeWater = v
        ), y);
        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                rightX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.surf.snow_on_ice"),
                this.workingServer.snowOnIce,
                v -> this.workingServer.snowOnIce = v
        ), y);
        y += 24;

        addScrolled(new EternalSnowWidgets.ModeSelectorButton(
                leftX, 0, fullW, 20,
                Component.translatable("eternalsnow.gui.surf.leaves_mode"),
                new int[]{0, 1, 2},
                this.workingServer.leavesMode,
                mode -> switch (mode) {
                    case 0 -> Component.translatable("eternalsnow.gui.surf.leaves.none");
                    case 1 -> Component.translatable("eternalsnow.gui.surf.leaves.on");
                    default -> Component.translatable("eternalsnow.gui.surf.leaves.through");
                },
                v -> this.workingServer.leavesMode = v
        ), y);
        y += 24;

        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.surf.crush_leaves"),
                this.workingServer.crushLeavesUnderWeight,
                v -> this.workingServer.crushLeavesUnderWeight = v
        ), y);
        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                rightX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.admin.bury_plants"),
                this.workingServer.buryPlants,
                v -> this.workingServer.buryPlants = v
        ), y);
        y += 26;

        addScrolled(new EternalSnowWidgets.SectionHeader(leftX, 0, fullW, Component.translatable("eternalsnow.gui.folder.surf_blocks")), y);
        y += 18;

        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.surf.slabs"),
                this.workingServer.snowOnSlabs,
                v -> this.workingServer.snowOnSlabs = v
        ), y);
        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                rightX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.surf.stairs"),
                this.workingServer.snowOnStairs,
                v -> this.workingServer.snowOnStairs = v
        ), y);
        y += 24;

        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.surf.glass"),
                this.workingServer.snowOnGlass,
                v -> this.workingServer.snowOnGlass = v
        ), y);
        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                rightX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.surf.paths"),
                this.workingServer.snowOnPathsAndFarmland,
                v -> this.workingServer.snowOnPathsAndFarmland = v
        ), y);
    }

    private void buildAdminThermalRoot(int leftX, int fullW) {
        int y = addAdminBackHeader(leftX, fullW, ADMIN_HUB, "eternalsnow.gui.tab.admin_main", "eternalsnow.gui.cat.thermal");

        addScrolled(makeDoubleSlider(
                leftX, fullW, "eternalsnow.gui.therm.global_bonus", "°C",
                -40.0D, 100.0D, this.workingServer.globalHeatBonusC, 1.0D, 0,
                v -> this.workingServer.globalHeatBonusC = v.floatValue()
        ), y);
        y += 23;

        addScrolled(makeDoubleSlider(
                leftX, fullW, "eternalsnow.gui.therm.global_speed", "x",
                0.2D, 4.0D, this.workingServer.globalHeatingSpeedMultiplier, 0.1D, 1,
                v -> this.workingServer.globalHeatingSpeedMultiplier = v.floatValue()
        ), y);
        y += 23;

        // v2.5.2: whole-room heating balance (envelope resistance model)
        addScrolled(makeDoubleSlider(
                leftX, fullW, "eternalsnow.gui.therm.room_power", "%",
                0.0D, 300.0D, this.workingServer.roomHeatingPowerPercent, 5.0D, 0,
                v -> this.workingServer.roomHeatingPowerPercent = v.intValue()
        ), y);
        y += 23;

        // v2.5.3: own section for the two GLOBAL material multipliers.
        addScrolled(new EternalSnowWidgets.SectionHeader(
                leftX, 0, fullW, Component.translatable("eternalsnow.gui.sec.therm_global_mult")), y);
        y += 18;

        addScrolled(makeDoubleSlider(
                leftX, fullW, "eternalsnow.gui.therm.cond_mult", "x",
                0.20D, 3.00D, this.workingServer.globalConductivityMultiplier, 0.05D, 2,
                v -> this.workingServer.globalConductivityMultiplier = v.floatValue()
        ), y);
        y += 23;

        addScrolled(makeDoubleSlider(
                leftX, fullW, "eternalsnow.gui.therm.cap_mult", "x",
                0.20D, 3.00D, this.workingServer.globalCapacityMultiplier, 0.05D, 2,
                v -> this.workingServer.globalCapacityMultiplier = v.floatValue()
        ), y);
        y += 26;

        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, fullW, 20,
                Component.translatable("eternalsnow.gui.therm.melt_roof"),
                this.workingServer.meltRoofSnowFromWarmBlocks,
                v -> this.workingServer.meltRoofSnowFromWarmBlocks = v
        ), y);
        y += 25;

        addScrolled(new EternalSnowWidgets.SectionHeader(leftX, 0, fullW, Component.translatable("eternalsnow.gui.sec.thermal_folders")), y);
        y += 18;

        addScrolled(new EternalSnowWidgets.FolderButton(
                leftX, 0, fullW, 26,
                Component.translatable("eternalsnow.gui.folder.therm_sources"),
                Component.translatable("eternalsnow.gui.folder.therm_sources.sub"),
                () -> openAdminPage(ADMIN_THERM_SOURCES)
        ), y);
        y += 29;

        addScrolled(new EternalSnowWidgets.FolderButton(
                leftX, 0, fullW, 26,
                Component.translatable("eternalsnow.gui.folder.therm_cond"),
                Component.translatable("eternalsnow.gui.folder.therm_cond.sub"),
                () -> openAdminPage(ADMIN_THERM_COND)
        ), y);
        y += 29;

        addScrolled(new EternalSnowWidgets.FolderButton(
                leftX, 0, fullW, 26,
                Component.translatable("eternalsnow.gui.folder.therm_cap"),
                Component.translatable("eternalsnow.gui.folder.therm_cap.sub"),
                () -> openAdminPage(ADMIN_THERM_CAP)
        ), y);
    }

    private void buildAdminThermalSources(int leftX, int fullW) {
        int y = addAdminBackHeader(leftX, fullW, ADMIN_THERMAL, "eternalsnow.gui.cat.thermal", "eternalsnow.gui.folder.therm_sources");

        addScrolled(makeDoubleSlider(leftX, fullW, "eternalsnow.gui.therm.torch", "°C",
                0, 80, this.workingServer.heatTorch, 1, 0, v -> this.workingServer.heatTorch = v.floatValue()), y);
        y += 23;
        addScrolled(makeDoubleSlider(leftX, fullW, "eternalsnow.gui.therm.lantern", "°C",
                0, 80, this.workingServer.heatLantern, 1, 0, v -> this.workingServer.heatLantern = v.floatValue()), y);
        y += 23;
        addScrolled(makeDoubleSlider(leftX, fullW, "eternalsnow.gui.therm.furnace", "°C",
                0, 140, this.workingServer.heatFurnace, 2, 0, v -> this.workingServer.heatFurnace = v.floatValue()), y);
        y += 23;
        addScrolled(makeDoubleSlider(leftX, fullW, "eternalsnow.gui.therm.blast", "°C",
                0, 160, this.workingServer.heatBlastSmoker, 2, 0, v -> this.workingServer.heatBlastSmoker = v.floatValue()), y);
        y += 23;
        addScrolled(makeDoubleSlider(leftX, fullW, "eternalsnow.gui.therm.campfire", "°C",
                0, 160, this.workingServer.heatCampfire, 2, 0, v -> this.workingServer.heatCampfire = v.floatValue()), y);
        y += 23;
        addScrolled(makeDoubleSlider(leftX, fullW, "eternalsnow.gui.therm.soul_campfire", "°C",
                0, 160, this.workingServer.heatSoulCampfire, 2, 0, v -> this.workingServer.heatSoulCampfire = v.floatValue()), y);
        y += 23;
        addScrolled(makeDoubleSlider(leftX, fullW, "eternalsnow.gui.therm.fireplace", "°C",
                0, 180, this.workingServer.heatFireplace, 2, 0, v -> this.workingServer.heatFireplace = v.floatValue()), y);
        y += 23;
        addScrolled(makeDoubleSlider(leftX, fullW, "eternalsnow.gui.therm.magma", "°C",
                0, 180, this.workingServer.heatMagma, 2, 0, v -> this.workingServer.heatMagma = v.floatValue()), y);
        y += 23;
        addScrolled(makeDoubleSlider(leftX, fullW, "eternalsnow.gui.therm.lava", "°C",
                0, 220, this.workingServer.heatLava, 5, 0, v -> this.workingServer.heatLava = v.floatValue()), y);
    }

    private void buildAdminThermalCond(int leftX, int fullW) {
        int y = addAdminBackHeader(leftX, fullW, ADMIN_THERMAL, "eternalsnow.gui.cat.thermal", "eternalsnow.gui.folder.therm_cond");
        for (BlockThermalCategory cat : BlockThermalCategory.VALUES) {
            final int idx = cat.ordinal();
            Component catName = Component.translatable(cat.getTranslationKey());
            ForgeSlider condSlider = new ForgeSlider(
                    leftX, 0, fullW, 20,
                    Component.translatable("eternalsnow.gui.therm.cond_full_fmt", catName),
                    Component.empty(),
                    0.01D, 1.00D, this.workingServer.blockConductivity[idx], 0.01D, 2, true
            ) {
                @Override
                protected void applyValue() {
                    EternalSnowConfigScreen.this.workingServer.blockConductivity[idx] = (float) this.getValue();
                }
            };
            addScrolled(condSlider, y);
            y += 23;
        }
    }

    private void buildAdminThermalCap(int leftX, int fullW) {
        int y = addAdminBackHeader(leftX, fullW, ADMIN_THERMAL, "eternalsnow.gui.cat.thermal", "eternalsnow.gui.folder.therm_cap");
        for (BlockThermalCategory cat : BlockThermalCategory.VALUES) {
            final int idx = cat.ordinal();
            Component catName = Component.translatable(cat.getTranslationKey());
            ForgeSlider capSlider = new ForgeSlider(
                    leftX, 0, fullW, 20,
                    Component.translatable("eternalsnow.gui.therm.cap_full_fmt", catName),
                    Component.empty(),
                    0.2D, 15.0D, this.workingServer.blockCapacity[idx], 0.2D, 1, true
            ) {
                @Override
                protected void applyValue() {
                    EternalSnowConfigScreen.this.workingServer.blockCapacity[idx] = (float) this.getValue();
                }
            };
            addScrolled(capSlider, y);
            y += 23;
        }
    }

    private void buildAdminSurvival(int leftX, int rightX, int fullW, int halfW) {
        int y = addAdminBackHeader(leftX, fullW, ADMIN_HUB, "eternalsnow.gui.tab.admin_main", "eternalsnow.gui.cat.survival");

        addScrolled(new EternalSnowWidgets.SectionHeader(leftX, 0, fullW, Component.translatable("eternalsnow.gui.folder.surv_climate")), y);
        y += 18;

        addScrolled(makeIntSlider(
                leftX, halfW, "eternalsnow.gui.surv.base_temp", "°C",
                -55, 0, this.workingServer.minWorldTempC, 1,
                v -> this.workingServer.minWorldTempC = v
        ), y);
        addScrolled(makeIntSlider(
                rightX, halfW, "eternalsnow.gui.surv.max_temp", "°C",
                -40, 15, this.workingServer.maxWorldTempC, 1,
                v -> this.workingServer.maxWorldTempC = v
        ), y);
        y += 26;

        addScrolled(new EternalSnowWidgets.SectionHeader(leftX, 0, fullW, Component.translatable("eternalsnow.gui.folder.surv_body")), y);
        y += 18;

        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, fullW, 20,
                Component.translatable("eternalsnow.gui.surv.player_temp"),
                this.workingServer.enablePlayerTemperature,
                v -> this.workingServer.enablePlayerTemperature = v
        ), y);
        y += 24;
        addScrolled(makeDoubleSlider(
                leftX, halfW, "eternalsnow.gui.surv.temp_rate", "x",
                0.1D, 5.0D, this.workingServer.playerTempRateMultiplier, 0.1D, 1,
                v -> this.workingServer.playerTempRateMultiplier = v.floatValue()
        ), y);
        addScrolled(makeDoubleSlider(
                rightX, halfW, "eternalsnow.gui.surv.armor_insulation_mult", "x",
                0.0D, 3.0D, this.workingServer.armorInsulationMultiplier, 0.1D, 1,
                v -> this.workingServer.armorInsulationMultiplier = v.floatValue()
        ), y);
        y += 26;

        addScrolled(new EternalSnowWidgets.SectionHeader(leftX, 0, fullW, Component.translatable("eternalsnow.gui.folder.surv_thirst")), y);
        y += 18;

        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, halfW, 20,
                Component.translatable("eternalsnow.gui.surv.thirst"),
                this.workingServer.enableThirst,
                v -> this.workingServer.enableThirst = v
        ), y);
        addScrolled(makeDoubleSlider(
                rightX, halfW, "eternalsnow.gui.surv.thirst_rate", "x",
                0.1D, 5.0D, this.workingServer.thirstDrainMultiplier, 0.1D, 1,
                v -> this.workingServer.thirstDrainMultiplier = v.floatValue()
        ), y);
        y += 24;

        addScrolled(new EternalSnowWidgets.SectionHeader(leftX, 0, fullW, Component.translatable("eternalsnow.gui.surv.custom_drinks_label")), y);
        y += 18;

        EditBox drinksBox = new EditBox(
                this.font, leftX, 0, fullW, 20,
                Component.translatable("eternalsnow.gui.surv.custom_drinks")
        );
        drinksBox.setMaxLength(1024);
        drinksBox.setValue(this.workingServer.customDrinks != null ? this.workingServer.customDrinks : "");
        drinksBox.setHint(Component.translatable("eternalsnow.gui.surv.custom_drinks_hint"));
        drinksBox.setResponder(text -> this.workingServer.customDrinks = text);
        addScrolled(drinksBox, y);
        y += 26;

        addScrolled(new EternalSnowWidgets.SectionHeader(leftX, 0, fullW, Component.translatable("eternalsnow.gui.folder.surv_mobs")), y);
        y += 18;

        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, fullW, 20,
                Component.translatable("eternalsnow.gui.surv.mobs_no_burn"),
                this.workingServer.mobsDoNotBurnInSnow,
                v -> this.workingServer.mobsDoNotBurnInSnow = v
        ), y);
        y += 24;
        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, fullW, 20,
                Component.translatable("eternalsnow.gui.surv.mobs_freeze"),
                this.workingServer.enableMobFreezing,
                v -> this.workingServer.enableMobFreezing = v
        ), y);

        // v2.7: food systems, switchable separately — some players want freezing without rot
        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, fullW, 20,
                Component.translatable("eternalsnow.gui.surv.food_freezing"),
                this.workingServer.enableFoodFreezing,
                v -> this.workingServer.enableFoodFreezing = v
        ), y);
        y += 24;
        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, fullW, 20,
                Component.translatable("eternalsnow.gui.surv.food_spoilage"),
                this.workingServer.enableFoodSpoilage,
                v -> this.workingServer.enableFoodSpoilage = v
        ), y);
        y += 24;
        y += 24;
        addScrolled(makeDoubleSlider(
                leftX, halfW, "eternalsnow.gui.surv.mobs_freeze_rate", "x",
                0.05D, 1.0D, this.workingServer.mobFreezeRateMultiplier, 0.05D, 2,
                v -> this.workingServer.mobFreezeRateMultiplier = v.floatValue()
        ), y);
        y += 26;

        addScrolled(new EternalSnowWidgets.ToggleSwitch(
                leftX, 0, fullW, 20,
                Component.translatable("eternalsnow.gui.surv.convection"),
                this.workingServer.enableConvection,
                v -> this.workingServer.enableConvection = v
        ), y);
        y += 24;
        addScrolled(makeIntSlider(
                leftX, halfW, "eternalsnow.gui.surv.convection_strength", "%",
                0, 300, this.workingServer.convectionStrengthPercent, 10,
                v -> this.workingServer.convectionStrengthPercent = v
        ), y);
        y += 26;

        addScrolled(new EternalSnowWidgets.SectionHeader(leftX, 0, fullW, Component.translatable("eternalsnow.gui.folder.surv_golem")), y);
        y += 18;

        addScrolled(makeIntSlider(
                leftX, halfW, "eternalsnow.gui.surv.golem_interval", " тик",
                10, 120, this.workingServer.golemSwingIntervalTicks, 5,
                v -> this.workingServer.golemSwingIntervalTicks = v
        ), y);
        addScrolled(makeIntSlider(
                rightX, halfW, "eternalsnow.gui.surv.golem_max_radius", " бл.",
                4, 48, this.workingServer.golemMaxRadius, 1,
                v -> this.workingServer.golemMaxRadius = v
        ), y);
        y += 24;
        addScrolled(makeIntSlider(
                leftX, fullW, "eternalsnow.gui.surv.shovel_durability", " ед.",
                64, 4096, this.workingServer.shovelDurability, 32,
                v -> this.workingServer.shovelDurability = v
        ), y);
    }

    private ForgeSlider makeIntSlider(
            int x, int w, String key, String suffix,
            int min, int max, int current, int step, Consumer<Integer> setter
    ) {
        return new ForgeSlider(
                x, 0, w, 20,
                Component.translatable(key),
                // v2.6.5: a dotted suffix is treated as a translation key (e.g. blocks/sec units)
                suffix.isEmpty()
                        ? Component.empty()
                        : (suffix.indexOf('.') >= 0 ? Component.translatable(suffix) : Component.literal(suffix)),
                min, max, current, step, 0, true
        ) {
            @Override
            protected void applyValue() {
                setter.accept(this.getValueInt());
            }
        };
    }

    private ForgeSlider makeDoubleSlider(
            int x, int w, String key, String suffixKeyOrLiteral,
            double min, double max, double current, double step, int precision,
            Consumer<Double> setter
    ) {
        Component suffix = suffixKeyOrLiteral.startsWith("eternalsnow.")
                ? Component.translatable(suffixKeyOrLiteral)
                : Component.literal(suffixKeyOrLiteral);
        return new ForgeSlider(
                x, 0, w, 20,
                Component.translatable(key),
                suffix,
                min, max, current, step, precision, true
        ) {
            @Override
            protected void applyValue() {
                setter.accept(this.getValue());
            }
        };
    }

    private void applyScrollPositions() {
        int viewportH = Math.max(20, this.viewportBottom - this.viewportTop);
        double maxScroll = Math.max(0.0D, this.contentHeight - viewportH);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0.0D, maxScroll);

        for (ScrollEntry entry : this.scrollEntries) {
            int screenY = this.viewportTop + entry.localY - (int) Math.round(this.scrollOffset);
            entry.widget.setY(screenY);
            entry.widget.visible = (screenY + entry.widget.getHeight() > this.viewportTop) && (screenY < this.viewportBottom);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= this.viewportTop && mouseY <= this.viewportBottom) {
            this.scrollOffset -= delta * 18.0D;
            applyScrollPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseY < this.viewportTop || mouseY > this.viewportBottom) {
            for (ScrollEntry entry : this.scrollEntries) {
                if (entry.widget.isMouseOver(mouseX, mouseY)) {
                    return false;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void resetCurrentTabDefaults() {
        ServerSnowSettings def = new ServerSnowSettings();
        if (this.activeTab == TAB_CLIENT) {
            this.clientVisibleSnow = true;
            this.clientAutoWeatherParticles = true;
            this.clientParticleCount = 8500;
            this.clientSnowRadius = 24;
            this.clientFlakeSizePct = 130;
            this.clientFlakeOpacityPct = 92;
            this.clientWindVolumePct = 100;
            this.clientFaceSnow = true;
            this.clientClearSky = true;
            this.clientShowHud = true;
        } else if (this.canEditAdmin) {
            this.workingServer.copyFrom(def);
        }
    }

    private void saveAndClose() {
        ClientSnowConfig.visibleSnowfall = this.clientVisibleSnow;
        ClientSnowConfig.autoWeatherParticles = this.clientAutoWeatherParticles;
        ClientSnowConfig.snowParticleCount = this.clientParticleCount;
        ClientSnowConfig.snowfallRadius = this.clientSnowRadius;
        ClientSnowConfig.snowflakeSizePercent = this.clientFlakeSizePct;
        ClientSnowConfig.snowflakeOpacityPercent = this.clientFlakeOpacityPct;
        ClientSnowConfig.windSoundVolumePercent = this.clientWindVolumePct;
        ClientSnowConfig.faceSnowOverlay = this.clientFaceSnow;
        ClientSnowConfig.clearSkyAndLight = this.clientClearSky;
        ClientSnowConfig.showSurvivalHud = this.clientShowHud;
        ClientSnowConfig.save();

        if (this.canEditAdmin) {
            this.workingServer.clamp();
            EternalSnowState.getServerSettings().copyFrom(this.workingServer);
            EternalSnowState.updateClientSyncedSettings(this.workingServer, true);

            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.getConnection() != null) {
                PacketHandler.sendToServer(new ServerboundUpdateSettingsPacket(this.workingServer));
            }
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }

    @Override
    public void onClose() {
        // ESC inside a sub-folder steps back to the parent Admin page first!
        if (this.activeTab == TAB_ADMIN && this.adminPage != ADMIN_HUB) {
            if (this.adminPage >= 30) {
                openAdminPage(ADMIN_THERMAL);
            } else {
                openAdminPage(ADMIN_HUB);
            }
            return;
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // Gentle, peaceful falling snow specks in the menu backdrop (subtle frosty atmosphere, zero neon!)
        long ms = System.currentTimeMillis();
        for (int i = 0; i < 36; i++) {
            int hash = i * 73856093;
            int sx = Math.floorMod(hash + (int) ((ms / 45L) * (1 + (i & 1))), Math.max(1, this.width));
            int sy = Math.floorMod((hash >> 8) + (int) ((ms / (22L + (i % 9) * 4L))), Math.max(1, this.height));
            int sz = (i % 3 == 0) ? 2 : 1;
            graphics.fill(sx, sy, sx + sz, sy + sz, 0x44EDF3F8);
        }

        int centerX = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, centerX, 6, 0xFFF2F6FA);

        // Live Climate Readout Bar on Admin tab (calm dark-stone & snow-white styling)
        if (this.activeTab == TAB_ADMIN) {
            graphics.fill(this.panelLeft, 43, this.panelRight, 65, 0xDD22262C);
            graphics.renderOutline(this.panelLeft, 43, this.panelRight - this.panelLeft, 22, 0xFF88929E);

            WeatherEventType ev = EternalSnowState.getActiveWeatherEvent();
            int windPct = Math.round(EternalSnowState.windSpeed * 100.0F);
            int yaw = Math.round(EternalSnowState.windYawDeg);
            String worldT = String.format(Locale.ROOT, "%+.1f°C", EternalSnowState.clientStreetTempC);
            String localT = String.format(Locale.ROOT, "%+.1f°C", EternalSnowState.clientLocalTempC);
            String bodyT = String.format(Locale.ROOT, "%.1f°C", EternalSnowState.clientBodyTempC);
            float livePrecip = this.workingServer.computeCurrentPrecipitationSpeed(ev, EternalSnowState.snowIntensity);
            String precipStr = String.format(Locale.ROOT, "%.1fx", livePrecip);

            Component line1 = Component.translatable(
                    "eternalsnow.gui.thermometer.line1",
                    Component.translatable(ev.getTranslationKey()),
                    precipStr,
                    windPct,
                    yaw,
                    worldT
            );
            Component line2 = Component.translatable(
                    "eternalsnow.gui.thermometer.line2",
                    localT,
                    bodyT
            );
            graphics.drawCenteredString(this.font, line1, centerX, 46, 0xFFE6EDF4);
            graphics.drawCenteredString(this.font, line2, centerX, 55, 0xFFC8D2DC);
        }

        // Scrollable Vanilla-Stone Backdrop
        graphics.fill(this.panelLeft, this.viewportTop, this.panelRight, this.viewportBottom, 0xBF1A1D22);
        graphics.renderOutline(this.panelLeft, this.viewportTop, this.panelRight - this.panelLeft, this.viewportBottom - this.viewportTop, 0xFF707A86);
        graphics.fill(this.panelLeft + 1, this.viewportTop + 1, this.panelRight - 1, this.viewportTop + 2, 0xFFE8EEF4);

        for (ScrollEntry entry : this.scrollEntries) {
            entry.savedVisible = entry.widget.visible;
            entry.widget.visible = false;
        }
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.enableScissor(this.panelLeft + 1, this.viewportTop + 2, this.panelRight - 1, this.viewportBottom - 1);
        for (ScrollEntry entry : this.scrollEntries) {
            entry.widget.visible = entry.savedVisible;
            if (entry.widget.visible) {
                entry.widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }
        graphics.disableScissor();

        // Calm snow-silver scrollbar
        int viewportH = Math.max(1, this.viewportBottom - this.viewportTop);
        if (this.contentHeight > viewportH) {
            int barX = this.panelRight - 5;
            graphics.fill(barX, this.viewportTop + 3, barX + 3, this.viewportBottom - 2, 0x66282C32);
            int thumbH = Math.max(18, (int) ((double) viewportH * viewportH / this.contentHeight));
            double maxScroll = this.contentHeight - viewportH;
            int thumbY = this.viewportTop + 3 + (int) ((this.scrollOffset / maxScroll) * (viewportH - 5 - thumbH));
            graphics.fill(barX, thumbY, barX + 3, thumbY + thumbH, 0xFFDCE4EC);
        }
    }

    private static final class ScrollEntry {
        final AbstractWidget widget;
        final int localY;
        boolean savedVisible;

        ScrollEntry(AbstractWidget widget, int localY) {
            this.widget = widget;
            this.localY = localY;
        }
    }
}
