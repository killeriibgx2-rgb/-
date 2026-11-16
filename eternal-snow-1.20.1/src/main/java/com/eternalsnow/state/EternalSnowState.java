package com.eternalsnow.state;

import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.weather.WeatherEventType;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class EternalSnowState {
    private static volatile boolean serverRunningInProcess = false;
    private static volatile ServerSnowSettings serverSettings = new ServerSnowSettings();
    private static volatile ServerSnowSettings clientSyncedSettings = new ServerSnowSettings();
    private static volatile boolean clientHasOpPermission = false;
    private static volatile boolean clientConnectedToServerMod = false;

    // Live Climate & Wind state (synced to clients for 3D Blizzard renderer, Fog, Face Snow & Admin Thermometer)
    public static volatile int activeWeatherEventOrdinal = WeatherEventType.LIGHT_SNOW.ordinal();
    public static volatile int weatherEventId = WeatherEventType.LIGHT_SNOW.ordinal();
    public static volatile float windAngleRad = 0.785F; // ~45 deg
    public static volatile float windYawDeg = 45.0F;
    public static volatile float windSpeed = 0.25F;     // 0.0 .. 1.0
    public static volatile float snowIntensity = 0.45F; // 0.0 .. 1.0
    public static volatile float worldAmbientTempC = -18.0F;   // server-authoritative live air
    public static volatile float thermalMassTempC = -18.0F;    // v2.5.3: slow walls/soil lag (server)
    public static volatile float clientStreetTempC = -18.0F;  // v2.5.3: client-only HUD street readout
    public static volatile int eventTicksRemaining = 4800;

    // Live Client Player Survival Telemetry (synced every 10 ticks to each player)
    public static volatile float clientLocalTempC = -18.0F;
    public static volatile float clientBodyTempC = 36.6F;
    public static volatile float clientBodyTempState = 0.0F;
    public static volatile int clientThirstLevel = 20;
    public static volatile float clientThirstSaturation = 5.0F;
    public static volatile float clientSmoothOvercast = 0.0F;

    // Live Look-At Campfire Telemetry (synced seamlessly in ClientboundPlayerSurvivalPacket)
    public static volatile long clientLookedCampfirePos = Long.MIN_VALUE;
    public static volatile int clientLookedCampfireTicks = 0;
    public static volatile boolean clientLookedCampfireWind = false;

    private EternalSnowState() {
    }

    public static ServerSnowSettings getServerSettings() {
        return serverSettings;
    }

    public static void setServerSettings(ServerSnowSettings settings) {
        if (settings != null) {
            settings.clamp();
            serverSettings = settings;
        }
    }

    public static void setServerRunning(boolean running) {
        serverRunningInProcess = running;
    }

    public static ServerSnowSettings getClientSyncedSettings() {
        return clientSyncedSettings;
    }

    public static void updateClientSyncedSettings(ServerSnowSettings settings, boolean canEdit) {
        if (settings != null) {
            settings.clamp();
            clientSyncedSettings = settings.copy();
        }
        clientHasOpPermission = canEdit;
        clientConnectedToServerMod = true;
    }

    public static void resetClientSession() {
        clientHasOpPermission = false;
        clientConnectedToServerMod = false;
        clientSyncedSettings = ServerSnowSettings.fromCommonDefaults();
        clientBodyTempC = 36.6F;
        clientBodyTempState = 0.0F;
        clientThirstLevel = 20;
        // v2.6.1: never keep another server's temperature on the HUD after a disconnect
        clientLocalTempC = -18.0F;
        clientStreetTempC = -18.0F;
        clientBodyTempC = 36.6F;
    }

    public static boolean canClientEditAdmin() {
        return clientHasOpPermission;
    }

    public static boolean isClientConnectedToServerMod() {
        return clientConnectedToServerMod;
    }

    public static ServerSnowSettings getEffectiveSettings() {
        return serverRunningInProcess ? serverSettings : clientSyncedSettings;
    }

    public static boolean isGlobalColdActive() {
        ServerSnowSettings s = getEffectiveSettings();
        return s.enabled && s.freezeWater;
    }

    public static boolean isSnowPrecipitationEverywhere() {
        ServerSnowSettings s = getEffectiveSettings();
        return s.enabled;
    }

    public static float getWindDirX() {
        return Mth.cos(windYawDeg * ((float) Math.PI / 180.0F));
    }

    public static float getWindDirZ() {
        return Mth.sin(windYawDeg * ((float) Math.PI / 180.0F));
    }

    public static WeatherEventType getActiveWeatherEvent() {
        return WeatherEventType.byOrdinal(weatherEventId);
    }

    public static boolean isOverworldLike(Level level) {
        if (level == null) {
            return false;
        }
        return level.dimension() == Level.OVERWORLD
                || (level.dimensionType().natural() && level.dimensionType().hasSkyLight());
    }

    public static boolean canPlayerManageServer(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (player.hasPermissions(2)) {
            return true;
        }
        MinecraftServer server = player.getServer();
        return server != null && server.isSingleplayerOwner(player.getGameProfile());
    }

    public static void tagBiomesFromRegistry(RegistryAccess registryAccess) {
        if (registryAccess == null) {
            return;
        }
        try {
            Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);
            for (Holder.Reference<Biome> holder : biomeRegistry.holders().toList()) {
                Object rawBiome = holder.value();
                if (rawBiome instanceof EternalSnowBiomeAccess access) {
                    boolean nonOverworld = holder.is(BiomeTags.IS_NETHER) || holder.is(BiomeTags.IS_END);
                    access.eternalsnow$setNonOverworldBiome(nonOverworld);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
