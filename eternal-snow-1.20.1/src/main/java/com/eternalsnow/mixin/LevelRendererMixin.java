package com.eternalsnow.mixin;

import com.eternalsnow.state.EternalSnowState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    @Nullable
    private ClientLevel level;

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void eternalsnow$replaceVanillaSnowWithWindSnow(
            LightTexture lightTexture,
            float partialTick,
            double camX,
            double camY,
            double camZ,
            CallbackInfo ci
    ) {
        if (this.level != null
                && EternalSnowState.isOverworldLike(this.level)
                && EternalSnowState.getEffectiveSettings().enabled) {
            // Replaced by GPU-batched 3D WindSnowRenderer in RenderLevelStageEvent.Stage.AFTER_WEATHER!
            ci.cancel();
        }
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void eternalsnow$stripVanillaClouds(
            PoseStack poseStack,
            Matrix4f projectionMatrix,
            float partialTick,
            double camX,
            double camY,
            double camZ,
            CallbackInfo ci
    ) {
        if (this.level != null
                && EternalSnowState.isOverworldLike(this.level)
                && EternalSnowState.getEffectiveSettings().enabled) {
            // Completely strip vanilla cloud rendering for clean winter sky & higher FPS!
            ci.cancel();
        }
    }

    @Inject(method = "tickRain", at = @At("HEAD"), cancellable = true)
    private void eternalsnow$skipVanillaRainSplash(Camera camera, CallbackInfo ci) {
        if (this.level != null
                && EternalSnowState.isOverworldLike(this.level)
                && EternalSnowState.getEffectiveSettings().enabled) {
            ci.cancel();
        }
    }
}
