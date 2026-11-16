package com.eternalsnow.mixin;

import com.eternalsnow.state.EternalSnowState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BaseAshSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Д-1: Sweeps outdoor furnace, torch, and chimney ash/smoke particles along the live 3D winter wind vector!
 */
@Mixin(BaseAshSmokeParticle.class)
public abstract class BaseAshSmokeParticleMixin extends Particle {
    protected BaseAshSmokeParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void eternalsnow$driftAshSmokeWithWind(CallbackInfo ci) {
        if (!EternalSnowState.isOverworldLike(this.level) || !EternalSnowState.getEffectiveSettings().enabled) {
            return;
        }
        BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
        if (this.level.getBrightness(LightLayer.SKY, pos) < 13) {
            return;
        }
        float wind = EternalSnowState.windSpeed;
        double targetVx = EternalSnowState.getWindDirX() * wind * 0.13D;
        double targetVz = EternalSnowState.getWindDirZ() * wind * 0.13D;
        this.xd = Mth.lerp(0.08D, this.xd, targetVx);
        this.zd = Mth.lerp(0.08D, this.zd, targetVz);
    }
}
