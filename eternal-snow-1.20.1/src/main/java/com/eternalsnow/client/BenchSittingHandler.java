package com.eternalsnow.client;

import com.eternalsnow.block.WoodenBenchBlock;
import com.eternalsnow.config.ClientSnowConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.TickEvent;

/**
 * v2.6.4: ambience only — right-click a wooden bench and the local player sits down at bench height.
 * 100% client-side: the server does not know about it, so nobody can get stuck, and multiplayer is safe.
 * Sneaking or moving away stands the player up again.
 */
public final class BenchSittingHandler {

    private static BlockPos sitPos = null;

    private BenchSittingHandler() {
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            sitPos = null;
            return;
        }
        if (!ClientSnowConfig.benchSitting) {
            sitPos = null;
            return;
        }

        if (sitPos != null) {
            boolean moved = player.blockPosition().distSqr(sitPos) > 6.25D;
            boolean benchGone = !(mc.level.getBlockState(sitPos).getBlock() instanceof WoodenBenchBlock);
            if (player.isShiftKeyDown() || moved || benchGone) {
                standUp(player);
                return;
            }
            return;
        }

        if (mc.screen != null || mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (!(mc.hitResult instanceof BlockHitResult bhr)) {
            return;
        }
        if (!mc.options.keyUse.isDown() || !mc.options.keyShift.isDown()) {
            return;
        }
        BlockState state = mc.level.getBlockState(bhr.getBlockPos());
        if (!(state.getBlock() instanceof WoodenBenchBlock)) {
            return;
        }
        sitPos = bhr.getBlockPos().immutable();
        player.setPos(player.getX(), sitPos.getY() + 0.55D, player.getZ());
    }

    private static void standUp(LocalPlayer player) {
        if (sitPos != null) {
            player.setPos(player.getX(), sitPos.getY() + 1.0D, player.getZ());
        }
        sitPos = null;
    }
}
