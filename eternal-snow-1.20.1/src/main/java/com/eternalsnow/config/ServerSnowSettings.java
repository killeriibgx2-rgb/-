package com.eternalsnow.config;

import com.eternalsnow.thermal.BlockThermalCategory;
import com.eternalsnow.weather.WeatherEventType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

public final class ServerSnowSettings {
    // 1. Weather & Snowfall
    public boolean enabled = true;
    public boolean autoWeatherEvents = true;
    public int forcedEventOrdinal = 1; // LIGHT_SNOW
    public int weatherChangeMinutes = 5;
    public boolean autoWind = true;
    public int manualWindAngleDeg = 45;
    public boolean autoWindDrift = true;
    public int windDriftStrengthPercent = 100;
    public boolean blizzardWindPush = true;

    public boolean infiniteHeight = true;
    public double averagePillarHeight = 3.0D;
    public static final float MIN_PRECIP_FLOOR = 0.35F; // never fully dry during real snowfall
    public int minAccumulationSpeed = 2;   // Precipitation speed in Light Snow (0..100)
    public int maxAccumulationSpeed = 18;  // Precipitation speed in Blizzard (0..150)
    public int accumulationSpeed = 6;      // Legacy fallback for older saves
    /** v2.6.5: peak snow blocks placed per SECOND (GUI slider 10..1200). */
    public int maxBlockUpdatesPerTick = 140;
    public boolean smoothDrifts = true;
    public boolean autoThawExcess = true;
    public boolean keepClearSkyLight = true;
    public int trampleChancePercent = 12;

    // 2. Surface Rules
    public boolean freezeWater = true;
    public boolean snowOnIce = true;
    public boolean snowOnSlabs = true;
    public int leavesMode = 2; // 0 = No, 1 = On Leaves, 2 = Pass Through Leaves
    public boolean crushLeavesUnderWeight = true;
    public boolean snowOnGlass = false;
    public boolean snowOnStairs = false;
    public boolean snowOnPathsAndFarmland = false;
    public boolean buryPlants = true;

    // 3. Climate Waves, Global Heating Controls & Heat Sources (°C)
    public int minWorldTempC = -34;
    public int maxWorldTempC = -6;
    public int nightColdDropC = 8;
    public boolean meltRoofSnowFromWarmBlocks = true;

    public float globalHeatBonusC = 0.0F;              // Adds +/- °C directly to ALL active heat sources!
    public float globalHeatingSpeedMultiplier = 1.0F;  // Scales warming speed & heat spread strength across all sources!

    // v2.5.2: enclosed-room lumped heat model (power in / envelope resistance out)
    public int roomHeatingPowerPercent = 100;          // 0..300: how strongly a fire heats the ROOM (not just nearby voxels)
    /**
     * Multiplier on a source's nominal power. Power is spread over the air of the fire's own
     * tile, so a large multiplier warms the room without frying a single cell. Measured on a
     * closed 8x8x8 brick room at -20 C outside: 10 lifts the room to about +1 C in 15 minutes
     * with sane air next to the fire, 3 barely moves it, and 30 cooks the room.
     */
    public float roomSourcePower = 10.0F;              // 1..200
    /** Air mixing above plain face contact, so a fire warms the whole room, not one voxel. */
    public float roomAirMixing = 20.0F;                // 1..100
    /**
     * Air equalisation sweeps per step. Each sweep spreads heat by one block, so a large hall
     * needs more sweeps than a small room. 0 disables forced mixing.
     */
    public int roomMixingSweeps = 12;                  // 0..64
    public float roomComfortCeilingC = 26.0F;          // LEGACY (2.6.x): kept for old saves, physics ignores it since 2.7

    // v2.7: food freezing + spoilage
    public int foodSpoilageSpeedPercent = 25;
    public int foodFreezingSpeedPercent = 100;
    public int foodThawingSpeedPercent = 100;
    public boolean enableFoodFreezing = true;
    public boolean enableFoodSpoilage = true;

    // v2.7.1: master switch for the furnace/food-shelf mixins. The dev environment is pinned to
    // Forge 47.2.0, so a mixin that applies here can still fail on a newer 47.x runtime; this lets a
    // player recover a broken world instead of staring at a mixin crash screen.
    public boolean enableCookingTweaks = true;

    // Measured: a 6x4x6 cellar rocking to +43 C with eight torches at 70 W each means the shell
    // leaks about 12 W/C, so ~21 W per torch gives the expected +10..15 C cellar instead of a
    // sauna. A torch is a candle-sized flame; a campfire is not, and keeps its own value.
    public float heatTorch = 4.0F;
    public float heatLantern = 10.0F;
    // Measured: a 5x5x3 plank room at -12 C holds 48 W/K of loss, so a campfire needs about
    // 1000 W to lift it by twenty degrees. The old 68 W could only manage one degree, which is
    // the "too slow" half of the owner's report. Torches and lanterns are scaled alongside.
    public float heatFurnace = 110.0F;
    public float heatBlastSmoker = 140.0F;
    public float heatCampfire = 150.0F;
    public float heatSoulCampfire = 100.0F;
    public float heatFireplace = 94.0F;
    public float heatMagma = 170.0F;
    public float heatLava = 260.0F;

    // 4. Per-Material Conductivity (0.01..1.00) & Capacity (0.2..15.0)
    // v2.5.3: two GLOBAL multipliers (their own section in the general thermal settings page).
    public float globalConductivityMultiplier = 1.0F;  // 0.2..3.0: scales EVERY material's heat flow
    public float globalCapacityMultiplier = 1.0F;      // 0.2..3.0: scales EVERY material's thermal mass
    public final float[] blockConductivity = new float[BlockThermalCategory.VALUES.length];
    public final float[] blockCapacity = new float[BlockThermalCategory.VALUES.length];

