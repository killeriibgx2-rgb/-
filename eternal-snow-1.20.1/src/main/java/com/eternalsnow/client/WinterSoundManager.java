package com.eternalsnow.client;

import com.eternalsnow.config.ClientSnowConfig;
import com.eternalsnow.registry.ModRegistry;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.weather.WeatherEventType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;

public final class WinterSoundManager {
    private static WindLoopSound calmLoop;
    private static WindLoopSound galeLoop;
    private static WindLoopSound blizzardLoop;

    private static float cachedExposure = 1.0F;
    private static long lastExposureTick = -100L;

    private WinterSoundManager() {
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Player player = mc.player;

        if (level == null || player == null || mc.isPaused()
                || !EternalSnowState.isOverworldLike(level)
                || !EternalSnowState.getEffectiveSettings().enabled) {
            fadeOutAll();
            return;
        }

        float masterVol = ClientSnowConfig.windSoundVolumePercent / 100.0F;
        if (masterVol <= 0.01F) {
            fadeOutAll();
            return;
        }

        long gt = level.getGameTime();
        if (gt - lastExposureTick >= 5L) {
            lastExposureTick = gt;
            cachedExposure = computeAcousticWindExposure(level, BlockPos.containing(player.getEyePosition()));
        }

        WeatherEventType ev = EternalSnowState.getActiveWeatherEvent();
        float wind = EternalSnowState.windSpeed;

        // Indoors / in caves we also pitch wind slightly lower so muffled storm sounds distant & deep through walls!
        float mufflePitchFactor = 0.82F + 0.18F * cachedExposure;

        float targetCalm = switch (ev) {
            case CLEAR_FROST -> 0.16F;
            case LIGHT_SNOW -> 0.30F;
            case HEAVY_SNOW -> 0.38F;
            case GALE -> 0.18F;
            case BLIZZARD -> 0.10F;
        } * cachedExposure * masterVol;

        float targetGale = switch (ev) {
            case CLEAR_FROST -> 0.0F;
            case LIGHT_SNOW -> 0.05F * wind;
            case HEAVY_SNOW -> 0.28F;
            case GALE -> 0.72F;
            case BLIZZARD -> 0.45F;
        } * cachedExposure * masterVol;

        float targetBlizzard = (ev == WeatherEventType.BLIZZARD ? 0.88F : 0.0F) * cachedExposure * masterVol;

        calmLoop = ensureAndUpdate(mc, calmLoop, ModRegistry.WIND_CALM.get(), targetCalm, (0.92F + wind * 0.15F) * mufflePitchFactor);
        galeLoop = ensureAndUpdate(mc, galeLoop, ModRegistry.WIND_GALE.get(), targetGale, (0.90F + wind * 0.22F) * mufflePitchFactor);
        blizzardLoop = ensureAndUpdate(mc, blizzardLoop, ModRegistry.WIND_BLIZZARD.get(), targetBlizzard, (0.95F + wind * 0.12F) * mufflePitchFactor);
    }

    /**
     * 100% Client-Accurate Acoustic Wind Exposure (uses MOTION_BLOCKING + LightLayer.SKY + real 3D block checks,
     * NEVER server-only MOTION_BLOCKING_NO_LEAVES!):
     * - Caves / Mines at ANY Y: 0.00 (0% complete silence!)
     * - Enclosed House / Cabin (roof overhead + walls/windows around): 0.035 (3.5% faint muffled whisper!)
     * - Porch / Open doorway under a roof: 0.14..0.28
     * - Outdoors & Forests (leaves ignored!): 1.00 (100%)
     */
    private static float computeAcousticWindExposure(ClientLevel level, BlockPos eyePos) {
        int skyLightHere = level.getBrightness(LightLayer.SKY, eyePos);

        // 1. Deep cave / mine anywhere at any Y where sky light doesn't even reach -> 0% COMPLETE SILENCE!
        if (skyLightHere <= 1) {
            return 0.0F;
        }

        // 2. Scan upward from eyePos up to client-synced MOTION_BLOCKING top, counting solid NON-LEAF overhead blocks
        int topMotion = level.getHeight(Heightmap.Types.MOTION_BLOCKING, eyePos.getX(), eyePos.getZ());
        if (eyePos.getY() >= topMotion) {
            return 1.0F; // Completely open sky above head!
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int solidOverhead = 0;
        int maxScanY = Math.min(level.getMaxBuildHeight() - 1, Math.min(topMotion + 1, eyePos.getY() + 24));
        boolean prevWasLeaves = false;

        for (int y = eyePos.getY() + 1; y <= maxScanY; y++) {
            cursor.set(eyePos.getX(), y, eyePos.getZ());
            BlockState st = level.getBlockState(cursor);
            if (st.isAir()) {
                prevWasLeaves = false;
                continue;
            }
            if (st.is(BlockTags.LEAVES)) {
                prevWasLeaves = true;
                continue;
            }
            if ((st.is(Blocks.SNOW) || st.is(Blocks.SNOW_BLOCK)) && prevWasLeaves) {
                continue; // Snow sitting on forest canopy is still outdoors!
            }
            if (!st.getCollisionShape(level, cursor).isEmpty()) {
                solidOverhead++;
            }
        }

        // If overhead was ONLY tree leaves & snow on leaves (0 solid roof/rock blocks) -> 100% outdoors in forest!
        if (solidOverhead == 0 && (topMotion - eyePos.getY()) <= 24) {
            return 1.0F;
        }

        // 3. Cave / Mine entrance check: thick rock/earth ceiling (>= 4 solid blocks) & low sky light -> 0%..3%!
        if (solidOverhead >= 4 || (topMotion - eyePos.getY()) > 24) {
            if (skyLightHere <= 7) {
                return 0.0F;
            }
            return 0.03F;
        }

        // 4. Player is under a solid roof (1..3 blocks thick, e.g. house/cabin/porch)!
        // Cast 4 horizontal rays (N, S, E, W up to 6 blocks) to check if walls/glass/closed doors enclose the room!
        int walledSides = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            boolean hitWall = false;
            for (int step = 1; step <= 6; step++) {
                cursor.set(eyePos.getX() + dir.getStepX() * step, eyePos.getY(), eyePos.getZ() + dir.getStepZ() * step);
                BlockState st = level.getBlockState(cursor);
                if (isAcousticWall(level, cursor, st)) {
                    hitWall = true;
                    break;
                }
            }
            if (hitWall) {
                walledSides++;
            }
        }

        // Inside a building (3 or 4 walled/glazed sides + solid roof) -> heavily muffled ~3.5%..5% whisper!
        if (walledSides >= 4) {
            return 0.035F;
        }
        if (walledSides == 3) {
            return 0.065F;
        }
        if (walledSides == 2) {
            return 0.14F;
        }
        // Under an open gazebo/canopy with 0..1 walls
        return 0.28F;
    }

