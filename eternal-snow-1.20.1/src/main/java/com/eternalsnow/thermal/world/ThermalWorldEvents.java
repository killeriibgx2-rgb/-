package com.eternalsnow.thermal.world;

import com.eternalsnow.EternalSnowMod;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Wires the engine into the server tick and keeps one manager per level. */
@Mod.EventBusSubscriber(modid = EternalSnowMod.MOD_ID)
public final class ThermalWorldEvents {
    private static final Map<ServerLevel, ZoneManager> MANAGERS = new HashMap<>();
    private static final AmbientField FIELD = new AmbientField();

    private ThermalWorldEvents() {
    }

    public static ZoneManager manager(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level, ZoneManager::new);
    }

    public static AmbientField ambientField() {
        return FIELD;
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;
        manager(level).tick(FIELD);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MANAGERS.clear();
    }
}