    // 5. Survival, Armor Insulation, Thirst, Shovel & Golem
    public boolean enablePlayerTemperature = true;
    public float playerTempRateMultiplier = 1.0F;
    public float armorInsulationMultiplier = 1.0F;
    public boolean enableThirst = true;
    public float thirstDrainMultiplier = 1.0F;
    public String customDrinks = "";
    public int shovelDurability = 650;
    public int golemSwingIntervalTicks = 40; // 2.0 seconds
    public int golemMaxRadius = 24;

    // 6. v2.6.6: Mobs — no burning during snowfall, slow freezing, vertical convection
    public boolean mobsDoNotBurnInSnow = true;     // snowfall / snow cover extinguishes fire on any mob
    public boolean enableMobFreezing = true;       // mobs get hypothermia (5x slower than the player)
    public float mobFreezeRateMultiplier = 0.20F;  // 0.05..1.0 of the player's chill rate
    public boolean enableConvection = true;        // cold floor / warm ceiling stratification
    public int convectionStrengthPercent = 100;    // 0..300: how strongly storeys differ in temperature

    public ServerSnowSettings() {
        for (BlockThermalCategory cat : BlockThermalCategory.VALUES) {
            this.blockConductivity[cat.ordinal()] = cat.defaultConductivity;
            this.blockCapacity[cat.ordinal()] = cat.defaultCapacity;
        }
    }

    public static ServerSnowSettings fromCommonDefaults() {
        return new ServerSnowSettings();
    }

    /**
     * Computes live precipitation speed based on current weather event & snowIntensity.
     * In CLEAR_FROST (or snowIntensity <= 0.03), precipitation is strictly 0.0!
     * In LIGHT_SNOW (0.25) -> minAccumulationSpeed; in BLIZZARD (1.00) -> maxAccumulationSpeed.
     */
    public float computeCurrentPrecipitationSpeed(WeatherEventType event, float snowIntensity) {
        if (!this.enabled) {
            return 0.0F;
        }
        if (event == WeatherEventType.CLEAR_FROST && snowIntensity < 0.08F) {
            return 0.0F;
        }
        if (snowIntensity <= 0.025F) {
            return 0.0F;
        }
        // v2.6.3: a 0 slider used to mean "no snow at all, forever", which reads like a bug.
        // A LIGHT_SNOW phase always lays at least a trickle; 0 now means "minimum slow snowfall".
        float lo = this.minAccumulationSpeed <= 0 ? MIN_PRECIP_FLOOR : this.minAccumulationSpeed;
        float hi = Math.max(lo, this.maxAccumulationSpeed);
        if (snowIntensity < 0.25F) {
            float onset = (snowIntensity - 0.025F) / 0.225F;
            return lo * Mth.clamp(onset, 0.0F, 1.0F);
        }
        float t = Mth.clamp((snowIntensity - 0.25F) / 0.75F, 0.0F, 1.0F);
        return Mth.lerp(t, lo, hi);
    }

    public ServerSnowSettings copy() {
        ServerSnowSettings c = new ServerSnowSettings();
        c.copyFrom(this);
        return c;
    }

    public void copyFrom(ServerSnowSettings src) {
        this.enabled = src.enabled;
        this.autoWeatherEvents = src.autoWeatherEvents;
        this.forcedEventOrdinal = src.forcedEventOrdinal;
        this.weatherChangeMinutes = src.weatherChangeMinutes;
        this.autoWind = src.autoWind;
        this.manualWindAngleDeg = src.manualWindAngleDeg;
        this.autoWindDrift = src.autoWindDrift;
        this.windDriftStrengthPercent = src.windDriftStrengthPercent;
        this.blizzardWindPush = src.blizzardWindPush;

        this.infiniteHeight = src.infiniteHeight;
        this.averagePillarHeight = src.averagePillarHeight;
        this.minAccumulationSpeed = src.minAccumulationSpeed;
        this.maxAccumulationSpeed = src.maxAccumulationSpeed;
        this.accumulationSpeed = src.accumulationSpeed;
        this.maxBlockUpdatesPerTick = src.maxBlockUpdatesPerTick;
        this.smoothDrifts = src.smoothDrifts;
        this.autoThawExcess = src.autoThawExcess;
        this.keepClearSkyLight = src.keepClearSkyLight;
        this.trampleChancePercent = src.trampleChancePercent;

        this.freezeWater = src.freezeWater;
        this.snowOnIce = src.snowOnIce;
        this.snowOnSlabs = src.snowOnSlabs;
        this.leavesMode = src.leavesMode;
        this.crushLeavesUnderWeight = src.crushLeavesUnderWeight;
        this.snowOnGlass = src.snowOnGlass;
        this.snowOnStairs = src.snowOnStairs;
        this.snowOnPathsAndFarmland = src.snowOnPathsAndFarmland;
        this.buryPlants = src.buryPlants;

        this.minWorldTempC = src.minWorldTempC;
        this.maxWorldTempC = src.maxWorldTempC;
        this.nightColdDropC = src.nightColdDropC;
        this.meltRoofSnowFromWarmBlocks = src.meltRoofSnowFromWarmBlocks;

        this.globalHeatBonusC = src.globalHeatBonusC;
        this.globalHeatingSpeedMultiplier = src.globalHeatingSpeedMultiplier;
        this.roomHeatingPowerPercent = src.roomHeatingPowerPercent;
        this.roomSourcePower = src.roomSourcePower;
        this.roomAirMixing = src.roomAirMixing;
        this.roomMixingSweeps = src.roomMixingSweeps;
        this.roomComfortCeilingC = src.roomComfortCeilingC;
        this.foodSpoilageSpeedPercent = src.foodSpoilageSpeedPercent;
        this.foodFreezingSpeedPercent = src.foodFreezingSpeedPercent;
        this.foodThawingSpeedPercent = src.foodThawingSpeedPercent;
        this.enableFoodFreezing = src.enableFoodFreezing;
        this.enableFoodSpoilage = src.enableFoodSpoilage;
        this.enableCookingTweaks = src.enableCookingTweaks;

        this.heatTorch = src.heatTorch;
        this.heatLantern = src.heatLantern;
        this.heatFurnace = src.heatFurnace;
        this.heatBlastSmoker = src.heatBlastSmoker;
        this.heatCampfire = src.heatCampfire;
        this.heatSoulCampfire = src.heatSoulCampfire;
        this.heatFireplace = src.heatFireplace;
        this.heatMagma = src.heatMagma;
        this.heatLava = src.heatLava;

        this.globalConductivityMultiplier = src.globalConductivityMultiplier;
        this.globalCapacityMultiplier = src.globalCapacityMultiplier;
        System.arraycopy(src.blockConductivity, 0, this.blockConductivity, 0, this.blockConductivity.length);
        System.arraycopy(src.blockCapacity, 0, this.blockCapacity, 0, this.blockCapacity.length);

        this.enablePlayerTemperature = src.enablePlayerTemperature;
        this.playerTempRateMultiplier = src.playerTempRateMultiplier;
        this.armorInsulationMultiplier = src.armorInsulationMultiplier;
        this.enableThirst = src.enableThirst;
        this.thirstDrainMultiplier = src.thirstDrainMultiplier;
        this.customDrinks = src.customDrinks != null ? src.customDrinks : "";
        this.shovelDurability = src.shovelDurability;
        this.golemSwingIntervalTicks = src.golemSwingIntervalTicks;
        this.golemMaxRadius = src.golemMaxRadius;

        this.mobsDoNotBurnInSnow = src.mobsDoNotBurnInSnow;
        this.enableMobFreezing = src.enableMobFreezing;
        this.mobFreezeRateMultiplier = src.mobFreezeRateMultiplier;
        this.enableConvection = src.enableConvection;
        this.convectionStrengthPercent = src.convectionStrengthPercent;
    }

