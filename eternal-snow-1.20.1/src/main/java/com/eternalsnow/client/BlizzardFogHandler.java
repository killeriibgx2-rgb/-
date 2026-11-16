package com.eternalsnow.client;

import com.eternalsnow.config.ClientSnowConfig;
import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.weather.WeatherEventType;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.client.event.ViewportEvent;

public final class BlizzardFogHandler {
    private static float smoothedHaze = 0.0F;
    private static float smoothedOvercast = 0.0F;
    private static float smoothedColorBlend = 0.0F;
    private static float smoothedTintR = 0.88F;
    private static float smoothedTintG = 0.93F;
    private static float smoothedTintB = 1.00F;
    private static long lastFogNanos = -1L;

    private static boolean cachedSheltered = false;
    private static long lastShelterCheckTick = -100L;

    private BlizzardFogHandler() {
    }

    /**
     * Client-accurate shelter check using MOTION_BLOCKING (which IS synced to ClientLevel) + upward block scan
     * that strictly ignores BlockTags.LEAVES and snow resting on leaves so forests remain 100% outdoors!
     */
    public static boolean isShelteredIgnoringLeaves(ClientLevel level, BlockPos camBlock) {
        long gt = level.getGameTime();
        if (gt - lastShelterCheckTick < 5L) {
            return cachedSheltered;
        }
        lastShelterCheckTick = gt;

        // Note: MOTION_BLOCKING IS synced to ClientLevel (unlike MOTION_BLOCKING_NO_LEAVES!)
        int topMotion = level.getHeight(Heightmap.Types.MOTION_BLOCKING, camBlock.getX(), camBlock.getZ());
        if (camBlock.getY() >= topMotion) {
            cachedSheltered = false;
            return false;
        }

        // If deep underground with low sky light, definitely sheltered
        int skyLight = level.getBrightness(LightLayer.SKY, camBlock);
        if (skyLight <= 4 && (topMotion - camBlock.getY()) > 6) {
            cachedSheltered = true;
            return true;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int maxScanY = Math.min(level.getMaxBuildHeight() - 1, Math.min(topMotion + 1, camBlock.getY() + 28));
        boolean prevWasLeaves = false;
        for (int y = camBlock.getY() + 1; y <= maxScanY; y++) {
            cursor.set(camBlock.getX(), y, camBlock.getZ());
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
                continue;
            }
            if (!st.getCollisionShape(level, cursor).isEmpty()) {
                cachedSheltered = true;
                return true;
            }
        }

        cachedSheltered = false;
        return false;
    }

    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !EternalSnowState.isOverworldLike(level)) {
            smoothedHaze = 0.0F;
            smoothedOvercast = 0.0F;
            smoothedColorBlend = 0.0F;
            EternalSnowState.clientSmoothOvercast = 0.0F;
            lastFogNanos = -1L;
            return;
        }

        long nowNanos = System.nanoTime();
        float dt = 0.0F;
        if (lastFogNanos > 0L && !mc.isPaused()) {
            dt = Mth.clamp((nowNanos - lastFogNanos) * 1.0e-9F, 0.0F, 0.10F);
        }
        lastFogNanos = nowNanos;

        // Smooth ~16s exponential glide (tau = 4.2s)
        float megaSlowAlpha = 1.0F - (float) Math.exp(-dt / 4.2F);

        ServerSnowSettings settings = EternalSnowState.getEffectiveSettings();
        if (!settings.enabled) {
            smoothedHaze = Mth.lerp(megaSlowAlpha, smoothedHaze, 0.0F);
            smoothedOvercast = Mth.lerp(megaSlowAlpha, smoothedOvercast, 0.0F);
            smoothedColorBlend = Mth.lerp(megaSlowAlpha, smoothedColorBlend, 0.0F);
            EternalSnowState.clientSmoothOvercast = smoothedOvercast;
            return;
        }

        WeatherEventType ev = EternalSnowState.getActiveWeatherEvent();

        // 1. Target sky & sun overcast (gradual_overcast profile across all 5 weather types!)
        float targetOvercast = switch (ev) {
            case CLEAR_FROST -> 0.00F; // Crisp azure sky, bright winter sun & stars
            case LIGHT_SNOW -> 0.34F;  // Soft milky-silver winter sky, gentle sun through frosty haze
            case HEAVY_SNOW -> 0.96F;  // True overcast snowy winter sky (sun hidden behind snow clouds)
            case GALE -> 0.98F;        // Steel-grey windy storm sky
            case BLIZZARD -> 1.00F;    // Full deep blizzard sky
        };
        if (!ClientSnowConfig.clearSkyAndLight && targetOvercast < 0.55F && ev != WeatherEventType.CLEAR_FROST) {
            targetOvercast = 0.65F;
        }
        smoothedOvercast = Mth.lerp(megaSlowAlpha, smoothedOvercast, targetOvercast);
        EternalSnowState.clientSmoothOvercast = smoothedOvercast;

        // 2. Target distance haze & winter atmospheric color per weather
        float targetHaze = switch (ev) {
            case CLEAR_FROST -> 0.06F; // Subtle crisp horizon frost shimmer
            case LIGHT_SNOW -> 0.22F;  // Gentle snowy air veil in the distance
            case HEAVY_SNOW -> 0.52F;  // Cozy thick snowfall veil
            case GALE -> 0.66F;        // Windswept mist
            case BLIZZARD -> 1.00F;    // Dense blizzard whiteout
        };

        float targetColorBlend = switch (ev) {
            case CLEAR_FROST -> 0.22F;
            case LIGHT_SNOW -> 0.48F;
            case HEAVY_SNOW -> 0.74F;
            case GALE -> 0.80F;
            case BLIZZARD -> 0.90F;
        };

        float tr, tg, tb;
        switch (ev) {
            case CLEAR_FROST -> { tr = 0.78F; tg = 0.89F; tb = 1.00F; } // Crisp icy azure horizon
            case LIGHT_SNOW -> { tr = 0.86F; tg = 0.91F; tb = 0.98F; }  // Soft silver-milk snow haze
            case HEAVY_SNOW -> { tr = 0.84F; tg = 0.88F; tb = 0.94F; }  // Calm overcast snow grey-white
            case GALE -> { tr = 0.78F; tg = 0.84F; tb = 0.92F; }        // Cold steel-slate wind haze
            default -> { tr = 0.90F; tg = 0.94F; tb = 0.99F; }          // Bright snow-white blizzard wall
        }

        BlockPos camBlock = event.getCamera().getBlockPosition();
        if (isShelteredIgnoringLeaves(level, camBlock)) {
            targetHaze *= 0.22F;
        }

        smoothedHaze = Mth.lerp(megaSlowAlpha, smoothedHaze, targetHaze);
        smoothedColorBlend = Mth.lerp(megaSlowAlpha, smoothedColorBlend, targetColorBlend);
        smoothedTintR = Mth.lerp(megaSlowAlpha, smoothedTintR, tr);
        smoothedTintG = Mth.lerp(megaSlowAlpha, smoothedTintG, tg);
        smoothedTintB = Mth.lerp(megaSlowAlpha, smoothedTintB, tb);

        if (smoothedHaze <= 0.02F) {
            return;
        }

        float origNear = event.getNearPlaneDistance();
        float origFar = event.getFarPlaneDistance();

        // Continuous mapping from subtle horizon haze (haze=0.06 -> ~90% far) to full blizzard (haze=1.0 -> near=2.5, far=21)
        float desiredNear;
        float desiredFar;
        if (smoothedHaze < 0.35F) {
            float t = smoothedHaze / 0.35F;
            desiredNear = Mth.lerp(t, origNear * 0.85F, Math.min(origNear, 18.0F));
            desiredFar = Mth.lerp(t, origFar * 0.94F, Math.min(origFar, 96.0F));
        } else {
            float t = (smoothedHaze - 0.35F) / 0.65F;
            float smoothT = t * t * (3.0F - 2.0F * t);
            desiredNear = Mth.lerp(smoothT, Math.min(origNear, 18.0F), 2.5F);
            desiredFar = Mth.lerp(smoothT, Math.min(origFar, 96.0F), 21.0F);
        }

        event.setNearPlaneDistance(Math.min(origNear, desiredNear));
        event.setFarPlaneDistance(Math.min(origFar, Math.max(desiredNear + 6.0F, desiredFar)));
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (smoothedColorBlend <= 0.02F) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !EternalSnowState.isOverworldLike(mc.level)) {
            return;
        }

        float star = mc.level.getStarBrightness((float) event.getPartialTick());
        float dayBright = Mth.clamp(1.0F - star * 0.74F, 0.18F, 0.98F);

        float targetR = smoothedTintR * dayBright;
        float targetG = smoothedTintG * dayBright;
        float targetB = smoothedTintB * dayBright;

        float blend = Mth.clamp(smoothedColorBlend, 0.0F, 0.90F);
        event.setRed(Mth.lerp(blend, event.getRed(), targetR));
        event.setGreen(Mth.lerp(blend, event.getGreen(), targetG));
        event.setBlue(Mth.lerp(blend, event.getBlue(), targetB));
    }
}
