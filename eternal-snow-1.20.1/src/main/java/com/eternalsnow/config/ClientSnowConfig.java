package com.eternalsnow.config;

import net.minecraft.util.Mth;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class ClientSnowConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;

    public static final ForgeConfigSpec.BooleanValue CFG_VISIBLE_SNOWFALL;
    public static final ForgeConfigSpec.BooleanValue CFG_AUTO_WEATHER_PARTICLES;
    public static final ForgeConfigSpec.IntValue CFG_SNOW_PARTICLE_COUNT;
    public static final ForgeConfigSpec.IntValue CFG_SNOWFALL_RADIUS;
    public static final ForgeConfigSpec.IntValue CFG_SNOWFLAKE_SIZE_PCT;
    public static final ForgeConfigSpec.IntValue CFG_SNOWFLAKE_OPACITY_PCT;
    public static final ForgeConfigSpec.IntValue CFG_WIND_SOUND_VOLUME_PCT;
    public static final ForgeConfigSpec.BooleanValue CFG_CLEAR_SKY_AND_LIGHT;
    public static final ForgeConfigSpec.BooleanValue CFG_FACE_SNOW_OVERLAY;
    public static final ForgeConfigSpec.BooleanValue CFG_SHOW_SURVIVAL_HUD;
    public static final ForgeConfigSpec.BooleanValue CFG_FROSTED_WINDOWS;
    public static final ForgeConfigSpec.BooleanValue CFG_FOOTPRINTS;

    public static volatile boolean visibleSnowfall = true;
    public static volatile boolean autoWeatherParticles = true;
    public static volatile int snowParticleCount = 8500;       // 500 .. 20000
    public static volatile int snowfallRadius = 24;            // 8 .. 40 blocks
    public static volatile int snowflakeSizePercent = 130;     // 50 .. 250%
    public static volatile int snowflakeOpacityPercent = 92;   // 20 .. 100%
    public static volatile int windSoundVolumePercent = 100;   // 0 .. 200%
    public static volatile boolean clearSkyAndLight = true;
    public static volatile boolean faceSnowOverlay = true;
    public static volatile boolean showSurvivalHud = true;
    public static volatile boolean frostedWindows = true;
    public static volatile boolean footprints = true;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Client Settings for Eternal Snow 2.1").push("client_snowfall");

        CFG_VISIBLE_SNOWFALL = builder.define("visibleSnowfall", true);
        CFG_AUTO_WEATHER_PARTICLES = builder.define("autoWeatherParticles", true);
        CFG_SNOW_PARTICLE_COUNT = builder.defineInRange("snowParticleCount", 8500, 500, 20000);
        CFG_SNOWFALL_RADIUS = builder.defineInRange("snowfallRadius", 24, 8, 40);
        CFG_SNOWFLAKE_SIZE_PCT = builder.defineInRange("snowflakeSizePercent", 130, 50, 250);
        CFG_SNOWFLAKE_OPACITY_PCT = builder.defineInRange("snowflakeOpacityPercent", 92, 20, 100);
        CFG_WIND_SOUND_VOLUME_PCT = builder.defineInRange("windSoundVolumePercent", 100, 0, 200);
        CFG_CLEAR_SKY_AND_LIGHT = builder.define("clearSkyAndLight", true);
        CFG_FACE_SNOW_OVERLAY = builder.define("faceSnowOverlay", true);
        CFG_SHOW_SURVIVAL_HUD = builder.define("showSurvivalHud", true);
        CFG_FROSTED_WINDOWS = builder.define("frostedWindows", true);
        CFG_FOOTPRINTS = builder.define("footprints", true);

        builder.pop();
        CLIENT_SPEC = builder.build();
    }

    private ClientSnowConfig() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, "eternalsnow-client.toml");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSnowConfig::onLoad);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSnowConfig::onReload);
    }

    private static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == CLIENT_SPEC) {
            refreshFromSpec();
        }
    }

    private static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == CLIENT_SPEC) {
            refreshFromSpec();
        }
    }

    public static void refreshFromSpec() {
        try {
            if (CLIENT_SPEC.isLoaded()) {
                visibleSnowfall = CFG_VISIBLE_SNOWFALL.get();
                autoWeatherParticles = CFG_AUTO_WEATHER_PARTICLES.get();
                snowParticleCount = Mth.clamp(CFG_SNOW_PARTICLE_COUNT.get(), 500, 20000);
                snowfallRadius = Mth.clamp(CFG_SNOWFALL_RADIUS.get(), 8, 40);
                snowflakeSizePercent = Mth.clamp(CFG_SNOWFLAKE_SIZE_PCT.get(), 50, 250);
                snowflakeOpacityPercent = Mth.clamp(CFG_SNOWFLAKE_OPACITY_PCT.get(), 20, 100);
                windSoundVolumePercent = Mth.clamp(CFG_WIND_SOUND_VOLUME_PCT.get(), 0, 200);
                clearSkyAndLight = CFG_CLEAR_SKY_AND_LIGHT.get();
                faceSnowOverlay = CFG_FACE_SNOW_OVERLAY.get();
                showSurvivalHud = CFG_SHOW_SURVIVAL_HUD.get();
                frostedWindows = CFG_FROSTED_WINDOWS.get();
                footprints = CFG_FOOTPRINTS.get();
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        snowParticleCount = Mth.clamp(snowParticleCount, 500, 20000);
        snowfallRadius = Mth.clamp(snowfallRadius, 8, 40);
        snowflakeSizePercent = Mth.clamp(snowflakeSizePercent, 50, 250);
        snowflakeOpacityPercent = Mth.clamp(snowflakeOpacityPercent, 20, 100);
        windSoundVolumePercent = Mth.clamp(windSoundVolumePercent, 0, 200);
        try {
            if (CLIENT_SPEC.isLoaded()) {
                CFG_VISIBLE_SNOWFALL.set(visibleSnowfall);
                CFG_AUTO_WEATHER_PARTICLES.set(autoWeatherParticles);
                CFG_SNOW_PARTICLE_COUNT.set(snowParticleCount);
                CFG_SNOWFALL_RADIUS.set(snowfallRadius);
                CFG_SNOWFLAKE_SIZE_PCT.set(snowflakeSizePercent);
                CFG_SNOWFLAKE_OPACITY_PCT.set(snowflakeOpacityPercent);
                CFG_WIND_SOUND_VOLUME_PCT.set(windSoundVolumePercent);
                CFG_CLEAR_SKY_AND_LIGHT.set(clearSkyAndLight);
                CFG_FACE_SNOW_OVERLAY.set(faceSnowOverlay);
                CFG_SHOW_SURVIVAL_HUD.set(showSurvivalHud);
                CLIENT_SPEC.save();
            }
        } catch (Exception ignored) {
        }
    }
}