    public void clamp() {
        this.foodThawingSpeedPercent = Mth.clamp(this.foodThawingSpeedPercent, 0, 400);
        this.foodFreezingSpeedPercent = Mth.clamp(this.foodFreezingSpeedPercent, 0, 400);
        this.foodSpoilageSpeedPercent = Mth.clamp(this.foodSpoilageSpeedPercent, 0, 400);
        this.forcedEventOrdinal = Mth.clamp(this.forcedEventOrdinal, 0, 4);
        this.weatherChangeMinutes = Mth.clamp(this.weatherChangeMinutes, 2, 60);
        this.manualWindAngleDeg = ((this.manualWindAngleDeg % 360) + 360) % 360;
        this.windDriftStrengthPercent = Mth.clamp(this.windDriftStrengthPercent, 0, 200);
        this.averagePillarHeight = Mth.clamp(this.averagePillarHeight, 0.1D, 15.0D);
        this.minAccumulationSpeed = Mth.clamp(this.minAccumulationSpeed, 0, 100);
        this.maxAccumulationSpeed = Mth.clamp(this.maxAccumulationSpeed, 0, 150);
        if (this.maxAccumulationSpeed < this.minAccumulationSpeed) {
            this.maxAccumulationSpeed = this.minAccumulationSpeed;
        }
        this.accumulationSpeed = Mth.clamp(this.accumulationSpeed, 1, 100);
        this.maxBlockUpdatesPerTick = Mth.clamp(this.maxBlockUpdatesPerTick, 10, 1200);
        this.trampleChancePercent = Mth.clamp(this.trampleChancePercent, 0, 100);
        this.leavesMode = Mth.clamp(this.leavesMode, 0, 2);

        this.minWorldTempC = Mth.clamp(this.minWorldTempC, -60, 10);
        this.maxWorldTempC = Mth.clamp(this.maxWorldTempC, -45, 20);
        if (this.maxWorldTempC < this.minWorldTempC) {
            this.maxWorldTempC = this.minWorldTempC;
        }
        this.nightColdDropC = Mth.clamp(this.nightColdDropC, 0, 25);

        this.globalHeatBonusC = Mth.clamp(this.globalHeatBonusC, -40.0F, 100.0F);
        this.globalHeatingSpeedMultiplier = Mth.clamp(this.globalHeatingSpeedMultiplier, 0.2F, 4.0F);
        this.roomHeatingPowerPercent = Mth.clamp(this.roomHeatingPowerPercent, 0, 300);
        this.roomSourcePower = Mth.clamp(this.roomSourcePower, 1.0F, 200.0F);
        this.roomAirMixing = Mth.clamp(this.roomAirMixing, 1.0F, 100.0F);
        this.roomMixingSweeps = Mth.clamp(this.roomMixingSweeps, 0, 64);
        this.roomComfortCeilingC = Mth.clamp(this.roomComfortCeilingC, -5.0F, 60.0F);
        this.globalConductivityMultiplier = Mth.clamp(this.globalConductivityMultiplier, 0.2F, 3.0F);
        this.globalCapacityMultiplier = Mth.clamp(this.globalCapacityMultiplier, 0.2F, 3.0F);

        this.heatTorch = Mth.clamp(this.heatTorch, 0.0F, 80.0F);
        this.heatLantern = Mth.clamp(this.heatLantern, 0.0F, 80.0F);
        this.heatFurnace = Mth.clamp(this.heatFurnace, 0.0F, 140.0F);
        this.heatBlastSmoker = Mth.clamp(this.heatBlastSmoker, 0.0F, 160.0F);
        this.heatCampfire = Mth.clamp(this.heatCampfire, 0.0F, 160.0F);
        this.heatSoulCampfire = Mth.clamp(this.heatSoulCampfire, 0.0F, 160.0F);
        this.heatFireplace = Mth.clamp(this.heatFireplace, 0.0F, 200.0F);
        this.heatMagma = Mth.clamp(this.heatMagma, 0.0F, 180.0F);
        this.heatLava = Mth.clamp(this.heatLava, 0.0F, 220.0F);
        this.mobFreezeRateMultiplier = Mth.clamp(this.mobFreezeRateMultiplier, 0.05F, 1.0F);
        this.convectionStrengthPercent = Mth.clamp(this.convectionStrengthPercent, 0, 300);

        for (int i = 0; i < this.blockConductivity.length; i++) {
            this.blockConductivity[i] = Mth.clamp(this.blockConductivity[i], 0.01F, 1.00F);
            this.blockCapacity[i] = Mth.clamp(this.blockCapacity[i], 0.2F, 15.0F);
        }

        this.playerTempRateMultiplier = Mth.clamp(this.playerTempRateMultiplier, 0.1F, 5.0F);
        this.armorInsulationMultiplier = Mth.clamp(this.armorInsulationMultiplier, 0.0F, 3.0F);
        this.thirstDrainMultiplier = Mth.clamp(this.thirstDrainMultiplier, 0.1F, 5.0F);
        if (this.customDrinks == null) {
            this.customDrinks = "";
        } else if (this.customDrinks.length() > 1024) {
            this.customDrinks = this.customDrinks.substring(0, 1024);
        }
        this.shovelDurability = Mth.clamp(this.shovelDurability, 64, 4096);
        this.golemSwingIntervalTicks = Mth.clamp(this.golemSwingIntervalTicks, 10, 200);
        this.golemMaxRadius = Mth.clamp(this.golemMaxRadius, 4, 48);
        // v2.7 legacy: the room ceiling no longer caps physics, it is only kept for old saves
        this.roomComfortCeilingC = Mth.clamp(this.roomComfortCeilingC, -5.0F, 60.0F);
    }

