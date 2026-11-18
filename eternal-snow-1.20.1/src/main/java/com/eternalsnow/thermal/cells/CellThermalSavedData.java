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
    // Legacy entries migrate lazily, tile by tile; unvisited/unloaded values remain untouched.
    final Long2ObjectOpenHashMap<Long2DoubleOpenHashMap> chunks = new Long2ObjectOpenHashMap<>();
    final Long2ObjectOpenHashMap<TileTemperatureState> tiles = new Long2ObjectOpenHashMap<>();
    final Long2ObjectOpenHashMap<it.unimi.dsi.fastutil.longs.LongOpenHashSet> tileChunks = new Long2ObjectOpenHashMap<>();
    void putTile(long key,TileTemperatureState tile) {
        tiles.put(key,tile);var pos=net.minecraft.core.BlockPos.of(key);
        long chunk=net.minecraft.world.level.ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4);
        tileChunks.computeIfAbsent(chunk,k->new it.unimi.dsi.fastutil.longs.LongOpenHashSet()).add(key);
        setDirty();
    }
    TileTemperatureState migrateTile(long key,double[] initial) {
        TileTemperatureState existing=tiles.get(key);if(existing!=null)return existing;
        if(initial.length!=512)throw new IllegalArgumentException("Tile size");
        var origin=net.minecraft.core.BlockPos.of(key);
        long chunkKey=net.minecraft.world.level.ChunkPos.asLong(origin.getX()>>4,origin.getZ()>>4);
        var old=chunks.get(chunkKey);double[] values=initial.clone();
        for(int i=0;i<512;i++) {
            long p=origin.offset(i&7,i>>6,(i>>3)&7).asLong();
            double retained=old==null?Double.NaN:old.get(p);
            if(!Double.isNaN(retained))values[i]=retained;
        }
        TileTemperatureState result=new TileTemperatureState(values);putTile(key,result);
        if(old!=null) {
            for(int i=0;i<512;i++)old.remove(origin.offset(i&7,i>>6,(i>>3)&7).asLong());
            if(old.isEmpty())chunks.remove(chunkKey);
        }
        return result;
    }
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
        if (tag.getInt("version") != 1 && tag.getInt("version") != 2) throw new IllegalArgumentException("Unsupported thermal world data");
        ListTag chunks = tag.getList("chunks", Tag.TAG_COMPOUND);
        for (int i=0;i<chunks.size();i++) {
            CompoundTag entry = chunks.getCompound(i);
            CellTemperatureSnapshot snapshot = CellTemperatureSnapshot.load(entry.getCompound("cells"));
            long[] positions = snapshot.positions(); double[] temperatures = snapshot.temperatures();
            Long2DoubleOpenHashMap cells = data.chunk(entry.getLong("chunk"));
            for (int j=0;j<positions.length;j++) cells.put(positions[j], temperatures[j]);
        }
        ListTag tileList=tag.getList("tiles",Tag.TAG_COMPOUND);
        for(int i=0;i<tileList.size();i++) {
            var entry=tileList.getCompound(i);
            data.putTile(entry.getLong("key"),TileTemperatureState.load(entry.getCompound("temperatures")));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        tag.putInt("version", 2);
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
        ListTag tileList=new ListTag();
        for(var entry:tiles.long2ObjectEntrySet()) {
            CompoundTag saved=new CompoundTag();saved.putLong("key",entry.getLongKey());
            saved.put("temperatures",entry.getValue().save());tileList.add(saved);
        }
        tag.put("tiles",tileList);
        return tag;
    }
}
