package com.eternalsnow.thermal.cells;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Dimension-local storage, partitioned by chunk. No time stamps: unloaded chunks are paused. */
public final class CellThermalSavedData extends SavedData {
    final Long2ObjectOpenHashMap<Long2DoubleOpenHashMap> chunks = new Long2ObjectOpenHashMap<>();
    public static CellThermalSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(CellThermalSavedData::load,
                CellThermalSavedData::new, "eternalsnow_cell_heat_v1");
    }
    Long2DoubleOpenHashMap chunk(long key) {
        return chunks.computeIfAbsent(key, k -> {
            Long2DoubleOpenHashMap result = new Long2DoubleOpenHashMap();
            result.defaultReturnValue(Double.NaN);
            return result;
        });
    }
    public static CellThermalSavedData load(CompoundTag tag) {
        CellThermalSavedData data = new CellThermalSavedData();
        if (tag.getInt("version") != 1) throw new IllegalArgumentException("Unsupported thermal world data");
        ListTag chunks = tag.getList("chunks", Tag.TAG_COMPOUND);
        for (int i=0;i<chunks.size();i++) {
            CompoundTag entry = chunks.getCompound(i);
            CellTemperatureSnapshot snapshot = CellTemperatureSnapshot.load(entry.getCompound("cells"));
            long[] positions = snapshot.positions(); double[] temperatures = snapshot.temperatures();
            Long2DoubleOpenHashMap cells = data.chunk(entry.getLong("chunk"));
            for (int j=0;j<positions.length;j++) cells.put(positions[j], temperatures[j]);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        tag.putInt("version", 1);
        ListTag list = new ListTag();
        for (var entry : chunks.long2ObjectEntrySet()) {
            var map = entry.getValue();
            if (map.isEmpty()) continue;
            long[] keys = map.keySet().toLongArray(); double[] temps = new double[keys.length];
            for (int i=0;i<keys.length;i++) temps[i]=map.get(keys[i]);
            CompoundTag saved = new CompoundTag();
            saved.putLong("chunk",entry.getLongKey());
            saved.put("cells",new CellTemperatureSnapshot(keys,temps).save());
            list.add(saved);
        }
        tag.put("chunks",list);
        return tag;
    }
}
