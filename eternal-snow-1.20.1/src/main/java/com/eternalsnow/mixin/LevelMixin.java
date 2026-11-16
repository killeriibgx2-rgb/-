package com.eternalsnow.mixin;

import com.eternalsnow.config.ClientSnowConfig;
import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.state.EternalSnowState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Shadow
    public float oRainLevel;

    @Shadow
    public float rainLevel;

    @Shadow
    @Final
    public boolean isClientSide;

    @Shadow
    private int skyDarken;

    @Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
    private void eternalsnow$keepIsRainingAccurate(CallbackInfoReturnable<Boolean> cir) {
        Level self = (Level) (Object) this;
        if (EternalSnowState.isOverworldLike(self) && EternalSnowState.getEffectiveSettings().enabled) {
            cir.setReturnValue(this.rainLevel > 0.2F);
        }
    }

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void eternalsnow$adjustClientSkyAtmosphere(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (!this.isClientSide) {
            return;
        }
        Level self = (Level) (Object) this;
        if (!EternalSnowState.isOverworldLike(self)) {
            return;
        }
        if (!EternalSnowState.getEffectiveSettings().enabled) {
            return;
        }

        // Smooth ~16s winter sky & sun overcast curve updated continuously in BlizzardFogHandler:
        // CLEAR_FROST = 0.00 (bright sunny blue sky), LIGHT_SNOW = 0.34 (soft sun through frosty milk haze),
        // HEAVY_SNOW = 0.96, GALE = 0.98, BLIZZARD = 1.00 (authentic overcast snow sky without harsh sun).
        cir.setReturnValue(Mth.clamp(EternalSnowState.clientSmoothOvercast, 0.0F, 1.0F));
    }

    @Inject(method = "updateSkyBrightness", at = @At("HEAD"), cancellable = true)
    private void eternalsnow$keepClearSkyLight(CallbackInfo ci) {
        Level self = (Level) (Object) this;
        if (!EternalSnowState.isOverworldLike(self)) {
            return;
        }
        ServerSnowSettings settings = EternalSnowState.getEffectiveSettings();
        boolean keepLight = this.isClientSide
                ? (settings.enabled && ClientSnowConfig.clearSkyAndLight)
                : (settings.enabled && settings.keepClearSkyLight);

        if (keepLight) {
            double dayCurve = 0.5D + 2.0D * Mth.clamp(
                    (double) Mth.cos(self.getTimeOfDay(1.0F) * ((float) Math.PI * 2F)),
                    -0.25D,
                    0.25D
            );
            int baseDarken = (int) ((1.0D - dayCurve) * 11.0D);
            // Gently soften daylight during heavy overcast snow/blizzard (+0..2) so weather feels alive
            int weatherSoften = this.isClientSide ? Math.round(EternalSnowState.clientSmoothOvercast * 2.0F) : 0;
            this.skyDarken = Mth.clamp(baseDarken + weatherSoften, 0, 11);
            ci.cancel();
        }
    }
}
