package com.eternalsnow.food;

import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.state.EternalSnowState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * v2.7 — temperature and spoilage state of one food stack.
 *
 * <p>State uses integer NBT fields. Display temperature is quantised to three degrees;
 * integration retains milli-degrees separately, so small cooling steps are not discarded.
 * Stacks with different thermal histories may have different tags and will not merge until
 * their state agrees. Never trade away thermal accuracy to force unrelated stacks to merge.
 *
 * <p>Temperature follows Newton's law of cooling instead of hard-coded timers, so "colder means
 * faster" falls out of the formula and needs no table:
 * <pre>
 *   T(t) = T_env + (T_item - T_env) * exp(-t / TAU)
 * </pre>
 * One {@link Math#exp} per scan — not a tick handler per item.
 */
public final class FoodState {

    /** Root key on the stack. Kept out of vanilla keys so nothing else touches it. */
    public static final String TAG_ROOT = "eternalsnow_food";
    private static final String TAG_EXACT_TEMP = "tm"; // milli-degrees, prevents rounding stalls
    private static final String TAG_TEMP = "t";     // whole °C, quantised to TEMP_STEP
    private static final String TAG_SPOIL = "s";    // 0..4
    private static final String TAG_PROGRESS = "p"; // 0..999 of the current spoilage step
    private static final String TAG_STAMP = "u";    // game time of the last scan

    /** Frozen at or below this, thawed at or above that — the gap is the hysteresis band. */
    public static final float FROZEN_AT_C = -2.0F;
    public static final float THAWED_AT_C = 3.0F;

    /**
     * Newton's time constant in ticks. Anchored on the owner's requirement "three minutes at -35 °C":
     * from body temperature (+36.6) down to the freeze threshold (-2) that is
     * {@code tau = 180 s / ln(71.6 / 33.6)} = 232.4 s.
     */
    public static final double TAU_TICKS = 232.4D * 20.0D;

    /** Quantisation of the stored temperature: the "2–3 degrees of slack" that keeps stacks merging. */
    public static final int TEMP_STEP = 3;

    public static final int SPOIL_MAX = 4;

    /** Body temperature of a player — the environment for food carried in an inventory. */
    public static final float BODY_C = 36.6F;

    /** How long one spoilage step takes at exactly +20 °C, per food profile. */
    private static final int[] TICKS_PER_STEP_AT_20 = {
            0, 14400, 22150, 25900, 10000, 18000, 144000, 288000
    };

    private FoodState() {
    }

    // ------------------------------------------------------------------ queries

    public static boolean hasState(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTag() && stack.getTag().contains(TAG_ROOT, CompoundTag.TAG_COMPOUND);
    }

    public static int tempC(ItemStack stack, float fallbackC) {
        CompoundTag tag = root(stack);
        return tag == null ? Math.round(fallbackC) : tag.getInt(TAG_TEMP);
    }

    /** True when the stack is frozen: below the lower hysteresis edge. */
    public static boolean isFrozen(ItemStack stack) {
        CompoundTag tag = root(stack);
        return tag != null && tag.getInt(TAG_TEMP) <= FROZEN_AT_C;
    }

    public static int spoil(ItemStack stack) {
        CompoundTag tag = root(stack);
        return tag == null ? 0 : Mth.clamp(tag.getInt(TAG_SPOIL), 0, SPOIL_MAX);
    }

    public static boolean isRotten(ItemStack stack) {
        return spoil(stack) >= SPOIL_MAX;
    }

    /** Spoiled but not yet rotten: still edible, just worse. */
    public static boolean isSpoiled(ItemStack stack) {
        int s = spoil(stack);
        return s >= 2 && s < SPOIL_MAX;
    }

    // ------------------------------------------------------------------ physics

    /**
     * Advances one stack toward the temperature of its surroundings and accrues spoilage.
     *
     * @param envC     temperature of the place the stack is lying in
     * @param stack    the stack, mutated in place when something actually changes
     * @param nowTick  current game time — becomes the new scan stamp
     * @param profile  spoilage speed group of this item
     */
    public static void scan(ItemStack stack, float envC, long nowTick, int profile) {
        ServerSnowSettings settings = EternalSnowState.getServerSettings();

        CompoundTag existing = root(stack);
        int oldTemp = existing == null ? quantise(BODY_C) : existing.getInt(TAG_TEMP);
        int spoil = existing == null ? 0 : Mth.clamp(existing.getInt(TAG_SPOIL), 0, SPOIL_MAX);
        int progress = existing == null ? 0 : Mth.clamp(existing.getInt(TAG_PROGRESS), 0, 999);
        long stamp = existing == null ? nowTick : existing.getLong(TAG_STAMP);

        long elapsed = Math.max(0L, nowTick - stamp);
        // A stack in an unloaded chunk (or in a chest nobody visited for a week) must not integrate
        // an enormous interval in one step — cap it at three time constants, past which the item has
        // already equalised with its surroundings.
        elapsed = Math.min(elapsed, (long) (TAU_TICKS * 3.0D));

        // Old saves migrate from their displayed temperature. New food starts warm, but must
        // receive a stamp even when this first scan has zero elapsed time.
        int exactTemp = existing != null && existing.contains(TAG_EXACT_TEMP)
                ? existing.getInt(TAG_EXACT_TEMP) : (existing == null ? Math.round(BODY_C * 1000) : oldTemp * 1000);
        int newTemp = oldTemp;
        if (settings.enableFoodFreezing && elapsed > 0L) {
            exactTemp = FoodCooling.advanceMilliC(exactTemp, envC,
                    Math.round(elapsed * (envC < exactTemp / 1000.0D
                            ? settings.foodFreezingSpeedPercent : settings.foodThawingSpeedPercent) / 100.0D), TAU_TICKS);
            newTemp = quantise(exactTemp / 1000.0D);
        }

        int remainder = existing == null ? 0 : Mth.clamp(existing.getInt("pr"), 0, 999);
        int newProgress = progress;
        if (settings.enableFoodSpoilage && profile > 0 && elapsed > 0L) {
            long micro = remainder + spoilageAmount(Math.round(elapsed * settings.foodSpoilageSpeedPercent / 100.0D), newTemp, profile);
            newProgress = progress + (int) (micro / 1000L);
            remainder = (int) (micro % 1000L);
            while (newProgress >= 1000 && spoil < SPOIL_MAX) {
                newProgress -= 1000;
                spoil++;
            }
            if (spoil >= SPOIL_MAX) {
                newProgress = 999; // rotten: hold just under the next step, the item turns into rot
            }
        }

        if (existing != null && elapsed == 0L) {
            return;
        }

        CompoundTag root = stack.getOrCreateTag().getCompound(TAG_ROOT);
        root.putInt(TAG_TEMP, newTemp);
        root.putInt(TAG_EXACT_TEMP, exactTemp);
        root.putInt(TAG_SPOIL, spoil);
        root.putInt(TAG_PROGRESS, newProgress);
        root.putInt("pr", remainder);
        root.putLong(TAG_STAMP, nowTick);
        stack.getOrCreateTag().put(TAG_ROOT, root);
    }

    /** Progress in 1/1000 of one spoilage step. */
    private static int spoilageAmount(long elapsedTicks, int tempC, int profile) {
        int ticksPerStep = ticksPerStep(profile);
        if (ticksPerStep <= 0) {
            return 0;
        }
        // Spoilage only happens above freezing; the rate grows with temperature and flattens out,
        // so a cold pantry slows it down and a hot room speeds it up.
        float factor = Mth.clamp(tempC / 20.0F, 0.0F, 3.0F);
        if (factor <= 0.0F) {
            return 0;
        }
        long amount = (elapsedTicks * 1000000L * Math.round(factor * 100.0F)) / (ticksPerStep * 100L);
        return (int) Math.min(amount, Integer.MAX_VALUE / 2);
    }

    private static int ticksPerStep(int profile) {
        return profile >= 0 && profile < TICKS_PER_STEP_AT_20.length ? TICKS_PER_STEP_AT_20[profile] : 0;
    }

    /** Rounds to the storage step and keeps the stack mergeable. */
    private static int quantise(double celsius) {
        return (int) (Math.round(celsius / TEMP_STEP) * TEMP_STEP);
    }

    /** Writes a stack's temperature directly — used by cooking slots, which have a known environment. */
    public static void setTemp(ItemStack stack, float celsius, long nowTick) {
        CompoundTag root = stack.getOrCreateTag().getCompound(TAG_ROOT);
        root.putInt(TAG_TEMP, quantise(celsius));
        root.putInt(TAG_EXACT_TEMP, Math.round(celsius * 1000));
        root.putLong(TAG_STAMP, nowTick);
        stack.getOrCreateTag().put(TAG_ROOT, root);
    }

    /** Clears only the freezing half, leaving spoilage alone (used when eating rot/unfreezing). */
    public static void markThawed(ItemStack stack, float celsius, long nowTick) {
        CompoundTag root = stack.getOrCreateTag().getCompound(TAG_ROOT);
        root.putInt(TAG_TEMP, quantise(Math.max(celsius, THAWED_AT_C + TEMP_STEP)));
        root.putInt(TAG_EXACT_TEMP, Math.round(Math.max(celsius, THAWED_AT_C + TEMP_STEP) * 1000));
        root.putLong(TAG_STAMP, nowTick);
        stack.getOrCreateTag().put(TAG_ROOT, root);
    }

    /** Number of ticks this stack needs to reach the thaw threshold in a place of temperature {@code envC}. */
    public static int ticksToThaw(ItemStack stack, float envC) {
        int temp = tempC(stack, envC);
        if (temp >= THAWED_AT_C) {
            return 0;
        }
        if (envC <= THAWED_AT_C) {
            return Integer.MAX_VALUE; // this place can never thaw anything
        }
        // T(t) = env + (T0 - env) e^(-t/tau)  ->  t = -tau * ln((env - target) / (env - T0))
        double ratio = (envC - THAWED_AT_C) / (double) (envC - temp);
        if (ratio <= 0.0D || ratio >= 1.0D) {
            return ratio <= 0.0D ? Integer.MAX_VALUE : 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.round(-TAU_TICKS * Math.log(ratio)));
    }

    private static CompoundTag root(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_ROOT, CompoundTag.TAG_COMPOUND) ? tag.getCompound(TAG_ROOT) : null;
    }
}
