package com.eternalsnow.world;

import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.state.EternalSnowState;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class EternalSnowSavedData extends SavedData {
    public static final String DATA_NAME = "eternalsnow_settings";

    private ServerSnowSettings settings;
    private final Long2IntOpenHashMap campfireFuelMap = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap campfireEmberMap = new Long2IntOpenHashMap();

    public EternalSnowSavedData() {
        this.settings = new ServerSnowSettings();
        this.campfireFuelMap.defaultReturnValue(0);
        this.campfireEmberMap.defaultReturnValue(0);
    }

    public EternalSnowSavedData(ServerSnowSettings settings) {
        this();
        this.settings = settings.copy();
        this.settings.clamp();
    }

    public Long2IntOpenHashMap getCampfireFuelMap() {
        return this.campfireFuelMap;
    }

    public Long2IntOpenHashMap getCampfireEmberMap() {
        return this.campfireEmberMap;
    }

    public static EternalSnowSavedData load(CompoundTag tag) {
        ServerSnowSettings s = new ServerSnowSettings();
        s.loadFromTag(tag);
        if (tag.contains("activeWeatherEvent")) {
            EternalSnowState.weatherEventId = tag.getInt("activeWeatherEvent");
            EternalSnowState.windYawDeg = tag.getFloat("activeWindYaw");
            EternalSnowState.windSpeed = tag.getFloat("activeWindSpeed");
            EternalSnowState.snowIntensity = tag.getFloat("activeSnowIntensity");
            EternalSnowState.eventTicksRemaining = tag.getInt("activeEventTicks");
        }
        EternalSnowSavedData loaded = new EternalSnowSavedData(s);
        CampfireFuelManager.loadFromTag(tag, loaded.campfireFuelMap);
        return loaded;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        this.settings.saveToTag(tag);
        tag.putInt("activeWeatherEvent", EternalSnowState.weatherEventId);
        tag.putFloat("activeWindYaw", EternalSnowState.windYawDeg);
        tag.putFloat("activeWindSpeed", EternalSnowState.windSpeed);
        tag.putFloat("activeSnowIntensity", EternalSnowState.snowIntensity);
        tag.putInt("activeEventTicks", EternalSnowState.eventTicksRemaining);
        CampfireFuelManager.saveToTag(tag, this.campfireFuelMap);
        return tag;
    }

    public ServerSnowSettings getSettings() {
        return this.settings;
    }

    public void updateSettings(ServerSnowSettings newSettings) {
        this.settings = newSettings.copy();
        this.settings.clamp();
        this.setDirty();
    }

    public static EternalSnowSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                EternalSnowSavedData::load,
                EternalSnowSavedData::new,
                DATA_NAME
        );
    }
}
