package com.eternalsnow.mixin;

import com.eternalsnow.state.EternalSnowBiomeAccess;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.world.SnowAccumulationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public abstract class BiomeMixin implements EternalSnowBiomeAccess {

    @Unique
    private boolean eternalsnow$nonOverworld = false;

    @Override
    public boolean eternalsnow$isNonOverworldBiome() {
        return this.eternalsnow$nonOverworld;
    }

    @Override
    public void eternalsnow$setNonOverworldBiome(boolean nonOverworld) {
        this.eternalsnow$nonOverworld = nonOverworld;
    }

    @Inject(method = "hasPrecipitation", at = @At("HEAD"), cancellable = true)
    private void eternalsnow$forceOverworldPrecipitation(CallbackInfoReturnable<Boolean> cir) {
        if (!this.eternalsnow$nonOverworld && EternalSnowState.isSnowPrecipitationEverywhere()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true)
    private void eternalsnow$overridePrecipitation(BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
        if (!this.eternalsnow$nonOverworld && EternalSnowState.isSnowPrecipitationEverywhere()) {
            cir.setReturnValue(Biome.Precipitation.SNOW);
        }
    }

    @Inject(method = "warmEnoughToRain", at = @At("HEAD"), cancellable = true)
    private void eternalsnow$forceColdEverywhere(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!this.eternalsnow$nonOverworld && EternalSnowState.isGlobalColdActive()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldMeltFrozenOceanIcebergSlightly", at = @At("HEAD"), cancellable = true)
    private void eternalsnow$preventIcebergMelting(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!this.eternalsnow$nonOverworld && EternalSnowState.isSnowPrecipitationEverywhere()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z", at = @At("HEAD"), cancellable = true)
    private void eternalsnow$protectIceFishingBobberHole(LevelReader level, BlockPos waterPos, boolean mustBeAtEdge, CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof ServerLevel sl && SnowAccumulationManager.isProtectedByFishingBobber(sl, waterPos)) {
            cir.setReturnValue(false);
        }
    }
}