    public void saveToTag(CompoundTag tag) {
        tag.putBoolean("enabled", this.enabled);
        tag.putBoolean("autoWeatherEvents", this.autoWeatherEvents);
        tag.putInt("forcedEventOrdinal", this.forcedEventOrdinal);
        tag.putInt("weatherChangeMinutes", this.weatherChangeMinutes);
        tag.putBoolean("autoWind", this.autoWind);
        tag.putInt("manualWindAngleDeg", this.manualWindAngleDeg);
        tag.putBoolean("autoWindDrift", this.autoWindDrift);
        tag.putInt("windDriftStrengthPercent", this.windDriftStrengthPercent);
        tag.putBoolean("blizzardWindPush", this.blizzardWindPush);

        tag.putBoolean("infiniteHeight", this.infiniteHeight);
        tag.putDouble("averagePillarHeight", this.averagePillarHeight);
        tag.putInt("minAccumulationSpeed", this.minAccumulationSpeed);
        tag.putInt("maxAccumulationSpeed", this.maxAccumulationSpeed);
        tag.putInt("accumulationSpeed", this.accumulationSpeed);
        tag.putInt("maxBlockUpdatesPerTick", this.maxBlockUpdatesPerTick);
        tag.putBoolean("smoothDrifts", this.smoothDrifts);
        tag.putBoolean("autoThawExcess", this.autoThawExcess);
        tag.putBoolean("keepClearSkyLight", this.keepClearSkyLight);
        tag.putInt("trampleChancePercent", this.trampleChancePercent);

        tag.putBoolean("freezeWater", this.freezeWater);
        tag.putBoolean("snowOnIce", this.snowOnIce);
        tag.putBoolean("snowOnSlabs", this.snowOnSlabs);
        tag.putInt("leavesMode", this.leavesMode);
        tag.putBoolean("crushLeavesUnderWeight", this.crushLeavesUnderWeight);
        tag.putBoolean("snowOnGlass", this.snowOnGlass);
        tag.putBoolean("snowOnStairs", this.snowOnStairs);
        tag.putBoolean("snowOnPathsAndFarmland", this.snowOnPathsAndFarmland);
        tag.putBoolean("buryPlants", this.buryPlants);

        tag.putInt("minWorldTempC", this.minWorldTempC);
        tag.putInt("maxWorldTempC", this.maxWorldTempC);
        tag.putInt("nightColdDropC", this.nightColdDropC);
        tag.putBoolean("meltRoofSnowFromWarmBlocks", this.meltRoofSnowFromWarmBlocks);

        tag.putFloat("globalHeatBonusC", this.globalHeatBonusC);
        tag.putFloat("globalHeatingSpeedMultiplier", this.globalHeatingSpeedMultiplier);
        tag.putInt("roomHeatingPowerPercent", this.roomHeatingPowerPercent);
        tag.putFloat("roomSourcePower", this.roomSourcePower);
        tag.putFloat("roomAirMixing", this.roomAirMixing);
        tag.putInt("roomMixingSweeps", this.roomMixingSweeps);
        tag.putFloat("roomComfortCeilingC", this.roomComfortCeilingC);
        tag.putInt("foodSpoilageSpeedPercent", this.foodSpoilageSpeedPercent);
        tag.putInt("foodFreezingSpeedPercent", this.foodFreezingSpeedPercent);
        tag.putInt("foodThawingSpeedPercent", this.foodThawingSpeedPercent);
        tag.putBoolean("enableFoodFreezing", this.enableFoodFreezing);
        tag.putBoolean("enableFoodSpoilage", this.enableFoodSpoilage);
        tag.putBoolean("enableCookingTweaks", this.enableCookingTweaks);
        tag.putFloat("globalConductivityMultiplier", this.globalConductivityMultiplier);
        tag.putFloat("globalCapacityMultiplier", this.globalCapacityMultiplier);

        tag.putFloat("heatTorch", this.heatTorch);
        tag.putFloat("heatLantern", this.heatLantern);
        tag.putFloat("heatFurnace", this.heatFurnace);
        tag.putFloat("heatBlastSmoker", this.heatBlastSmoker);
        tag.putFloat("heatCampfire", this.heatCampfire);
        tag.putFloat("heatSoulCampfire", this.heatSoulCampfire);
        tag.putFloat("heatFireplace", this.heatFireplace);
        tag.putFloat("heatMagma", this.heatMagma);
        tag.putFloat("heatLava", this.heatLava);

        CompoundTag condTag = new CompoundTag();
        CompoundTag capTag = new CompoundTag();
        for (BlockThermalCategory cat : BlockThermalCategory.VALUES) {
            condTag.putFloat(cat.id, this.blockConductivity[cat.ordinal()]);
            capTag.putFloat(cat.id, this.blockCapacity[cat.ordinal()]);
        }
        tag.put("blockConductivity", condTag);
        tag.put("blockCapacity", capTag);

        tag.putBoolean("enablePlayerTemperature", this.enablePlayerTemperature);
        tag.putFloat("playerTempRateMultiplier", this.playerTempRateMultiplier);
        tag.putFloat("armorInsulationMultiplier", this.armorInsulationMultiplier);
        tag.putBoolean("enableThirst", this.enableThirst);
        tag.putFloat("thirstDrainMultiplier", this.thirstDrainMultiplier);
        tag.putString("customDrinks", this.customDrinks != null ? this.customDrinks : "");
        tag.putInt("shovelDurability", this.shovelDurability);
        tag.putInt("golemSwingIntervalTicks", this.golemSwingIntervalTicks);
        tag.putInt("golemMaxRadius", this.golemMaxRadius);

        // v2.6.6
        tag.putBoolean("mobsDoNotBurnInSnow", this.mobsDoNotBurnInSnow);
        tag.putBoolean("enableMobFreezing", this.enableMobFreezing);
        tag.putFloat("mobFreezeRateMultiplier", this.mobFreezeRateMultiplier);
        tag.putBoolean("enableConvection", this.enableConvection);
        tag.putInt("convectionStrengthPercent", this.convectionStrengthPercent);
    }

