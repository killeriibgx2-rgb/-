package com.eternalsnow.food;

import com.eternalsnow.block.FoodShelfBlock;
import com.eternalsnow.block.FoodShelfBlockEntity;
import com.eternalsnow.thermal.ThermalEngine;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * v2.7 — everything the food shelf needs that is not the block itself: hook picking, what may hang,
 * and the ageing pass for loaded shelves.
 */
public final class FoodShelfManager {

    /** How far from a player a shelf is still worth ageing (loaded chunk, visible range). */
    private static final double SHELF_SCAN_RADIUS = 48.0D;

    private FoodShelfManager() {
    }

    /**
     * Resolves which of the {@link FoodShelfBlock#HOOKS} hooks the player is actually aiming at.
     *
     * <p>Do <em>not</em> take {@code floor(hitX * hooks)}: that is the "looks about right" version and
     * it misses constantly. Instead the exact hit point is projected onto the axis the shelf runs
     * along, clamped into the block, and rounded to the NEAREST hook centre. A hook is two pixels
     * wide, so an exact-hit rule would be unhittable — nearest-centre is what feels accurate.
     */
    public static int hookFromHit(BlockState state, BlockPos pos, BlockHitResult hit) {
        Direction facing = state.getValue(FoodShelfBlock.FACING);
        Vec3 location = hit.getLocation();
        double local = alongShelf(facing, location.x - pos.getX(), location.z - pos.getZ());

        // Model centres are 1.6, 4.6, 7.6, 10.6, 13.6 pixels, NOT a 3.2px grid.
        int hook = (int) Math.round((local * 16.0D - 1.6D) / 3.0D);
        return Mth.clamp(hook, 0, FoodShelfBlock.HOOKS - 1);
    }

    /** Centre in the unrotated (north-facing) JSON model. */
    public static float hookCenterAlong(int hook) {
        return (1.6F + 3.0F * hook) / 16.0F;
    }

    /** Inverse of the blockstate model rotation, shared with the item placement convention. */
    private static double alongShelf(Direction facing, double localX, double localZ) {
        return switch (facing) {
            case NORTH -> localX;
            case SOUTH -> 1.0D - localX;
            case WEST -> 1.0D - localZ;
            default -> localZ; // EAST
        };
    }

    /** Anything edible, plus potions/thermos — the owner asked "anything goes" on the shelf. */
    public static boolean isHangable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.isEdible()) {
            return true;
        }
        return stack.getItem() instanceof net.minecraft.world.item.PotionItem
                || stack.is(com.eternalsnow.registry.ModRegistry.COPPER_THERMOS.get())
                || stack.is(com.eternalsnow.registry.ModRegistry.EMPTY_CUP.get());
    }

    /**
     * Ages every loaded shelf that has a player within {@link #SHELF_SCAN_RADIUS}.
     *
     * <p>Walks only the chunk block-entity maps of loaded chunks around each player: a handful of map
     * lookups, never a 48³ cube of block positions (that would be ~900k misses per pass). Chunks that
     * are not loaded simply return {@code null} and cost nothing.
     */
    public static void scanShelvesNearPlayers(ServerLevel level, long now) {
        if (level.players().isEmpty()) {
            return;
        }
        Set<BlockPos> seen = new HashSet<>();
        int chunkRadius = (int) Math.ceil(SHELF_SCAN_RADIUS / 16.0D);

        for (ServerPlayer player : level.players()) {
            int pcx = player.getBlockX() >> 4;
            int pcz = player.getBlockZ() >> 4;
            for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(pcx + dx, pcz + dz);
                    if (chunk == null) {
                        continue;
                    }
                    for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                        if (blockEntity instanceof FoodShelfBlockEntity shelf
                                && seen.add(shelf.getBlockPos())) {
                            ageShelf(level, shelf, now);
                        }
                    }
                }
            }
        }
    }

    private static void ageShelf(ServerLevel level, FoodShelfBlockEntity shelf, long now) {
        float env = ThermalEngine.getLocalTemperatureC(level, shelf.getBlockPos());
        for (int hook = 0; hook < FoodShelfBlock.HOOKS; hook++) {
            ItemStack hanging = shelf.getHook(hook);
            if (hanging.isEmpty()) {
                continue;
            }
            ItemStack updated = FoodAgingManager.advance(hanging, env, now);
            if (updated != null) {
                shelf.setHook(hook, updated);
            }
        }
    }
}
