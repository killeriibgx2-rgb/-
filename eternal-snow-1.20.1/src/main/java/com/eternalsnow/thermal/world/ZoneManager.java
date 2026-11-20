package com.eternalsnow.thermal.world;

import com.eternalsnow.config.ThermalConfig;
import com.eternalsnow.network.BodyStatePacket;
import com.eternalsnow.network.ThermalNetwork;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Owns nearby zones and advances the world and body models on a fixed cadence. */
public final class ZoneManager {
    private static final int PERIOD_TICKS = 10;

    private final ServerLevel level;
    private final List<ZoneSimulation> zones = new ArrayList<>();
    private int tickCounter;

    public ZoneManager(ServerLevel level) {
        this.level = level;
    }

    public ServerLevel level() {
        return level;
    }

    public List<ZoneSimulation> zones() {
        return zones;
    }

    /** Called once per level tick from the Forge event handler. */
    public void tick(AmbientField field) {
        if (++tickCounter % PERIOD_TICKS != 0) return;
        if (!ThermalConfig.enabled()) {
            zones.clear();
            for (ServerPlayer player : level.players()) {
                ThermalNetwork.sendToPlayer(player, BodyStatePacket.disabled());
            }
            return;
        }

        double realSeconds = PERIOD_TICKS * 0.05;
        double speed = ThermalConfig.SPEED_MULTIPLIER.get();
        double modelSeconds = realSeconds * speed;
        double ambientKelvin = field.kelvinAt(level);
        double ambientCelsius = field.celsiusAt(level);

        TemperatureStore store = TemperatureStore.get(level);
        rebuildZones();
        for (ZoneSimulation zone : zones) {
            // capture() replaces the grid, so configuration must be applied after it.
            zone.capture(store, ambientKelvin);
            zone.grid().setSpeedMultiplier(speed);
            zone.step(realSeconds);
            zone.writeBack(store, ambientKelvin);
        }

        for (ServerPlayer player : level.players()) {
            BlockPos bodyPos = BlockPos.containing(player.getX(), player.getY() + 0.9, player.getZ());
            ZoneSimulation zone = zoneContaining(bodyPos);
            double airCelsius = zone == null
                    ? ambientCelsius : zone.airCelsiusAt(bodyPos, ambientCelsius);
            if (ThermalConfig.BODY_ENABLED.get()) {
                PlayerBodySimulator.BodyState body = PlayerBodySimulator.advance(
                        player, airCelsius, modelSeconds);
                ThermalNetwork.sendToPlayer(player,
                        new BodyStatePacket(true, body.coreCelsius(), airCelsius));
            } else {
                ThermalNetwork.sendToPlayer(player, BodyStatePacket.disabled());
            }
        }
    }

    private void rebuildZones() {
        zones.clear();
        int radius = ThermalConfig.ZONE_RADIUS.get();
        Set<Long> origins = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            BlockPos origin = player.blockPosition().offset(-radius, -radius, -radius);
            // Players standing in the same place share one grid, not duplicate work.
            if (!origins.add(origin.asLong())) continue;
            zones.add(new ZoneSimulation(level, player.blockPosition(), radius));
        }
    }

    /** Returns the zone containing a position, or null if it is outside all active zones. */
    public ZoneSimulation zoneContaining(BlockPos pos) {
        for (ZoneSimulation zone : zones) {
            BlockPos origin = zone.origin();
            int side = zone.side();
            if (pos.getX() >= origin.getX() && pos.getX() < origin.getX() + side
                    && pos.getY() >= origin.getY() && pos.getY() < origin.getY() + side
                    && pos.getZ() >= origin.getZ() && pos.getZ() < origin.getZ() + side) {
                return zone;
            }
        }
        return null;
    }

    public double airCelsiusAt(BlockPos pos, double ambientCelsius) {
        ZoneSimulation zone = zoneContaining(pos);
        return zone == null ? ambientCelsius : zone.airCelsiusAt(pos, ambientCelsius);
    }
}
