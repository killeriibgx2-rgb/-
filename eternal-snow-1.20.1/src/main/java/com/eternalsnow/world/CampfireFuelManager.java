package com.eternalsnow.world;

import com.eternalsnow.item.WoodenFirestarterItem;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.thermal.ThermalEngine;
import com.eternalsnow.weather.WeatherEventType;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Stick-Fueled Campfire System:
 * - Placed unlit (0 / 30 sticks).
 * - 1 stick = 10s (200 ticks); max 30 sticks = 5:00 (6,000 ticks).
 * - Right-click / Shift+Right-click with Sticks to load fuel; light with Flint & Steel / Fire Charge / Wooden Fire-Starter.
 * - Exposed to open Gale / Blizzard without shelter -> hard to ignite (~80% spark blowout) and drains rapidly until snuffed!
 */
public final class CampfireFuelManager {
    public static final int MAX_STICKS = 30;
    public static final int TICKS_PER_STICK = 200; // 10 seconds
    public static final int MAX_FUEL_TICKS = MAX_STICKS * TICKS_PER_STICK; // 6,000 ticks = 5:00
    private static final int EMBER_GRACE_TICKS = 400; // 20 seconds hot embers after burning out

    private CampfireFuelManager() {
    }

    public static void onCampfirePlaced(ServerLevel level, BlockPos pos, BlockState placedState) {
        if (!(placedState.getBlock() instanceof CampfireBlock)) {
            return;
        }
        EternalSnowSavedData data = EternalSnowSavedData.get(level.getServer());
        long key = pos.asLong();
        data.getCampfireFuelMap().put(key, 0);
        data.getCampfireEmberMap().remove(key);
        data.setDirty();

        if (placedState.getValue(CampfireBlock.LIT)) {
            level.setBlock(pos, placedState.setValue(CampfireBlock.LIT, false), Block.UPDATE_ALL);
            ThermalEngine.onHeatSourceChanged(level, pos, false);
        }
    }

    public static void onCampfireBroken(ServerLevel level, BlockPos pos) {
        EternalSnowSavedData data = EternalSnowSavedData.get(level.getServer());
        long key = pos.asLong();
        if (data.getCampfireFuelMap().containsKey(key) || data.getCampfireEmberMap().containsKey(key)) {
            data.getCampfireFuelMap().remove(key);
            data.getCampfireEmberMap().remove(key);
            data.setDirty();
        }
    }