    private static boolean isAcousticWall(ClientLevel level, BlockPos pos, BlockState st) {
        if (st.isAir() || st.is(BlockTags.LEAVES)) {
            return false;
        }
        if (st.getBlock() instanceof DoorBlock) {
            return !st.getValue(DoorBlock.OPEN);
        }
        if (st.getBlock() instanceof TrapDoorBlock) {
            return !st.getValue(TrapDoorBlock.OPEN);
        }
        // Glass, glass panes, planks, stone, logs, stairs, slabs all block wind sound!
        return !st.getCollisionShape(level, pos).isEmpty();
    }

    public static void fadeOutAll() {
        if (calmLoop != null) calmLoop.setTargetVolume(0.0F);
        if (galeLoop != null) galeLoop.setTargetVolume(0.0F);
        if (blizzardLoop != null) blizzardLoop.setTargetVolume(0.0F);
    }

    private static WindLoopSound ensureAndUpdate(
            Minecraft mc,
            WindLoopSound current,
            SoundEvent event,
            float targetVol,
            float targetPitch
    ) {
        if (targetVol <= 0.003F && (current == null || current.isStopped())) {
            return null;
        }
        if (current == null || current.isStopped() || !mc.getSoundManager().isActive(current)) {
            current = new WindLoopSound(event, targetVol, targetPitch);
            mc.getSoundManager().play(current);
        } else {
            current.setTargetVolume(targetVol);
            current.setTargetPitch(targetPitch);
        }
        return current;
    }

    private static final class WindLoopSound extends AbstractTickableSoundInstance {
        private float targetVolume;
        private float targetPitch;
        private int quietTicks = 0;

        protected WindLoopSound(SoundEvent soundEvent, float initialTargetVol, float initialPitch) {
            super(soundEvent, SoundSource.WEATHER, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.targetVolume = Mth.clamp(initialTargetVol, 0.0F, 1.5F);
            this.targetPitch = Mth.clamp(initialPitch, 0.65F, 1.45F);
            this.volume = Math.max(0.005F, this.targetVolume * 0.25F);
            this.pitch = this.targetPitch;
        }

        public void setTargetVolume(float targetVolume) {
            this.targetVolume = Mth.clamp(targetVolume, 0.0F, 1.5F);
        }

        public void setTargetPitch(float targetPitch) {
            this.targetPitch = Mth.clamp(targetPitch, 0.65F, 1.45F);
        }

        @Override
        public void tick() {
            // Fast responsive indoor/cave acoustic damping (~0.8s smooth fade when stepping inside!)
            float rate = (this.targetVolume < this.volume) ? 0.16F : 0.08F;
            this.volume = Mth.lerp(rate, this.volume, this.targetVolume);
            this.pitch = Mth.lerp(0.10F, this.pitch, this.targetPitch);

            if (this.targetVolume <= 0.002F && this.volume <= 0.004F) {
                this.volume = 0.0F;
                this.quietTicks++;
                if (this.quietTicks > 20) {
                    this.stop();
                }
            } else {
                this.quietTicks = 0;
            }
        }
    }
}
