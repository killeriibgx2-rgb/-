package com.eternalsnow.config;

/** One-shot settings, never a difficulty lock or a change to Minecraft's hardcore flag. */
public enum WinterPreset {
    EASY(-20,-2,4,.6F,.10F,.65F,10,60,8,1.2F),
    MEDIUM(-34,-6,8,1F,.20F,1F,25,100,18,1F),
    HARD(-44,-12,10,1.3F,.30F,1.25F,40,125,26,.9F),
    HARDCORE(-54,-18,12,1.6F,.40F,1.5F,60,150,36,.8F);
    private final int min,max,night,spoil,freeze,snow;
    private final float cold,mob,thirst,heat;
    WinterPreset(int min,int max,int night,float cold,float mob,float thirst,int spoil,int freeze,int snow,float heat) {
        this.min=min;this.max=max;this.night=night;this.cold=cold;this.mob=mob;
        this.thirst=thirst;this.spoil=spoil;this.freeze=freeze;this.snow=snow;this.heat=heat;
    }
    public String key() { return "eternalsnow.preset."+name().toLowerCase(java.util.Locale.ROOT); }
    public void apply(ServerSnowSettings target) {
        // Preserve operator integration/performance settings, reset gameplay fields explicitly.
        ServerSnowSettings defaults=new ServerSnowSettings();
        target.enabled=true; target.autoWeatherEvents=true; target.forcedEventOrdinal=1;
        target.weatherChangeMinutes=5; target.autoWind=true;target.autoWindDrift=true;
        target.blizzardWindPush=this!=EASY;target.windDriftStrengthPercent=this==EASY?60:100;
        target.minWorldTempC=min;target.maxWorldTempC=max;target.nightColdDropC=night;
        target.minAccumulationSpeed=2;target.maxAccumulationSpeed=snow;target.accumulationSpeed=6;
        target.enablePlayerTemperature=true;target.playerTempRateMultiplier=cold;
        target.armorInsulationMultiplier=this==EASY?1.2F:1F;
        target.enableThirst=true;target.thirstDrainMultiplier=thirst;
        target.enableMobFreezing=true;target.mobFreezeRateMultiplier=mob;target.mobsDoNotBurnInSnow=true;
        target.enableFoodFreezing=true;target.enableFoodSpoilage=true;
        target.foodSpoilageSpeedPercent=spoil;target.foodFreezingSpeedPercent=freeze;target.foodThawingSpeedPercent=100;
        target.globalHeatBonusC=0;target.globalHeatingSpeedMultiplier=1;target.roomHeatingPowerPercent=100;
        target.globalConductivityMultiplier=1;target.globalCapacityMultiplier=1;
        System.arraycopy(defaults.blockConductivity,0,target.blockConductivity,0,defaults.blockConductivity.length);
        System.arraycopy(defaults.blockCapacity,0,target.blockCapacity,0,defaults.blockCapacity.length);
        target.heatTorch=7*heat;target.heatLantern=20*heat;target.heatFurnace=48*heat;
        target.heatBlastSmoker=62*heat;target.heatCampfire=68*heat;target.heatSoulCampfire=45*heat;
        target.heatFireplace=94*heat;target.heatMagma=75*heat;target.heatLava=115*heat;
        target.enableConvection=true;target.convectionStrengthPercent=100;
        target.freezeWater=true;target.meltRoofSnowFromWarmBlocks=true;
    }
}
