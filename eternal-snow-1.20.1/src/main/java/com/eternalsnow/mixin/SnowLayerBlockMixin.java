package com.eternalsnow.mixin;

import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.state.EternalSnowState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SnowLayerBlock.class)
public abstract class SnowLayerBlockMixin {

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void eternalsnow$applySurfaceRules(
            BlockState state,
            LevelReader level,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ServerSnowSettings s = EternalSnowState.getEffectiveSettings();
        if (!s.enabled) {
            return;
        }

        BlockState below = level.getBlockState(pos.below());

        // 1. Ice toggle
        if (below.is(Blocks.ICE)
                || below.is(Blocks.PACKED_ICE)
                || below.is(Blocks.BLUE_ICE)
                || below.is(Blocks.FROSTED_ICE)) {
            cir.setReturnValue(s.snowOnIce);
            return;
        }

        // 2. Leaves mode (0 = No, 1 = On Leaves, 2 = Pass Through Leaves)
        if (below.is(BlockTags.LEAVES)) {
            if (s.leavesMode != 1) {
                cir.setReturnValue(false);
            }
            return;
        }

        // 3. Slabs toggle
        if (below.getBlock() instanceof SlabBlock) {
            if (!s.snowOnSlabs) {
                cir.setReturnValue(false);
            }
            return;
        }

        // 4. Glass toggle
        if (below.getBlock() instanceof AbstractGlassBlock
                || below.getBlock() instanceof HalfTransparentBlock
                || below.is(Blocks.GLASS)
                || below.is(Blocks.TINTED_GLASS)) {
            if (!s.snowOnGlass) {
                cir.setReturnValue(false);
            }
            return;
        }

        // 5. Stairs toggle
        if (below.getBlock() instanceof StairBlock) {
            if (!s.snowOnStairs) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void eternalsnow$delegateMeltingToThermalEngine(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci
    ) {
        // ThermalEngine handles realistic 3D heat melting so torches/furnaces melt according to configured °C!
        if (EternalSnowState.getServerSettings().enabled) {
            ci.cancel();
        }
    }
}