    public void loadFromTag(CompoundTag tag) {
        if (tag.contains("enabled")) this.enabled = tag.getBoolean("enabled");
        if (tag.contains("autoWeatherEvents")) this.autoWeatherEvents = tag.getBoolean("autoWeatherEvents");
        if (tag.contains("forcedEventOrdinal")) this.forcedEventOrdinal = tag.getInt("forcedEventOrdinal");
        if (tag.contains("weatherChangeMinutes")) this.weatherChangeMinutes = tag.getInt("weatherChangeMinutes");
        if (tag.contains("autoWind")) this.autoWind = tag.getBoolean("autoWind");
        if (tag.contains("manualWindAngleDeg")) this.manualWindAngleDeg = tag.getInt("manualWindAngleDeg");
        if (tag.contains("autoWindDrift")) this.autoWindDrift = tag.getBoolean("autoWindDrift");
        if (tag.contains("windDriftStrengthPercent")) this.windDriftStrengthPercent = tag.getInt("windDriftStrengthPercent");
        if (tag.contains("blizzardWindPush")) this.blizzardWindPush = tag.getBoolean("blizzardWindPush");

        if (tag.contains("infiniteHeight")) this.infiniteHeight = tag.getBoolean("infiniteHeight");
        if (tag.contains("averagePillarHeight")) this.averagePillarHeight = tag.getDouble("averagePillarHeight");
        if (tag.contains("accumulationSpeed")) {
            this.accumulationSpeed = tag.getInt("accumulationSpeed");
            // Smooth migration from pre-v2.2 saves if min/max weren't saved yet
            if (!tag.contains("minAccumulationSpeed") && !tag.contains("maxAccumulationSpeed")) {
                this.minAccumulationSpeed = Math.max(1, this.accumulationSpeed / 3);
                this.maxAccumulationSpeed = Math.min(150, this.accumulationSpeed * 3);
            }
        }
        if (tag.contains("minAccumulationSpeed")) this.minAccumulationSpeed = tag.getInt("minAccumulationSpeed");
        if (tag.contains("maxAccumulationSpeed")) this.maxAccumulationSpeed = tag.getInt("maxAccumulationSpeed");
        if (tag.contains("maxBlockUpdatesPerTick")) this.maxBlockUpdatesPerTick = tag.getInt("maxBlockUpdatesPerTick");
        if (tag.contains("smoothDrifts")) this.smoothDrifts = tag.getBoolean("smoothDrifts");
        if (tag.contains("autoThawExcess")) this.autoThawExcess = tag.getBoolean("autoThawExcess");
        if (tag.contains("keepClearSkyLight")) this.keepClearSkyLight = tag.getBoolean("keepClearSkyLight");
        if (tag.contains("trampleChancePercent")) this.trampleChancePercent = tag.getInt("trampleChancePercent");

        if (tag.contains("freezeWaterAndSnowOnIce")) {
            boolean legacy = tag.getBoolean("freezeWaterAndSnowOnIce");
            this.freezeWater = legacy;
            this.snowOnIce = legacy;
        }
        if (tag.contains("freezeWater")) this.freezeWater = tag.getBoolean("freezeWater");
        if (tag.contains("snowOnIce")) this.snowOnIce = tag.getBoolean("snowOnIce");
        if (tag.contains("snowOnSlabs")) this.snowOnSlabs = tag.getBoolean("snowOnSlabs");
        if (tag.contains("leavesMode")) this.leavesMode = tag.getInt("leavesMode");
        if (tag.contains("crushLeavesUnderWeight")) this.crushLeavesUnderWeight = tag.getBoolean("crushLeavesUnderWeight");
        if (tag.contains("snowOnGlass")) this.snowOnGlass = tag.getBoolean("snowOnGlass");
        if (tag.contains("snowOnStairs")) this.snowOnStairs = tag.getBoolean("snowOnStairs");
        if (tag.contains("snowOnPathsAndFarmland")) this.snowOnPathsAndFarmland = tag.getBoolean("snowOnPathsAndFarmland");
        if (tag.contains("buryPlants")) this.buryPlants = tag.getBoolean("buryPlants");

        if (tag.contains("minWorldTempC")) this.minWorldTempC = tag.getInt("minWorldTempC");
        if (tag.contains("maxWorldTempC")) this.maxWorldTempC = tag.getInt("maxWorldTempC");
        if (tag.contains("nightColdDropC")) this.nightColdDropC = tag.getInt("nightColdDropC");
        if (tag.contains("meltRoofSnowFromWarmBlocks")) this.meltRoofSnowFromWarmBlocks = tag.getBoolean("meltRoofSnowFromWarmBlocks");

        if (tag.contains("globalHeatBonusC")) this.globalHeatBonusC = tag.getFloat("globalHeatBonusC");
        if (tag.contains("globalHeatingSpeedMultiplier")) this.globalHeatingSpeedMultiplier = tag.getFloat("globalHeatingSpeedMultiplier");
        if (tag.contains("roomHeatingPowerPercent")) this.roomHeatingPowerPercent = tag.getInt("roomHeatingPowerPercent");
        if (tag.contains("roomSourcePower")) this.roomSourcePower = tag.getFloat("roomSourcePower");
        if (tag.contains("roomAirMixing")) this.roomAirMixing = tag.getFloat("roomAirMixing");
        if (tag.contains("roomMixingSweeps")) this.roomMixingSweeps = tag.getInt("roomMixingSweeps");
        if (tag.contains("roomComfortCeilingC")) this.roomComfortCeilingC = tag.getFloat("roomComfortCeilingC");
        if (tag.contains("foodSpoilageSpeedPercent")) this.foodSpoilageSpeedPercent = tag.getInt("foodSpoilageSpeedPercent");
        if (tag.contains("foodFreezingSpeedPercent")) this.foodFreezingSpeedPercent = tag.getInt("foodFreezingSpeedPercent");
        if (tag.contains("foodThawingSpeedPercent")) this.foodThawingSpeedPercent = tag.getInt("foodThawingSpeedPercent");
        if (tag.contains("enableFoodFreezing")) this.enableFoodFreezing = tag.getBoolean("enableFoodFreezing");
        if (tag.contains("enableFoodSpoilage")) this.enableFoodSpoilage = tag.getBoolean("enableFoodSpoilage");
        if (tag.contains("enableCookingTweaks")) this.enableCookingTweaks = tag.getBoolean("enableCookingTweaks");
        if (tag.contains("globalConductivityMultiplier")) this.globalConductivityMultiplier = tag.getFloat("globalConductivityMultiplier");
        if (tag.contains("globalCapacityMultiplier")) this.globalCapacityMultiplier = tag.getFloat("globalCapacityMultiplier");

        if (tag.contains("heatTorch")) this.heatTorch = tag.getFloat("heatTorch");
        if (tag.contains("heatLantern")) this.heatLantern = tag.getFloat("heatLantern");
        if (tag.contains("heatFurnace")) this.heatFurnace = tag.getFloat("heatFurnace");
        if (tag.contains("heatBlastSmoker")) this.heatBlastSmoker = tag.getFloat("heatBlastSmoker");
        if (tag.contains("heatCampfire")) this.heatCampfire = tag.getFloat("heatCampfire");
        if (tag.contains("heatSoulCampfire")) this.heatSoulCampfire = tag.getFloat("heatSoulCampfire");
        if (tag.contains("heatFireplace")) this.heatFireplace = tag.getFloat("heatFireplace");
        if (tag.contains("heatMagma")) this.heatMagma = tag.getFloat("heatMagma");
        if (tag.contains("heatLava")) this.heatLava = tag.getFloat("heatLava");

        if (tag.contains("blockConductivity")) {
            CompoundTag condTag = tag.getCompound("blockConductivity");
            for (BlockThermalCategory cat : BlockThermalCategory.VALUES) {
                if (condTag.contains(cat.id)) {
                    this.blockConductivity[cat.ordinal()] = condTag.getFloat(cat.id);
                }
            }
        }
        if (tag.contains("blockCapacity")) {
            CompoundTag capTag = tag.getCompound("blockCapacity");
            for (BlockThermalCategory cat : BlockThermalCategory.VALUES) {
                if (capTag.contains(cat.id)) {
                    this.blockCapacity[cat.ordinal()] = capTag.getFloat(cat.id);
                }
            }
        }

        if (tag.contains("enablePlayerTemperature")) this.enablePlayerTemperature = tag.getBoolean("enablePlayerTemperature");
        if (tag.contains("playerTempRateMultiplier")) this.playerTempRateMultiplier = tag.getFloat("playerTempRateMultiplier");
        if (tag.contains("armorInsulationMultiplier")) this.armorInsulationMultiplier = tag.getFloat("armorInsulationMultiplier");
        if (tag.contains("enableThirst")) this.enableThirst = tag.getBoolean("enableThirst");
        if (tag.contains("thirstDrainMultiplier")) this.thirstDrainMultiplier = tag.getFloat("thirstDrainMultiplier");
        if (tag.contains("customDrinks")) this.customDrinks = tag.getString("customDrinks");
        if (tag.contains("shovelDurability")) this.shovelDurability = tag.getInt("shovelDurability");
        if (tag.contains("golemSwingIntervalTicks")) this.golemSwingIntervalTicks = tag.getInt("golemSwingIntervalTicks");
        if (tag.contains("golemMaxRadius")) this.golemMaxRadius = tag.getInt("golemMaxRadius");
        // v2.6.6
        if (tag.contains("mobsDoNotBurnInSnow")) this.mobsDoNotBurnInSnow = tag.getBoolean("mobsDoNotBurnInSnow");
        if (tag.contains("enableMobFreezing")) this.enableMobFreezing = tag.getBoolean("enableMobFreezing");
        if (tag.contains("mobFreezeRateMultiplier")) this.mobFreezeRateMultiplier = tag.getFloat("mobFreezeRateMultiplier");
        if (tag.contains("enableConvection")) this.enableConvection = tag.getBoolean("enableConvection");
        if (tag.contains("convectionStrengthPercent")) this.convectionStrengthPercent = tag.getInt("convectionStrengthPercent");
        this.clamp();
    }

    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeBoolean(this.enabled);
        buf.writeBoolean(this.autoWeatherEvents);
        buf.writeVarInt(this.forcedEventOrdinal);
        buf.writeVarInt(this.weatherChangeMinutes);
        buf.writeBoolean(this.autoWind);
        buf.writeVarInt(this.manualWindAngleDeg);
        buf.writeBoolean(this.autoWindDrift);
        buf.writeVarInt(this.windDriftStrengthPercent);
        buf.writeBoolean(this.blizzardWindPush);

