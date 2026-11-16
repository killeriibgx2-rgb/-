package com.eternalsnow.weather;

public enum WeatherEventType {
    CLEAR_FROST("clear_frost", 0.00F, 0.08F, -6.0F),
    LIGHT_SNOW("light_snow", 0.25F, 0.22F, 0.0F),
    HEAVY_SNOW("heavy_snow", 0.82F, 0.16F, -3.5F),
    GALE("gale", 0.60F, 0.74F, -9.0F),
    BLIZZARD("blizzard", 1.00F, 1.00F, -16.0F);

    public final String id;
    public final float targetSnowIntensity;
    public final float targetWindSpeed;
    public final float tempModifierC;

    WeatherEventType(String id, float targetSnowIntensity, float targetWindSpeed, float tempModifierC) {
        this.id = id;
        this.targetSnowIntensity = targetSnowIntensity;
        this.targetWindSpeed = targetWindSpeed;
        this.tempModifierC = tempModifierC;
    }

    public int getId() {
        return this.ordinal();
    }

    public String getTranslationKey() {
        return "eternalsnow.weather." + this.id;
    }

    public String getShortButtonKey() {
        return "eternalsnow.weather.btn." + this.id;
    }

    public static WeatherEventType byOrdinal(int ord) {
        WeatherEventType[] vals = values();
        if (ord < 0 || ord >= vals.length) {
            return LIGHT_SNOW;
        }
        return vals[ord];
    }

    public static WeatherEventType byId(int ord) {
        return byOrdinal(ord);
    }

    public static WeatherEventType byId(String id) {
        for (WeatherEventType t : values()) {
            if (t.id.equalsIgnoreCase(id)) {
                return t;
            }
        }
        return LIGHT_SNOW;
    }
}
