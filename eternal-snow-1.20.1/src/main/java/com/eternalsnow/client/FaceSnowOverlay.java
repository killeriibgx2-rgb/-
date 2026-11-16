package com.eternalsnow.client;

import com.eternalsnow.EternalSnowMod;
import com.eternalsnow.config.ClientSnowConfig;
import com.eternalsnow.registry.ModRegistry;
import com.eternalsnow.state.EternalSnowState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Random;

public final class FaceSnowOverlay {
    private static final ResourceLocation FLAKE_TEX = new ResourceLocation(EternalSnowMod.MODID, "textures/environment/wind_flake.png");
    private static final ResourceLocation POWDER_SNOW_OUTLINE = new ResourceLocation("textures/misc/powder_snow_outline.png");
    private static final int MAX_STUCK_FLAKES = 18;

    private static final float[] FX = new float[MAX_STUCK_FLAKES];
    private static final float[] FY = new float[MAX_STUCK_FLAKES];
    private static final float[] FSIZE = new float[MAX_STUCK_FLAKES];
    private static final float[] FLIFE = new float[MAX_STUCK_FLAKES];
    private static final float[] FMAX_LIFE = new float[MAX_STUCK_FLAKES];
    private static final Random RNG = new Random();
    private static long lastTickHandled = -1L;

    // Frame-smooth frost overlay alpha (zero 0.5s vanilla ticksFrozen stutter!)
    private static float smoothFrostAlpha = 0.0F;
    private static long lastFrostNanos = -1L;

    private FaceSnowOverlay() {
    }

    public static final IGuiOverlay OVERLAY = FaceSnowOverlay::render;

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        if (!EternalSnowState.getEffectiveSettings().enabled) {
            return;
        }

        long nowNanos = System.nanoTime();
        float dt = (lastFrostNanos > 0L)
                ? Mth.clamp((nowNanos - lastFrostNanos) * 1.0e-9F, 0.0F, 0.10F)
                : 0.016F;
        lastFrostNanos = nowNanos;

        float bodyState = EternalSnowState.clientBodyTempState;
        boolean hasColdImmunity = ModRegistry.COLD_IMMUNITY.isPresent()
                && player.hasEffect(ModRegistry.COLD_IMMUNITY.get());

        // 1. Silky-smooth Hypothermia Frost Vignette (replaces choppy vanilla ticksFrozen!)
        float targetFrost = 0.0F;
        if (ClientSnowConfig.showSurvivalHud && !hasColdImmunity && bodyState <= -35.0F
                && !player.isCreative() && !player.isSpectator()) {
            targetFrost = Mth.clamp((-bodyState - 35.0F) / 65.0F, 0.0F, 0.88F);
        }
        float blend = 1.0F - (float) Math.exp(-dt * 2.4F);
        smoothFrostAlpha = Mth.lerp(blend, smoothFrostAlpha, targetFrost);

        if (smoothFrostAlpha > 0.01F) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(0.88F, 0.96F, 1.0F, smoothFrostAlpha);
            graphics.blit(POWDER_SNOW_OUTLINE, 0, 0, -90, 0.0F, 0.0F, screenWidth, screenHeight, screenWidth, screenHeight);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }

        // 2. Overheating warm edge border when bodyTempState >= 50
        if (ClientSnowConfig.showSurvivalHud && bodyState >= 50.0F && !player.isCreative() && !player.isSpectator()) {
            float heatIntensity = Mth.clamp((bodyState - 50.0F) / 50.0F, 0.0F, 1.0F);
            int alpha = (int) (heatIntensity * 95.0F);
            int color = (alpha << 24) | 0xFF5514;
            int border = Math.max(8, Math.min(screenWidth, screenHeight) / 14);
            graphics.fillGradient(0, 0, screenWidth, border, color, 0x00FF5514);
            graphics.fillGradient(0, screenHeight - border, screenWidth, screenHeight, 0x00FF5514, color);
        }

        // 3. Face Snowflakes sticking when outdoors & looking into windy snow!
        if (!ClientSnowConfig.faceSnowOverlay || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (!EternalSnowState.isOverworldLike(mc.level) || player.isUnderWater()) {
            return;
        }

        long gameTime = mc.level.getGameTime();
        if (gameTime != lastTickHandled) {
            lastTickHandled = gameTime;
            tickStuckFlakes(mc, player);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (int i = 0; i < MAX_STUCK_FLAKES; i++) {
            if (FLIFE[i] <= 0.0F) {
                continue;
            }
            float progress = FLIFE[i] / Math.max(1.0F, FMAX_LIFE[i]);
            float alpha = progress > 0.85F
                    ? (1.0F - progress) / 0.15F
                    : progress * 0.82F;
            alpha = Mth.clamp(alpha, 0.0F, 0.72F);
            if (alpha <= 0.02F) {
                continue;
            }

            int px = Math.round(FX[i] * screenWidth);
            int py = Math.round(FY[i] * screenHeight + (1.0F - progress) * 8.0F);
            int sz = Math.round(FSIZE[i]);

            RenderSystem.setShaderColor(0.95F, 0.98F, 1.0F, alpha);
            graphics.blit(FLAKE_TEX, px - sz / 2, py - sz / 2, 0, 0, sz, sz, sz, sz);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void tickStuckFlakes(Minecraft mc, Player player) {
        for (int i = 0; i < MAX_STUCK_FLAKES; i++) {
            if (FLIFE[i] > 0.0F) {
                FLIFE[i] -= 1.0F;
            }
        }

        float windSpeed = EternalSnowState.windSpeed;
        float snowIntensity = EternalSnowState.snowIntensity;
        if (windSpeed < 0.22F || snowIntensity < 0.20F) {
            return;
        }

        BlockPos eyeBlock = BlockPos.containing(player.getEyePosition());
        if (!mc.level.canSeeSky(eyeBlock) && !mc.level.canSeeSky(eyeBlock.above())) {
            return;
        }

        Vec3 look = player.getLookAngle();
        double intoWindDot = -(look.x * EternalSnowState.getWindDirX() + look.z * EternalSnowState.getWindDirZ());
        if (intoWindDot < 0.20D) {
            return;
        }

        float spawnChance = (float) intoWindDot * windSpeed * snowIntensity * 0.42F;
        if (RNG.nextFloat() < spawnChance) {
            for (int i = 0; i < MAX_STUCK_FLAKES; i++) {
                if (FLIFE[i] <= 0.0F) {
                    float rx = RNG.nextFloat();
                    float ry = RNG.nextFloat();
                    if (Math.abs(rx - 0.5F) < 0.12F && Math.abs(ry - 0.5F) < 0.12F) {
                        rx = rx < 0.5F ? 0.22F : 0.78F;
                    }
                    FX[i] = 0.06F + rx * 0.88F;
                    FY[i] = 0.08F + ry * 0.82F;
                    FSIZE[i] = 10.0F + RNG.nextInt(14);
                    FMAX_LIFE[i] = 36.0F + RNG.nextInt(32);
                    FLIFE[i] = FMAX_LIFE[i];
                    break;
                }
            }
        }
    }
}
