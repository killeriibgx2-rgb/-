package com.eternalsnow.weather;

import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.thermal.ThermalEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class WeatherEventManager {

    private static WeatherEventType currentEvent = WeatherEventType.LIGHT_SNOW;
    private static int eventTimerTicks = 20 * 300;
    private static float targetWindAngleRad = 0.785F;
    private static int windShiftTimerTicks = 20 * 180;

    private static float smoothWindAngleRad = 0.785F;
    private static float smoothWindSpeed = 0.24F;
    private static float smoothSnowIntensity = 0.42F;

    private WeatherEventManager() {
    }

    public static void forceEvent(WeatherEventType type, ServerSnowSettings settings) {
        currentEvent = type;
        settings.forcedEventOrdinal = type.ordinal();
        eventTimerTicks = computeOrganicPhaseDurationTicks(type, RandomSource.create());
        EternalSnowState.activeWeatherEventOrdinal = type.ordinal();
        EternalSnowState.weatherEventId = type.ordinal();
        EternalSnowState.eventTicksRemaining = eventTimerTicks;
    }

    public static void forceEvent(net.minecraft.server.MinecraftServer server, WeatherEventType type, int durationTicks) {
        currentEvent = type;
        EternalSnowState.getServerSettings().forcedEventOrdinal = type.ordinal();
        eventTimerTicks = durationTicks;
        EternalSnowState.activeWeatherEventOrdinal = type.ordinal();
        EternalSnowState.weatherEventId = type.ordinal();
        EternalSnowState.eventTicksRemaining = durationTicks;
    }

    /**
     * Smart automatic weather duration per phase (replaces manual minute slider):
     * Calm frost & light snow last comfortably longer; intense Blizzards feel dramatic without dragging on forever!
     */
    private static int computeOrganicPhaseDurationTicks(WeatherEventType type, RandomSource random) {
        return switch (type) {
            case CLEAR_FROST -> 20 * (300 + random.nextInt(180)); // 5.0 .. 8.0 min
            case LIGHT_SNOW -> 20 * (320 + random.nextInt(200));  // 5.3 .. 8.6 min
            case HEAVY_SNOW -> 20 * (240 + random.nextInt(150));  // 4.0 .. 6.5 min
            case GALE -> 20 * (190 + random.nextInt(120));        // 3.1 .. 5.1 min
            case BLIZZARD -> 20 * (150 + random.nextInt(95));     // 2.5 .. 4.1 min
        };
    }

    public static void tickServerWeather(net.minecraft.server.MinecraftServer server) {
        tickServerWeather(server.overworld());
    }

    public static void tickServerWeather(ServerLevel overworld) {
        ServerSnowSettings settings = EternalSnowState.getServerSettings();
        if (!settings.enabled) {
            smoothSnowIntensity = Mth.lerp(0.05F, smoothSnowIntensity, 0.0F);
            smoothWindSpeed = Mth.lerp(0.05F, smoothWindSpeed, 0.0F);
            EternalSnowState.snowIntensity = smoothSnowIntensity;
            EternalSnowState.windSpeed = smoothWindSpeed;
            EternalSnowState.worldAmbientTempC = 15.0F;
            return;
        }

        RandomSource random = overworld.random;
        long gameTime = overworld.getGameTime();

        // 1. Automatic organic weather phase transitions
        if (settings.autoWeatherEvents) {
            eventTimerTicks--;
            if (eventTimerTicks <= 0) {
                currentEvent = pickNextWeatherEvent(random, currentEvent);
                eventTimerTicks = computeOrganicPhaseDurationTicks(currentEvent, random);
            }
        } else {
            currentEvent = WeatherEventType.byOrdinal(settings.forcedEventOrdinal);
        }
        EternalSnowState.activeWeatherEventOrdinal = currentEvent.ordinal();
        EternalSnowState.weatherEventId = currentEvent.ordinal();
        EternalSnowState.eventTicksRemaining = eventTimerTicks;

        // 2. Wind Angle rotation (360 degrees)
        if (settings.autoWind) {
            windShiftTimerTicks--;
            if (windShiftTimerTicks <= 0) {
                float delta = (random.nextFloat() - 0.5F) * 2.2F;
                targetWindAngleRad = wrapAngleRad(targetWindAngleRad + delta);
                windShiftTimerTicks = 20 * (120 + random.nextInt(180));
            }
            float angleDiff = wrapAngleDiff(targetWindAngleRad - smoothWindAngleRad);
            smoothWindAngleRad = wrapAngleRad(smoothWindAngleRad + angleDiff * 0.008F);
        } else {
            smoothWindAngleRad = settings.manualWindAngleDeg * ((float) Math.PI / 180.0F);
        }

        // 3. Wind Speed & Snow Intensity with natural gusts
        float gust = (Mth.sin(gameTime * 0.045F) * 0.5F + Mth.cos(gameTime * 0.11F) * 0.5F)
                * (currentEvent.targetWindSpeed * 0.18F);
        float desiredWind = Mth.clamp(currentEvent.targetWindSpeed + gust, 0.02F, 1.0F);
        float desiredSnow = currentEvent.targetSnowIntensity;

        smoothWindSpeed = Mth.lerp(0.02F, smoothWindSpeed, desiredWind);
        smoothSnowIntensity = Mth.lerp(0.02F, smoothSnowIntensity, desiredSnow);

        EternalSnowState.windAngleRad = smoothWindAngleRad;
        EternalSnowState.windYawDeg = smoothWindAngleRad * (180.0F / (float) Math.PI);
        EternalSnowState.windSpeed = smoothWindSpeed;
        EternalSnowState.snowIntensity = smoothSnowIntensity;

        // 4. Automatic Temperature Curve inside [minWorldTempC .. maxWorldTempC] (wave + day/night + weather chill!)
        double multiDayWave = 0.5D + 0.5D * Math.sin(gameTime / 24000.0D * Math.PI * 0.65D
                + 0.4D * Math.cos(gameTime / 9000.0D)); // [0..1]
        float sunAngle = overworld.getTimeOfDay(1.0F) * ((float) Math.PI * 2.0F);
        float dayWarmthFactor = Mth.clamp(0.5F + 0.5F * Mth.cos(sunAngle), 0.0F, 1.0F); // 1 at noon, 0 at midnight
        float weatherWarmthFactor = switch (currentEvent) {
            case LIGHT_SNOW -> 0.85F;
            case HEAVY_SNOW -> 0.62F;
            case CLEAR_FROST -> 0.42F; // Crisp clear frost is cold especially at night!
            case GALE -> 0.28F;
            case BLIZZARD -> 0.05F;
        };

        float combinedFactor = Mth.clamp(
                (float) (multiDayWave * 0.48D) + dayWarmthFactor * 0.34F + weatherWarmthFactor * 0.18F,
                0.0F, 1.0F
        );
        EternalSnowState.worldAmbientTempC = Mth.lerp(combinedFactor, settings.minWorldTempC, settings.maxWorldTempC);

        // 5. Blizzard Wind Push on exposed players
        if (settings.blizzardWindPush
                && (currentEvent == WeatherEventType.GALE || currentEvent == WeatherEventType.BLIZZARD)
                && gameTime % 2L == 0L) {
            applyWindPushToExposedPlayers(overworld, currentEvent);
        }

        // 6. Д-4: During Blizzard, outdoor animals & villagers seek nearby warmth or roof shelter!
        if (currentEvent == WeatherEventType.BLIZZARD && gameTime % 80L == 0L) {
            nudgeOutdoorMobsTowardShelter(overworld);
        }
    }

    private static void nudgeOutdoorMobsTowardShelter(ServerLevel overworld) {
        for (ServerPlayer player : overworld.players()) {
            if (player.isSpectator()) continue;
            AABB box = player.getBoundingBox().inflate(24.0D, 8.0D, 24.0D);
            List<PathfinderMob> mobs = overworld.getEntitiesOfClass(
                    PathfinderMob.class,
                    box,
                    m -> (m instanceof Villager || m instanceof Animal) && m.getNavigation().isDone() && !m.isPassenger()
            );
            int nudged = 0;
            for (PathfinderMob mob : mobs) {
                if (nudged >= 5) break;
                BlockPos mp = mob.blockPosition();
                if (!overworld.canSeeSky(mp.above()) || ThermalEngine.getDeltaHeatAt(overworld, mp.above()) >= 5.0F) {
                    continue; // Already sheltered or warm!
                }
                BlockPos bestPos = null;
                float bestScore = -1.0F;
                for (int attempt = 0; attempt < 10; attempt++) {
                    BlockPos cand = mp.offset(
                            overworld.random.nextInt(17) - 8,
                            overworld.random.nextInt(5) - 2,
                            overworld.random.nextInt(17) - 8
                    );
                    if (!overworld.getBlockState(cand).isAir() || overworld.getBlockState(cand.below()).isAir()) {
                        continue;
                    }
                    float score = ThermalEngine.getDeltaHeatAt(overworld, cand) + (!overworld.canSeeSky(cand.above()) ? 12.0F : 0.0F);
                    if (score > bestScore && score >= 6.0F) {
                        bestScore = score;
                        bestPos = cand;
                    }
                }
                if (bestPos != null) {
                    mob.getNavigation().moveTo(bestPos.getX() + 0.5D, bestPos.getY(), bestPos.getZ() + 0.5D, 1.05D);
                    nudged++;
                }
            }
        }
    }

    private static void applyWindPushToExposedPlayers(ServerLevel overworld, WeatherEventType event) {
        float pushStrength = (event == WeatherEventType.BLIZZARD) ? 0.024F : 0.011F;
        float wx = EternalSnowState.getWindDirX() * pushStrength * EternalSnowState.windSpeed;
        float wz = EternalSnowState.getWindDirZ() * pushStrength * EternalSnowState.windSpeed;

        for (ServerPlayer player : overworld.players()) {
            if (player.isCreative() || player.isSpectator() || player.isShiftKeyDown() || player.isFallFlying()) {
                continue;
            }
            BlockPos eye = BlockPos.containing(player.getEyePosition());
            int top = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, eye.getX(), eye.getZ());
            if (eye.getY() >= top) {
                Vec3 dm = player.getDeltaMovement();
                if (dm.horizontalDistanceSqr() < 0.25D) {
                    player.setDeltaMovement(dm.x + wx, dm.y, dm.z + wz);
                    player.hurtMarked = true;
                }
            }
        }
    }

    private static WeatherEventType pickNextWeatherEvent(RandomSource random, WeatherEventType previous) {
        int roll = random.nextInt(100);
        WeatherEventType candidate;
        if (roll < 18) {
            candidate = WeatherEventType.CLEAR_FROST;
        } else if (roll < 52) {
            candidate = WeatherEventType.LIGHT_SNOW;
        } else if (roll < 77) {
            candidate = WeatherEventType.HEAVY_SNOW;
        } else if (roll < 91) {
            candidate = WeatherEventType.GALE;
        } else {
            candidate = WeatherEventType.BLIZZARD;
        }
        if (candidate == previous) {
            candidate = (previous == WeatherEventType.LIGHT_SNOW)
                    ? WeatherEventType.HEAVY_SNOW
                    : WeatherEventType.LIGHT_SNOW;
        }
        return candidate;
    }

    private static float wrapAngleRad(float a) {
        float twoPi = (float) (Math.PI * 2.0D);
        a = a % twoPi;
        if (a < 0.0F) a += twoPi;
        return a;
    }

    private static float wrapAngleDiff(float diff) {
        float pi = (float) Math.PI;
        float twoPi = pi * 2.0F;
        while (diff > pi) diff -= twoPi;
        while (diff < -pi) diff += twoPi;
        return diff;
    }
}
