package com.eternalsnow.survival;

import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.thermal.ThermalEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

/**
 * v2.6.6: the cold now applies to every mob, not just the player.
 *
 * Rules:
 *  - no mob burns while it is snowing over it or while it stands in snow (zombies, skeletons,
 *    blazes in the open, etc. simply stop catching fire);
 *  - mobs DO freeze, but MUCH slower than the player: mobFreezeRateMultiplier (default 0.20 =
 *    five times slower). A snow zombie on the surface loses ~1 body-heat unit per 10 s, so it
 *    takes minutes to become sluggish and never instantly dies;
 *  - armour counts (wool / jackets keep a mob warm), so an armoured mob survives far longer.
 *
 * Server-side only, runs on the same 10-tick cadence as the player model, no client cost.
 */
public final class MobSurvivalManager {

    private static final String TAG_MOB_CHILL = "EternalSnowMobChill";

    /** Mobs are 5x slower than the player by default; hard cap of the resulting slowdown. */
    private static final float MAX_SLOWDOWN = 0.30F;

    private MobSurvivalManager() {
    }

    /** True when snow is actually falling on the entity (snowfall event or snow under its feet). */
    public static boolean isSnowedOn(ServerLevel level, LivingEntity entity) {
        BlockPos pos = entity.blockPosition();
        if (level.getBlockState(pos).is(Blocks.SNOW) || level.getBlockState(pos.below()).is(Blocks.SNOW)) {
            return true;
        }
        if (EternalSnowState.snowIntensity <= 0.12F) {
            return false;
        }
        // Outside, during snowfall, and cold enough for the precipitation to be snow.
        return level.canSeeSky(pos)
                && EternalSnowState.weatherEventId != com.eternalsnow.weather.WeatherEventType.CLEAR_FROST.ordinal()
                && EternalSnowState.worldAmbientTempC <= 1.5F;
    }

    /**
     * Called from the damage event: if this mob would catch fire while it is snowed on, the fire
     * simply does not take. Returning true means "cancelled".
     */
    public static boolean shouldExtinguishFire(ServerLevel level, LivingEntity entity) {
        ServerSnowSettings settings = EternalSnowState.getServerSettings();
        if (!settings.enabled || !settings.mobsDoNotBurnInSnow) {
            return false;
        }
        if (entity instanceof Player) {
            return false; // players keep vanilla behaviour; the mod has its own chill model
        }
        return isSnowedOn(level, entity);
    }

    /** Per-tick entry point (only every 10 ticks does real work). */
    public static void tickMob(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        ServerSnowSettings settings = EternalSnowState.getServerSettings();
        if (!settings.enabled) {
            return;
        }

        // Fire in the snow goes out on its own — no burning zombies during a blizzard.
        if (settings.mobsDoNotBurnInSnow && mob.isOnFire() && isSnowedOn(level, mob)) {
            mob.clearFire();
        }

        if (!settings.enableMobFreezing || !settings.enablePlayerTemperature) {
            return;
        }
        if (mob.tickCount % 10 != 0 || mob.isDeadOrDying() || mob.isSpectator()) {
            return;
        }
        if (level.getGameTime() % 2L != 0L) {
            return; // spread mobs across ticks (half of them per tick) to keep TPS flat
        }

        float chill = mob.getPersistentData().getFloat(TAG_MOB_CHILL);
        BlockPos sample = mob.blockPosition().above();
        float airC = ThermalEngine.getLocalTemperatureC(level, sample);

        boolean inWater = mob.isInWaterOrBubble();
        boolean snowedOn = isSnowedOn(level, mob);

        if (airC < 4.0F || inWater) {
            float severity = (4.0F - airC) / 26.0F;
            if (inWater) {
                severity *= 2.0F;
            }
            if (snowedOn) {
                severity *= 1.25F;
            }
            if (severity < 0.0F) {
                severity = 0.0F;
            }

            float armorMult = 1.0F - ArmorInsulationHelper.computeColdDrainReduction(
                    ArmorInsulationHelper.getTotalWarmthPoints(mob, settings.armorInsulationMultiplier));
            float rate = settings.mobFreezeRateMultiplier * settings.playerTempRateMultiplier;

            // Fire / lava nearby warms the mob back up (same ThermalEngine field the player uses).
            float warmth = ThermalEngine.getDeltaHeatAt(level, sample);
            if (warmth > 1.0F) {
                severity *= Mth.clamp(1.0F - warmth / 12.0F, 0.15F, 1.0F);
            }

            chill -= severity * 1.45F * rate * armorMult;
        } else {
            chill = Mth.approach(chill, 0.0F, 1.2F);
        }

        chill = Mth.clamp(chill, -100.0F, 0.0F);
        mob.getPersistentData().putFloat(TAG_MOB_CHILL, chill);

        // Consequences: only deep chill, and never instant death.
        if (chill <= -55.0F) {
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false, false));
            if (chill <= -75.0F) {
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, false));
            }
            if (chill <= -92.0F && mob.tickCount % 100 == 0) {
                mob.hurt(level.damageSources().freeze(), 1.0F);
            }
        }

        // Visual: cold mobs shiver and puff frost breath (cheap, server particles are broadcast).
        if (chill <= -45.0F && mob.tickCount % 40 == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                    mob.getX(), mob.getY() + mob.getBbHeight() * 0.8D, mob.getZ(),
                    1, 0.18D, 0.12D, 0.18D, 0.0D);
        }
    }

}
