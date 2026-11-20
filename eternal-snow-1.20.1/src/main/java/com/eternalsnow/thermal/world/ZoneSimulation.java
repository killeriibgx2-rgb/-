package com.eternalsnow.thermal.world;

import com.eternalsnow.thermal.engine.Constants;
import com.eternalsnow.thermal.engine.ExternalLink;
import com.eternalsnow.thermal.engine.Infiltration;
import com.eternalsnow.thermal.engine.InfiltrationPath;
import com.eternalsnow.thermal.engine.Material;
import com.eternalsnow.thermal.engine.Materials;
import com.eternalsnow.thermal.engine.ThermalGrid;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A cube of world simulated honestly by the engine.
 *
 * <p>Built fresh from the world, stepped, and written back. There is no stored
 * cell state in this class: the world is the source of truth for materials and
 * for which block is where, and only the temperature travels through the grid.
 *
 * <p>Temperature persistence: the grid is seeded from the block temperatures
 * that the world adapter stores per block position, so a frozen zone resumes
 * exactly where it stopped.
 */
public final class ZoneSimulation {
    private final ServerLevel level;
    private final BlockPos origin;
    private final int radius;
    private ThermalGrid grid;
    private final List<HeatSourceBinding> sources = new ArrayList<>();

    private record HeatSourceBinding(int cellIndex, double watts) {
    }

    public ZoneSimulation(ServerLevel level, BlockPos centre, int radius) {
        this.level = level;
        this.origin = centre.offset(-radius, -radius, -radius);
        this.radius = radius;
        this.grid = new ThermalGrid(2 * radius + 1, 2 * radius + 1, 2 * radius + 1);
    }

    public BlockPos origin() {
        return origin;
    }

    public int side() {
        return 2 * radius + 1;
    }

    public ThermalGrid grid() {
        return grid;
    }

    /** Rebuilds the grid from the world, keeping temperatures where possible. */
    public void capture(TemperatureStore store, double ambientKelvin) {
        int side = side();
        grid = new ThermalGrid(side, side, side);
        sources.clear();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int y = 0; y < side; y++) {
            for (int z = 0; z < side; z++) {
                for (int x = 0; x < side; x++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    BlockState state = level.getBlockState(cursor);
                    Material material = MaterialMapper.of(state, level, cursor);
                    double temperature = store.temperatureAt(cursor, ambientKelvin);
                    grid.set(x, y, z, material, temperature);

                    double watts = sourceWatts(state);
                    if (watts > 0) {
                        sources.add(new HeatSourceBinding(grid.index(x, y, z), watts));
                    }
                }
            }
        }
        attachBoundaries(ambientKelvin, store);
    }

    private double sourceWatts(BlockState state) {
        if (state.is(Blocks.CAMPFIRE) && state.getValue(CampfireBlock.LIT)) {
            return com.eternalsnow.thermal.engine.Devices.CAMPFIRE_WATTS;
        }
        if (state.is(Blocks.SOUL_CAMPFIRE) && state.getValue(CampfireBlock.LIT)) {
            return com.eternalsnow.thermal.engine.Devices.SOUL_CAMPFIRE_WATTS;
        }
        if (state.is(Blocks.FURNACE) && state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)) {
            // Vanilla furnace: heat only while it is actually smelting.
            return com.eternalsnow.thermal.engine.Devices.FURNACE_WATTS;
        }
        return 0;
    }

    /**
     * Faces of the zone that touch the outside world become external links to
     * the ambient reservoir, so heat can leave the box instead of piling up on
     * the border.
     */
    private void attachBoundaries(double ambientKelvin, TemperatureStore store) {
        int side = side();
        double conductance = ExternalLink.wallConductance(Materials.STONE, 1.0, 1.0);
        for (int y = 0; y < side; y++) {
            for (int z = 0; z < side; z++) {
                for (int x = 0; x < side; x++) {
                    boolean border = x == 0 || y == 0 || z == 0
                            || x == side - 1 || y == side - 1 || z == side - 1;
                    if (!border) continue;
                    int index = grid.index(x, y, z);
                    grid.addExternalLink(new ExternalLink(index,
                            com.eternalsnow.thermal.engine.Ambient.constant(ambientKelvin),
                            1.0 / conductance));
                }
            }
        }
    }

    /** Steps the model once by {@code realSeconds}; returns sub-steps taken. */
    public int step(double realSeconds) {
        for (HeatSourceBinding source : sources) {
            grid.cell(source.cellIndex()).addPower(source.watts());
        }
        return grid.step(realSeconds);
    }

    /** Writes temperatures back to the store, excluding the outer shell. */
    public void writeBack(TemperatureStore store) {
        int side = side();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y < side; y++) {
            for (int z = 0; z < side; z++) {
                for (int x = 0; x < side; x++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    store.setTemperature(cursor, grid.cell(x, y, z).temperatureKelvin());
                }
            }
        }
    }

    /** Air temperature at the zone centre, °C, for the HUD and the panel. */
    public double centreCelsius() {
        int c = radius;
        return grid.cell(c, c, c).temperatureCelsius();
    }

    /** Mean air temperature over the zone, °C. */
    public double meanAirCelsius() {
        double sum = 0;
        int count = 0;
        for (int i = 0; i < grid.cellCount(); i++) {
            if (grid.cell(i).material() != Materials.AIR) continue;
            sum += grid.cell(i).temperatureCelsius();
            count++;
        }
        return count == 0 ? centreCelsius() : sum / count;
    }
}
