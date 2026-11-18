package com.eternalsnow.mixin;

import com.eternalsnow.thermal.cells.WorldCellThermal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers pistons, fluids, explosions and automated placements, not only player events. */
@Mixin(LevelChunk.class)
public abstract class CellThermalBlockChangeMixin {
    @Inject(method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("RETURN"))
    private void eternalsnow$thermalChange(BlockPos pos, BlockState state, boolean moving,
                                           CallbackInfoReturnable<BlockState> cir) {
        if (cir.getReturnValue() == null) return;
        LevelChunk chunk = (LevelChunk) (Object) this;
        if (chunk.getLevel() instanceof ServerLevel level && level.getServer().isSameThread())
            WorldCellThermal.blockChanged(level,pos,cir.getReturnValue(),state);
    }
}
