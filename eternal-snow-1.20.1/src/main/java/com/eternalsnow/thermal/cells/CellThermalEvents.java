package com.eternalsnow.thermal.cells;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "eternalsnow")
public final class CellThermalEvents {
    private CellThermalEvents() {}
    @SubscribeEvent public static void loaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            long chunk = event.getChunk().getPos().toLong();
            // Forge may notify while constructing a chunk. Only enqueue; never read the
            // chunk or SavedData here. The server thermal tick resolves ready chunks.
            WorldCellThermal.load(level,chunk);
        }
    }
}