    public static int getFuelTicks(ServerLevel level, BlockPos pos, BlockState state) {
        EternalSnowSavedData data = EternalSnowSavedData.get(level.getServer());
        Long2IntOpenHashMap map = data.getCampfireFuelMap();
        long key = pos.asLong();
        if (!map.containsKey(key)) {
            int initial = (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT))
                    ? (12 * TICKS_PER_STICK) : 0;
            map.put(key, initial);
            data.setDirty();
            return initial;
        }
        return map.get(key);
    }

    public static boolean isExposedToStormGale(ServerLevel level, BlockPos pos) {
        WeatherEventType ev = EternalSnowState.getActiveWeatherEvent();
        if (ev != WeatherEventType.GALE && ev != WeatherEventType.BLIZZARD) {
            return false;
        }
        if (!level.canSeeSky(pos.above())) {
            return false; // Roof or overhang protects the campfire!
        }
        // Check if upwind block (1..2 steps against wind direction) shields the fire from the storm!
        float wx = EternalSnowState.getWindDirX();
        float wz = EternalSnowState.getWindDirZ();
        for (int step = 1; step <= 2; step++) {
            BlockPos upwind = BlockPos.containing(pos.getX() + 0.5D - wx * step, pos.getY() + 0.5D, pos.getZ() + 0.5D - wz * step);
            BlockState st = level.getBlockState(upwind);
            if (!st.getCollisionShape(level, upwind).isEmpty()) {
                return false; // Windbreak wall protects the campfire!
            }
        }
        return true;
    }

    public static InteractionResult handleCampfireInteract(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            ServerPlayer player,
            InteractionHand hand,
            ItemStack held
    ) {
        if (!(state.getBlock() instanceof CampfireBlock)) {
            return InteractionResult.PASS;
        }
        if (state.getValue(CampfireBlock.WATERLOGGED)) {
            return InteractionResult.PASS;
        }

        EternalSnowSavedData data = EternalSnowSavedData.get(level.getServer());
        Long2IntOpenHashMap fuelMap = data.getCampfireFuelMap();
        Long2IntOpenHashMap emberMap = data.getCampfireEmberMap();
        long key = pos.asLong();
        int curTicks = getFuelTicks(level, pos, state);
        boolean isLit = state.getValue(CampfireBlock.LIT);

        // 1. Feeding Sticks (ПКМ = +1 stick, Shift+ПКМ = fill up to 30 sticks = 5:00!)
        if (held.is(Items.STICK)) {
            if (curTicks >= MAX_FUEL_TICKS) {
                player.displayClientMessage(Component.translatable("eternalsnow.msg.campfire_full"), true);
                return InteractionResult.SUCCESS;
            }
            int roomSticks = Math.max(1, (MAX_FUEL_TICKS - curTicks + TICKS_PER_STICK - 1) / TICKS_PER_STICK);
            int toAdd = player.isShiftKeyDown() ? Math.min(held.getCount(), roomSticks) : 1;
            int newTicks = Math.min(MAX_FUEL_TICKS, curTicks + toAdd * TICKS_PER_STICK);
            fuelMap.put(key, newTicks);

            if (!player.getAbilities().instabuild) {
                held.shrink(toAdd);
            }
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8F, 1.15F);

            // Auto-reignite from glowing embers if refueled within 20s of burning out!
            if (!isLit && emberMap.get(key) > 0 && !isExposedToStormGale(level, pos)) {
                emberMap.remove(key);
                level.setBlock(pos, state.setValue(CampfireBlock.LIT, true), Block.UPDATE_ALL);
                ThermalEngine.onHeatSourceChanged(level, pos, true);
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.65F, 1.2F);
            }
            data.setDirty();
            return InteractionResult.SUCCESS;
        }

        // 2. Igniting an unlit campfire with an instant lighter (Flint & Steel / Fire Charge)
        // Note: WoodenFirestarterItem uses its 10-second hold mechanic and calls tryIgniteFromFirestarterSuccess!
        if (!isLit && WoodenFirestarterItem.isInstantIgniter(held)) {
            if (curTicks <= 0) {
                player.displayClientMessage(Component.translatable("eternalsnow.msg.campfire_no_sticks"), true);
                return InteractionResult.SUCCESS;
            }
            boolean stormExposed = isExposedToStormGale(level, pos);
            WoodenFirestarterItem.tryConsumeInstantIgniter(level, pos, player, hand, held);
            if (stormExposed && level.random.nextFloat() < 0.82F) {
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.85F, 1.3F);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5D, pos.getY() + 0.4D, pos.getZ() + 0.5D, 8, 0.2D, 0.1D, 0.2D, 0.04D);
                player.displayClientMessage(Component.translatable("eternalsnow.msg.campfire_wind_blocked"), true);
                return InteractionResult.SUCCESS;
            }

            emberMap.remove(key);
            level.setBlock(pos, state.setValue(CampfireBlock.LIT, true), Block.UPDATE_ALL);
            ThermalEngine.onHeatSourceChanged(level, pos, true);
            data.setDirty();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public static void tryIgniteFromFirestarterSuccess(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        if (!(state.getBlock() instanceof CampfireBlock) || state.getValue(CampfireBlock.LIT) || state.getValue(CampfireBlock.WATERLOGGED)) {
            return;
        }
        if (getFuelTicks(level, pos, state) <= 0) {
            player.displayClientMessage(Component.translatable("eternalsnow.msg.campfire_no_sticks"), true);
            return;
        }
        if (isExposedToStormGale(level, pos) && level.random.nextFloat() < 0.82F) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.85F, 1.3F);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5D, pos.getY() + 0.4D, pos.getZ() + 0.5D, 8, 0.2D, 0.1D, 0.2D, 0.04D);
            player.displayClientMessage(Component.translatable("eternalsnow.msg.campfire_wind_blocked"), true);
            return;
        }
        EternalSnowSavedData data = EternalSnowSavedData.get(level.getServer());
        data.getCampfireEmberMap().remove(pos.asLong());
        level.setBlock(pos, state.setValue(CampfireBlock.LIT, true), Block.UPDATE_ALL);
        ThermalEngine.onHeatSourceChanged(level, pos, true);
        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 0.95F);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.75F, 1.1F);
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5D, pos.getY() + 0.45D, pos.getZ() + 0.5D, 12, 0.2D, 0.15D, 0.2D, 0.03D);
        data.setDirty();
    }

    /**
     * Ticked once every 10 server ticks (0.5s) on Overworld to drain active stick-fueled campfires with zero lag!
     */
    public static void tickCampfires(ServerLevel level) {
        if (level.getGameTime() % 10L != 0L) {
            return;
        }
        EternalSnowSavedData data = EternalSnowSavedData.get(level.getServer());
        Long2IntOpenHashMap fuelMap = data.getCampfireFuelMap();
        Long2IntOpenHashMap emberMap = data.getCampfireEmberMap();

        if (!emberMap.isEmpty()) {
            LongArrayList expiredEmbers = new LongArrayList();
            for (Long2IntMap.Entry e : emberMap.long2IntEntrySet()) {
                int rem = e.getIntValue() - 10;
                if (rem <= 0) {
                    expiredEmbers.add(e.getLongKey());
                } else {
                    e.setValue(rem);
                    BlockPos p = BlockPos.of(e.getLongKey());
                    if (level.getChunkSource().getChunkNow(p.getX() >> 4, p.getZ() >> 4) != null) {
                        level.sendParticles(ParticleTypes.SMOKE, p.getX() + 0.5D, p.getY() + 0.35D, p.getZ() + 0.5D, 2, 0.12D, 0.05D, 0.12D, 0.005D);
                    }
                }
            }
            for (int i = 0; i < expiredEmbers.size(); i++) {
                emberMap.remove(expiredEmbers.getLong(i));
            }
        }

        if (fuelMap.isEmpty()) {
            return;
        }

        LongArrayList invalidKeys = new LongArrayList();
        boolean dirty = false;

        for (Long2IntMap.Entry entry : fuelMap.long2IntEntrySet()) {
            long key = entry.getLongKey();
            int bx = BlockPos.getX(key);
            int bz = BlockPos.getZ(key);
            LevelChunk chunk = level.getChunkSource().getChunkNow(bx >> 4, bz >> 4);
            if (chunk == null) {
                continue; // Unloaded chunk pauses cleanly
            }
            BlockPos pos = BlockPos.of(key);
            BlockState st = chunk.getBlockState(pos);
            if (!(st.getBlock() instanceof CampfireBlock)) {
                invalidKeys.add(key);
                dirty = true;
                continue;
            }
            if (!st.getValue(CampfireBlock.LIT)) {
                continue;
            }

            boolean stormExposed = isExposedToStormGale(level, pos);
            int drain = stormExposed ? 75 : 10; // Normal = 10 ticks per 0.5s; Open storm = 7.5x faster + blowout!
            int nextFuel = Math.max(0, entry.getIntValue() - drain);
            entry.setValue(nextFuel);
            dirty = true;

            // In an open Blizzard, exposed campfires quickly snuff out from howling gusts!
            boolean blownOutByBlizzard = stormExposed
                    && EternalSnowState.getActiveWeatherEvent() == WeatherEventType.BLIZZARD
                    && level.random.nextFloat() < 0.18F;

            if (nextFuel <= 0 || blownOutByBlizzard) {
                level.setBlock(pos, st.setValue(CampfireBlock.LIT, false), Block.UPDATE_ALL);
                ThermalEngine.onHeatSourceChanged(level, pos, false);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.05F);
                if (!stormExposed && nextFuel <= 0) {
                    emberMap.put(key, EMBER_GRACE_TICKS);
                }
            }
        }

        for (int i = 0; i < invalidKeys.size(); i++) {
            fuelMap.remove(invalidKeys.getLong(i));
        }
        if (dirty) {
            data.setDirty();
        }
    }

    public static void saveToTag(CompoundTag tag, Long2IntOpenHashMap fuelMap) {
        long[] keys = new long[fuelMap.size()];
        int[] vals = new int[fuelMap.size()];
        int idx = 0;
        for (Long2IntMap.Entry e : fuelMap.long2IntEntrySet()) {
            keys[idx] = e.getLongKey();
            vals[idx] = Mth.clamp(e.getIntValue(), 0, MAX_FUEL_TICKS);
            idx++;
        }
        tag.putLongArray("CampfirePosKeys", keys);
        tag.putIntArray("CampfireFuelVals", vals);
    }

    public static void loadFromTag(CompoundTag tag, Long2IntOpenHashMap fuelMap) {
        fuelMap.clear();
        if (tag.contains("CampfirePosKeys") && tag.contains("CampfireFuelVals")) {
            long[] keys = tag.getLongArray("CampfirePosKeys");
            int[] vals = tag.getIntArray("CampfireFuelVals");
            int n = Math.min(keys.length, vals.length);
            for (int i = 0; i < n; i++) {
                fuelMap.put(keys[i], Mth.clamp(vals[i], 0, MAX_FUEL_TICKS));
            }
        }
    }
}
