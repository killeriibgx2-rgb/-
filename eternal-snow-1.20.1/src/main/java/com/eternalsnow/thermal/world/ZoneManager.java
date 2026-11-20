package com.eternalsnow.thermal.world;

import com.eternalsnow.config.ThermalConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Owns the simulated zones for one level and advances them.
 *
 * <p>Timing: the engine runs every 10 ticks (twice a second) rather than every
 * tick. A 0.5 s model step costs the same as 10 steps of 0.05 s — the physics
 * does not care — and it keeps the server's tick budget intact. Calling it more
 * often only makes the numbers smoother, not more correct.
 */
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
        if (!ThermalConfig.enabled()) return;
        if (++tickCounter % PERIOD_TICKS != 0) return;

        double realSeconds = PERIOD_TICKS * 0.05;
        double speed = ThermalConfig.SPEED_MULTIPLIER.get();
        double ambientKelvin = field.kelvinAt(level);

        TemperatureStore store = TemperatureStore.get(level);
        rebuildZones(ambientKelvin);

        for (ZoneSimulation zone : zones) {
            zone.grid().setSpeedMultiplier(speed);
            zone.capture(store, ambientKelvin);
            zone.step(realSeconds);
            zone.writeBack(store);
        }
    }

    private void rebuildZones(double ambientKelvin) {
        zones.clear();
        int radius = ThermalConfig.ZONE_RADIUS.get();
        for (ServerPlayer player : level.players()) {
            BlockPos centre = player.blockPosition();
            ZoneSimulation zone = new ZoneSimulation(level, centre, radius);
            zone.capture(TemperatureStore.get(level), ambientKelvin);
            zones.add(zone);
        }
    }

    /** Nearest zone to a position, or null when the player has no zone yet. */
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

    /** Air temperature at a position, falling back to ambient outside any zone. */
    public double airCelsiusAt(BlockPos pos, double ambientCelsius) {
        ZoneSimulation zone = zoneContaining(pos);
        if (zone == null) return ambientCelsius;
        BlockPos origin = zone.origin();
        return zone.grid().cell(
                pos.getX() - origin.getX(),
                pos.getY() - origin.getY(),
                pos.getZ() - origin.getZ()).temperatureCelsius();
    }
}