        buf.writeBoolean(this.infiniteHeight);
        buf.writeDouble(this.averagePillarHeight);
        buf.writeVarInt(this.minAccumulationSpeed);
        buf.writeVarInt(this.maxAccumulationSpeed);
        buf.writeVarInt(this.accumulationSpeed);
        buf.writeVarInt(this.maxBlockUpdatesPerTick);
        buf.writeBoolean(this.smoothDrifts);
        buf.writeBoolean(this.autoThawExcess);
        buf.writeBoolean(this.keepClearSkyLight);
        buf.writeVarInt(this.trampleChancePercent);

        buf.writeBoolean(this.freezeWater);
        buf.writeBoolean(this.snowOnIce);
        buf.writeBoolean(this.snowOnSlabs);
        buf.writeVarInt(this.leavesMode);
        buf.writeBoolean(this.crushLeavesUnderWeight);
        buf.writeBoolean(this.snowOnGlass);
        buf.writeBoolean(this.snowOnStairs);
        buf.writeBoolean(this.snowOnPathsAndFarmland);
        buf.writeBoolean(this.buryPlants);

        buf.writeVarInt(this.minWorldTempC);
        buf.writeVarInt(this.maxWorldTempC);
        buf.writeVarInt(this.nightColdDropC);
        buf.writeBoolean(this.meltRoofSnowFromWarmBlocks);
        buf.writeFloat(this.roomSourcePower);
        buf.writeFloat(this.roomAirMixing);
        buf.writeInt(this.roomMixingSweeps);

