package com.eternalsnow.thermal.world;

import com.eternalsnow.config.ThermalConfig;
import com.eternalsnow.thermal.engine.Ambient;
import com.eternalsnow.thermal.engine.Constants;
import com.eternalsnow.thermal.engine.Devices;
import com.eternalsnow.thermal.engine.ExternalLink;
import com.eternalsnow.thermal.engine.InfiltrationPath;
import com.eternalsnow.thermal.engine.Material;
import com.eternalsnow.thermal.engine.Materials;
import com.eternalsnow.thermal.engine.ThermalGrid;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** A fresh, local thermal grid around one active player. */
public final class ZoneSimulation {
    private final ServerLevel level;
    private final BlockPos origin;
    private final int radius;
    private ThermalGrid grid;
    private final List<HeatSourceBinding> sources = new ArrayList<>();

    private record HeatSourceBinding(int cellIndex, double watts) {
    }

    public ZoneSimulation(ServerLevel level, BlockPos centre, int radius) {
        if (radius < 1) throw new IllegalArgumentException("radius must be positive");
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

    /** Rebuilds materials and temperatures from the Minecraft world plus sparse saved state. */
    public void capture(TemperatureStore store, double ambientKelvin) {
        int side = side();
        grid = new ThermalGrid(side, side, side);
        sources.clear();
        boolean[] airCells = new boolean[grid.cellCount()];
        boolean[] openDoorCells = new boolean[grid.cellCount()];
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int y = 0; y < side; y++) {
            for (int z = 0; z < side; z++) {
                for (int x = 0; x < side; x++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    BlockState state = level.getBlockState(cursor);
                    Material material = MaterialMapper.of(state, level, cursor);
                    int index = grid.index(x, y, z);
                    grid.set(x, y, z, material, store.temperatureAt(cursor, ambientKelvin));
                    airCells[index] = material == Materials.AIR;
                    openDoorCells[index] = MaterialMapper.isOpenDoor(state);

                    double watts = sourceWatts(state);
                    if (watts > 0) sources.add(new HeatSourceBinding(index, watts));
                }
            }
        }

        Ambient ambient = Ambient.constant(ambientKelvin);
        attachBoundaries(ambient);
        attachOpenDoorLeaks(ambient, airCells, openDoorCells);
    }

    private double sourceWatts(BlockState state) {
        if ((state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
                && state.getValue(CampfireBlock.LIT)) {
            return state.is(Blocks.SOUL_CAMPFIRE)
                    ? Devices.SOUL_CAMPFIRE_WATTS : Devices.CAMPFIRE_WATTS;
        }
        if (state.is(Blocks.FURNACE) && state.getValue(BlockStateProperties.LIT)) {
            // The vanilla furnace is untouched: it simply radiates while lit.
            return Devices.FURNACE_WATTS;
        }
        return 0;
    }

    /** The box edge sees the local material half-cell plus an outside film. */
    private void attachBoundaries(Ambient ambient) {
        int side = side();
        for (int y = 0; y < side; y++) {
            for (int z = 0; z < side; z++) {
                for (int x = 0; x < side; x++) {
                    if (x != 0 && y != 0 && z != 0
                            && x != side - 1 && y != side - 1 && z != side - 1) continue;
                    int index = grid.index(x, y, z);
                    Material material = grid.cell(index).material();
                    double conductance = material == Materials.AIR
                            ? Constants.FREE_CONVECTION
                            : ExternalLink.outsideSurfaceConductance(material, 1.0, 0.5);
                    grid.addExternalLink(new ExternalLink(index, ambient, 1.0 / conductance));
                }
            }
        }
    }

    private void attachOpenDoorLeaks(Ambient ambient,
                                    boolean[] airCells, boolean[] openDoorCells) {
        if (!ThermalConfig.DOOR_INFILTRATION.get()) return;
        for (DoorLeakDetector.Leak leak : DoorLeakDetector.detect(
                side(), side(), side(), airCells, openDoorCells)) {
            grid.addInfiltration(new InfiltrationPath(leak.insideCellIndex(), leak.areaM2(),
                    leak.pressureHeadM(), ambient));
        }
    }

    /** Steps once, with device watts applied for the full model interval. */
    public int step(double realSeconds) {
        for (HeatSourceBinding source : sources) {
            grid.cell(source.cellIndex()).addPower(source.watts());
        }
        return grid.step(realSeconds);
    }

    /** Saves only cells that differ meaningfully from the outside field. */
    public void writeBack(TemperatureStore store, double ambientKelvin) {
        int side = side();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y < side; y++) {
            for (int z = 0; z < side; z++) {
                for (int x = 0; x < side; x++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    store.setTemperature(cursor, grid.cell(x, y, z).temperatureKelvin(), ambientKelvin);
                }
            }
        }
    }

    public double centreCelsius() {
        return grid.cell(radius, radius, radius).temperatureCelsius();
    }

    /** Air temperature at the body/head cell, with a safe fallback for solids. */
    public double airCelsiusAt(BlockPos worldPos, double ambientCelsius) {
        int x = worldPos.getX() - origin.getX();
        int y = worldPos.getY() - origin.getY();
        int z = worldPos.getZ() - origin.getZ();
        if (x < 0 || x >= side() || y < 0 || y >= side() || z < 0 || z >= side()) {
            return ambientCelsius;
        }
        return grid.cell(x, y, z).temperatureCelsius();
    }
}
