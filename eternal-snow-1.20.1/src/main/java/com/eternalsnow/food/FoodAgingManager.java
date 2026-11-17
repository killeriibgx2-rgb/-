package com.eternalsnow.food;

import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.thermal.ThermalEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * v2.7 — the "aging pass": walks the containers that are actually worth walking.
 *
 * <p>No per-item ticker. Every {@link #SCAN_INTERVAL} ticks the server visits
 * <ul>
 *   <li>every online player's inventory (which already covers armor + offhand) and ender chest,
 *       with the environment set to body temperature,</li>
 *   <li>every open container, with the environment taken from the block's own temperature,</li>
 *   <li>food shelves within reach of a player.</li>
 * </ul>
 * An unloaded chest costs nothing: it is caught up analytically on the next visit, because
 * {@link FoodState#scan} integrates the elapsed time in one closed-form step.
 */
public final class FoodAgingManager {

    /** 10 seconds. Cheap enough to run per level, fast enough that a thaw is never a surprise. */
    public static final int SCAN_INTERVAL = 200;

    private static final List<BlockPos> OPEN_CONTAINERS = new ArrayList<>();

    private FoodAgingManager() {
    }

    public static void onContainerOpened(BlockPos pos) {
        if (!OPEN_CONTAINERS.contains(pos)) {
            OPEN_CONTAINERS.add(pos);
        }
    }

    public static void onContainerClosed(BlockPos pos) {
        OPEN_CONTAINERS.remove(pos);
    }

    public static void clearSession() {
        OPEN_CONTAINERS.clear();
    }

    /** Called from the server tick on every level; does its own 200-tick gating. */
    public static void tickLevel(ServerLevel level) {
        ServerSnowSettings settings = EternalSnowState.getServerSettings();
        if (!settings.enabled) {
            return;
        }
        if (level.getGameTime() % SCAN_INTERVAL != 0L) {
            return;
        }
        if (!EternalSnowState.isOverworldLike(level)) {
            return; // the Nether and the End are not part of the winter model at all
        }

        long now = level.getGameTime();

        for (ServerPlayer player : level.players()) {
            // A carried item equilibrates toward body heat, not toward the air around the player:
            // a steak in your pocket does not freeze while you run through a blizzard.
            scanContainer(player.getInventory(), FoodState.BODY_C, now);
            scanContainer(player.getEnderChestInventory(), FoodState.BODY_C, now);
        }

        for (int i = OPEN_CONTAINERS.size() - 1; i >= 0; i--) {
            BlockPos pos = OPEN_CONTAINERS.get(i);
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof Container container)) {
                OPEN_CONTAINERS.remove(i);
                continue;
            }
            scanContainer(container, ThermalEngine.getLocalTemperatureC(level, pos), now);
        }

        FoodShelfManager.scanShelvesNearPlayers(level, now);
    }

    /**
     * One pass over one container. The slot is written back only when something actually changed, so
     * a chest full of fresh steak costs a handful of tag reads and nothing else.
     */
    public static boolean scanContainer(Container container, float envC, long now) {
        boolean changed = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack result = advance(stack, envC, now);
            if (result != null) {
                // ALWAYS store a fresh instance: the client menu may still hold the old one, and
                // mutating it in place would make the tooltip and the next scan disagree.
                container.setItem(slot, result);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Advances one stack in place.
     *
     * @return {@code null} when nothing changed, otherwise the stack to store back (the same instance,
     *         or a brand-new one when the food rotted into rotten flesh)
     */
    public static ItemStack advance(ItemStack stack, float envC, long now) {
        int profile = FoodProfiles.profileOf(stack);
        if (profile < 0) {
            return null;
        }

        String before = stack.getTag() == null ? "" : stack.getTag().toString();

        // Work on a copy so a client-side menu slot never sees a half-updated stack.
        ItemStack working = stack.copy();
        FoodState.scan(working, envC, now, profile);

        // Fully rotten food turns into rotten flesh, silently — the tooltip already warned at step 4.
        if (FoodState.isRotten(working)) {
            return new ItemStack(Items.ROTTEN_FLESH, Math.max(1, working.getCount()));
        }

        String after = working.getTag() == null ? "" : working.getTag().toString();
        return before.equals(after) ? null : working;
    }
}