        buf.writeFloat(this.globalHeatBonusC);
        buf.writeFloat(this.globalHeatingSpeedMultiplier);

        buf.writeFloat(this.heatTorch);
        buf.writeFloat(this.heatLantern);
        buf.writeFloat(this.heatFurnace);
        buf.writeFloat(this.heatBlastSmoker);
        buf.writeFloat(this.heatCampfire);
        buf.writeFloat(this.heatSoulCampfire);
        buf.writeFloat(this.heatFireplace);
        buf.writeFloat(this.heatMagma);
        buf.writeFloat(this.heatLava);

        for (int i = 0; i < this.blockConductivity.length; i++) {
            buf.writeFloat(this.blockConductivity[i]);
            buf.writeFloat(this.blockCapacity[i]);
        }

        buf.writeBoolean(this.enablePlayerTemperature);
        buf.writeFloat(this.playerTempRateMultiplier);
        buf.writeFloat(this.armorInsulationMultiplier);
        buf.writeBoolean(this.enableThirst);
        buf.writeFloat(this.thirstDrainMultiplier);
        buf.writeUtf(this.customDrinks != null ? this.customDrinks : "", 1024);
        buf.writeVarInt(this.shovelDurability);
        buf.writeVarInt(this.golemSwingIntervalTicks);
        buf.writeVarInt(this.golemMaxRadius);

