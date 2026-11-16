package com.eternalsnow.mixin;

import com.eternalsnow.state.EternalSnowState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Д-1: Sweeps outdoor campfire & rooftop chimney smoke smoothly along the live 3D winter wind vector!
 */
@Mixin(CampfireSmokeParticle.class)
public abstract class CampfireSmokeParticleMixin extends Particle {
    protected CampfireSmokeParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void eternalsnow$driftSmokeWithWind(CallbackInfo ci) {
        if (!EternalSnowState.isOverworldLike(this.level) || !EternalSnowState.getEffectiveSettings().enabled) {
            return;
        }
        BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
        if (this.level.getBrightness(LightLayer.SKY, pos) < 13) {
            return; // Indoors / under roof -> smoke rises calm and straight
        }
        float wind = EternalSnowState.windSpeed;
        double targetVx = EternalSnowState.getWindDirX() * wind * 0.16D;
        double targetVz = EternalSnowState.getWindDirZ() * wind * 0.16D;
        this.xd = Mth.lerp(0.06D, this.xd, targetVx);
        this.zd = Mth.lerp(0.06D, this.zd, targetVz);
    }
}
