package com.eternalsnow.mixin;

import com.eternalsnow.state.EternalSnowState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Shadow
    @Final
    private ServerLevelData serverLevelData;

    @Inject(method = "resetWeatherCycle", at = @At("HEAD"), cancellable = true)
    private void eternalsnow$keepSnowAfterSleep(CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (EternalSnowState.isOverworldLike(self) && EternalSnowState.getServerSettings().enabled) {
            this.serverLevelData.setThunderTime(0);
            this.serverLevelData.setThundering(false);
            this.serverLevelData.setClearWeatherTime(0);
            this.serverLevelData.setRainTime(Integer.MAX_VALUE);
            this.serverLevelData.setRaining(true);
            ci.cancel();
        }
    }
}