        // v2.5.2 appended at the END to keep the field order of older protocol versions intact
        buf.writeVarInt(this.roomHeatingPowerPercent);
        buf.writeFloat(this.roomComfortCeilingC);
        // v2.5.3 appended at the END (older protocol versions simply have no trailing bytes)
        buf.writeFloat(this.globalConductivityMultiplier);
        buf.writeFloat(this.globalCapacityMultiplier);
        // v2.6.6 appended at the END (protocol 17)
        buf.writeBoolean(this.mobsDoNotBurnInSnow);
        buf.writeBoolean(this.enableMobFreezing);
        buf.writeFloat(this.mobFreezeRateMultiplier);
        buf.writeBoolean(this.enableConvection);
        buf.writeVarInt(this.convectionStrengthPercent);
        // v2.7 appended at the END (protocol 18)
        buf.writeBoolean(this.enableFoodFreezing);
        buf.writeBoolean(this.enableFoodSpoilage);
        buf.writeBoolean(this.enableCookingTweaks);
        buf.writeVarInt(this.foodSpoilageSpeedPercent);
        buf.writeVarInt(this.foodFreezingSpeedPercent);
        buf.writeVarInt(this.foodThawingSpeedPercent);

    }

    public void readFromBuf(FriendlyByteBuf buf) {
        this.enabled = buf.readBoolean();
        this.autoWeatherEvents = buf.readBoolean();
        this.forcedEventOrdinal = buf.readVarInt();
        this.weatherChangeMinutes = buf.readVarInt();
        this.autoWind = buf.readBoolean();
        this.manualWindAngleDeg = buf.readVarInt();
        this.autoWindDrift = buf.readBoolean();
        this.windDriftStrengthPercent = buf.readVarInt();
        this.blizzardWindPush = buf.readBoolean();

        this.infiniteHeight = buf.readBoolean();
        this.averagePillarHeight = buf.readDouble();
        this.minAccumulationSpeed = buf.readVarInt();
        this.maxAccumulationSpeed = buf.readVarInt();
        this.accumulationSpeed = buf.readVarInt();
        this.maxBlockUpdatesPerTick = buf.readVarInt();
        this.smoothDrifts = buf.readBoolean();
        this.autoThawExcess = buf.readBoolean();
        this.keepClearSkyLight = buf.readBoolean();
        this.trampleChancePercent = buf.readVarInt();

        this.freezeWater = buf.readBoolean();
        this.snowOnIce = buf.readBoolean();
        this.snowOnSlabs = buf.readBoolean();
        this.leavesMode = buf.readVarInt();
        this.crushLeavesUnderWeight = buf.readBoolean();
        this.snowOnGlass = buf.readBoolean();
        this.snowOnStairs = buf.readBoolean();
        this.snowOnPathsAndFarmland = buf.readBoolean();
        this.buryPlants = buf.readBoolean();

        this.minWorldTempC = buf.readVarInt();
        this.maxWorldTempC = buf.readVarInt();
        this.nightColdDropC = buf.readVarInt();
        this.meltRoofSnowFromWarmBlocks = buf.readBoolean();
        this.roomSourcePower = buf.readFloat();
        this.roomAirMixing = buf.readFloat();
        this.roomMixingSweeps = buf.readInt();

        this.globalHeatBonusC = buf.readFloat();
        this.globalHeatingSpeedMultiplier = buf.readFloat();

        this.heatTorch = buf.readFloat();
        this.heatLantern = buf.readFloat();
        this.heatFurnace = buf.readFloat();
        this.heatBlastSmoker = buf.readFloat();
        this.heatCampfire = buf.readFloat();
        this.heatSoulCampfire = buf.readFloat();
        this.heatFireplace = buf.readFloat();
        this.heatMagma = buf.readFloat();
        this.heatLava = buf.readFloat();

        for (int i = 0; i < this.blockConductivity.length; i++) {
            this.blockConductivity[i] = buf.readFloat();
            this.blockCapacity[i] = buf.readFloat();
        }

        this.enablePlayerTemperature = buf.readBoolean();
        this.playerTempRateMultiplier = buf.readFloat();
        this.armorInsulationMultiplier = buf.readFloat();
        this.enableThirst = buf.readBoolean();
        this.thirstDrainMultiplier = buf.readFloat();
        this.customDrinks = buf.readUtf(1024);
        this.shovelDurability = buf.readVarInt();
        this.golemSwingIntervalTicks = buf.readVarInt();
        this.golemMaxRadius = buf.readVarInt();
        // v2.5.2 appended fields (safe defaults if an older packet has no trailing bytes)
        this.roomHeatingPowerPercent = buf.isReadable() ? buf.readVarInt() : 100;
        this.roomComfortCeilingC = buf.isReadable() ? buf.readFloat() : 26.0F;
        this.globalConductivityMultiplier = buf.isReadable() ? buf.readFloat() : 1.0F;
        this.globalCapacityMultiplier = buf.isReadable() ? buf.readFloat() : 1.0F;
        // v2.6.6 trailing fields
        this.mobsDoNotBurnInSnow = !buf.isReadable() || buf.readBoolean();
        this.enableMobFreezing = !buf.isReadable() || buf.readBoolean();
        this.mobFreezeRateMultiplier = buf.isReadable() ? buf.readFloat() : 0.20F;
        this.enableConvection = !buf.isReadable() || buf.readBoolean();
        this.convectionStrengthPercent = buf.isReadable() ? buf.readVarInt() : 100;
        // v2.7 trailing fields
        this.enableFoodFreezing = !buf.isReadable() || buf.readBoolean();
        this.enableFoodSpoilage = !buf.isReadable() || buf.readBoolean();
        this.enableCookingTweaks = buf.isReadable() ? buf.readBoolean() : true;
        this.foodSpoilageSpeedPercent = buf.isReadable() ? buf.readVarInt() : 25;
        this.foodFreezingSpeedPercent = buf.isReadable() ? buf.readVarInt() : 100;
        this.foodThawingSpeedPercent = buf.isReadable() ? buf.readVarInt() : 100;

        this.clamp();
    }
}
