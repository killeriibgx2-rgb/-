package com.eternalsnow.thermal.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * A dense box of cells plus the faces between them.
 *
 * <p>Stage 1 scope: conduction and convection inside the box, explicit
 * sub-stepping with a stability limit derived from the fastest path in the
 * grid. The solver is deliberately boring and provably conservative before any
 * cleverness (implicit stepping, LOD, radiative exchange) is layered on — those
 * all have to reproduce this baseline, and a baseline you cannot trust is worse
 * than none.
 *
 * <p>Energy rule: the sum of {@link ThermalCell#storedEnergy()} over all cells
 * changes only by external power added through {@link ThermalCell#addPower}.
 * No channel may quietly gain or lose joules.
 */
public final class ThermalGrid {
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final ThermalCell[] cells;
    private final boolean[] airMixingBlockedCells;
    private final List<ExternalLink> externalLinks = new ArrayList<>();
    private final List<InfiltrationPath> infiltrations = new ArrayList<>();
    private double speedMultiplier = 1.0;

    public ThermalGrid(int sizeX, int sizeY, int sizeZ) {
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            throw new IllegalArgumentException("grid dimensions must be positive");
        }
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.cells = new ThermalCell[sizeX * sizeY * sizeZ];
        this.airMixingBlockedCells = new boolean[this.cells.length];
    }

    public int sizeX() {
        return sizeX;
    }

    public int sizeY() {
        return sizeY;
    }

    public int sizeZ() {
        return sizeZ;
    }

    public int cellCount() {
        return cells.length;
    }

    public int index(int x, int y, int z) {
        if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ) {
            throw new IndexOutOfBoundsException("(" + x + "," + y + "," + z + ") outside grid");
        }
        return (y * sizeZ + z) * sizeX + x;
    }

    public ThermalCell cell(int index) {
        ThermalCell cell = cells[index];
        if (cell == null) throw new IllegalStateException("cell " + index + " is not set");
        return cell;
    }

    public ThermalCell cell(int x, int y, int z) {
        return cell(index(x, y, z));
    }

    public void set(int x, int y, int z, Material material, double temperatureKelvin) {
        cells[index(x, y, z)] = new ThermalCell(material, temperatureKelvin);
    }

    /** Total heat in the grid relative to absolute zero, J. */
    public double totalEnergy() {
        double sum = 0;
        for (ThermalCell cell : cells) {
            if (cell != null) sum += cell.storedEnergy();
        }
        return sum;
    }

    /** Mean temperature of the cells that exist, K. */
    public double meanTemperatureKelvin() {
        double sum = 0;
        int count = 0;
        for (ThermalCell cell : cells) {
            if (cell == null) continue;
            sum += cell.temperatureKelvin();
            count++;
        }
        if (count == 0) throw new IllegalStateException("grid is empty");
        return sum / count;
    }

    public double meanTemperatureCelsius() {
        return Constants.toCelsius(meanTemperatureKelvin());
    }

    /**
     * All faces between existing neighbouring cells. Rebuilt per step call,
     * which is fine for stage 1; the packed representation comes with the
     * benchmark stage.
     */
    private List<Face> faces() {
        List<Face> faces = new ArrayList<>();
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    int a = index(x, y, z);
                    if (cells[a] == null) continue;
                    if (x + 1 < sizeX) addFace(faces, a, index(x + 1, y, z), false);
                    if (y + 1 < sizeY) addFace(faces, a, index(x, y + 1, z), true);
                    if (z + 1 < sizeZ) addFace(faces, a, index(x, y, z + 1), false);
                }
            }
        }
        return faces;
    }

    private void addFace(List<Face> into, int a, int b, boolean vertical) {
        if (cells[b] == null) return;
        Material ma = cells[a].material();
        Material mb = cells[b].material();
        boolean airA = ma == Materials.AIR;
        boolean airB = mb == Materials.AIR;
        if (airA && airB && (airMixingBlockedCells[a] || airMixingBlockedCells[b])) {
            // Exterior door openings use their explicit infiltration path; do
            // not count the same opening again as ordinary room-air mixing.
            return;
        }
        if (airA && airB && vertical) {
            // Faces are added in +Y order, so a is below b. The conductance is
            // recalculated during each sub-step as the buoyancy gradient changes.
            into.add(new Face(a, b, Transfer.maximumVerticalAirMixingConductance(), true));
            return;
        }
        if (airA && airB) {
            into.add(new Face(a, b, Transfer.horizontalAirMixingConductance(), false));
            return;
        }
        if (airA || airB) {
            Material solid = airA ? mb : ma;
            into.add(new Face(a, b, Transfer.airSolidConductance(solid), false));
            return;
        }
        into.add(new Face(a, b, Transfer.conductionConductance(ma, mb), false));
    }

    /** Conductance, W/K, between two adjacent cells. */
    public double faceConductance(int x, int y, int z, Axis axis) {
        int dx = axis == Axis.X ? 1 : 0;
        int dy = axis == Axis.Y ? 1 : 0;
        int dz = axis == Axis.Z ? 1 : 0;
        int a = index(x, y, z);
        int b = index(x + dx, y + dy, z + dz);
        ThermalCell cellA = cell(a);
        ThermalCell cellB = cell(b);
        Material ma = cellA.material();
        Material mb = cellB.material();
        boolean airA = ma == Materials.AIR;
        boolean airB = mb == Materials.AIR;
        if (airA && airB) {
            if (airMixingBlockedCells[a] || airMixingBlockedCells[b]) return 0.0;
            return axis == Axis.Y
                    ? Transfer.verticalAirMixingConductance(
                            cellA.temperatureKelvin(), cellB.temperatureKelvin())
                    : Transfer.horizontalAirMixingConductance();
        }
        if (airA || airB) {
            return Transfer.airSolidConductance(airA ? mb : ma);
        }
        return Transfer.conductionConductance(ma, mb);
    }

    public enum Axis { X, Y, Z }

    private record Face(int a, int b, double maximumConductance, boolean buoyantVerticalAir) {
        double conductance(ThermalCell[] cells) {
            if (!buoyantVerticalAir) return maximumConductance;
            return Transfer.verticalAirMixingConductance(
                    cells[a].temperatureKelvin(), cells[b].temperatureKelvin());
        }
    }

    /**
     * Advances the simulation by {@code seconds} of model time.
     *
     * <p>Neighbour-to-neighbour conduction/convection uses explicit Euler with
     * sub-stepping capped at {@code 0.9·C/G}. Reservoir links are integrated
     * analytically as exponential exchange, so an open door cannot force the
     * entire 15,000-cell zone into thousands of tiny global steps.
     *
     * <p>Power is constant over this whole call and is consumed once at the
     * end. This matters when internal transfer requires several sub-steps: a
     * fire rated at 5 kW must still deliver 5 kW for every model second.
     *
     * @return number of internal-transfer sub-steps taken
     */
    public int step(double seconds) {
        if (!(seconds > 0) || !Double.isFinite(seconds)) {
            throw new IllegalArgumentException("seconds must be finite and > 0");
        }
        if (speedMultiplier == 0) {
            clearPower();
            return 0;
        }

        // Multiplying model time by N is equivalent to scaling all fluxes by N.
        double dt = seconds * speedMultiplier;
        List<Face> faces = faces();
        double stable = stableStep(faces);
        int substeps = Math.max(1, (int) Math.ceil(dt / stable));
        double sub = dt / substeps;
        double[] delta = new double[cells.length];

        for (int iteration = 0; iteration < substeps; iteration++) {
            java.util.Arrays.fill(delta, 0.0);
            for (int i = 0; i < cells.length; i++) {
                ThermalCell cell = cells[i];
                if (cell != null) {
                    delta[i] += cell.externalPowerWatts() * sub;
                }
            }

            // Internal faces are conservative: every joule leaving a is added to b.
            for (Face face : faces) {
                double flow = face.conductance(cells)
                        * (cells[face.b()].temperatureKelvin() - cells[face.a()].temperatureKelvin())
                        * sub;
                delta[face.a()] += flow;
                delta[face.b()] -= flow;
            }

            for (int i = 0; i < cells.length; i++) {
                ThermalCell cell = cells[i];
                if (cell != null) {
                    cell.setTemperatureKelvin(
                            cell.temperatureKelvin() + delta[i] / cell.heatCapacity());
                }
            }

            // Fixed-temperature reservoirs are one-way external energy paths.
            // Integrating dT/dt = G(Toutside-T)/C exactly prevents overshoot
            // even when a large opening has much greater G than the room air.
            for (ExternalLink link : externalLinks) {
                ThermalCell cell = cells[link.cellIndex()];
                if (cell == null) continue;
                double outside = link.ambient().temperatureKelvin(0, 0, 0);
                exchangeWithReservoir(cell, outside, link.conductance(), sub);
            }
            for (InfiltrationPath path : infiltrations) {
                ThermalCell cell = cells[path.cellIndex()];
                if (cell == null) continue;
                double outside = path.ambient().temperatureKelvin(0, 0, 0);
                double conductance = Infiltration.conductance(path.apertureAreaM2(), path.heightM(),
                        cell.temperatureKelvin(), outside);
                exchangeWithReservoir(cell, outside, conductance, sub);
            }
        }

        clearPower();
        return substeps;
    }

    private static void exchangeWithReservoir(ThermalCell cell, double outsideKelvin,
                                              double conductance, double seconds) {
        if (conductance <= 0) return;
        double fractionRemaining = Math.exp(-conductance * seconds / cell.heatCapacity());
        double next = outsideKelvin
                + (cell.temperatureKelvin() - outsideKelvin) * fractionRemaining;
        cell.setTemperatureKelvin(next);
    }

    private void clearPower() {
        for (ThermalCell cell : cells) {
            if (cell != null) cell.consumePower();
        }
    }

    /**
     * Prevents ordinary air-mixing links through an aperture voxel. The opening
     * can then be modelled exactly once by an explicit infiltration path.
     */
    public void blockAirMixingThroughCell(int cellIndex) {
        if (cellIndex < 0 || cellIndex >= cells.length) {
            throw new IndexOutOfBoundsException("air-mixing barrier cell index");
        }
        if (cell(cellIndex).material() != Materials.AIR) {
            throw new IllegalArgumentException("air-mixing barrier must be an air cell");
        }
        airMixingBlockedCells[cellIndex] = true;
    }

    /** Adds a fixed-temperature reservoir outside the grid. */
    public void addExternalLink(ExternalLink link) {
        if (link == null) throw new NullPointerException("link");
        if (link.cellIndex() >= cells.length) throw new IndexOutOfBoundsException("external cell index");
        externalLinks.add(link);
    }

    /** Adds a gap that leaks outside air into this cell (spec §3.7). */
    public void addInfiltration(InfiltrationPath path) {
        if (path == null) throw new NullPointerException("path");
        if (path.cellIndex() >= cells.length) throw new IndexOutOfBoundsException("infiltration cell index");
        infiltrations.add(path);
    }

    /** Model-time multiplier, 0 pauses physics (§5.3). */
    public void setSpeedMultiplier(double value) {
        if (!Double.isFinite(value) || value < 0 || value > SimulationClock.MAX_SPEED) {
            throw new IllegalArgumentException("bad speed multiplier " + value);
        }
        this.speedMultiplier = value;
    }

    public double speedMultiplier() {
        return speedMultiplier;
    }

    private double stableStep(List<Face> faces) {
        double[] rate = new double[cells.length];
        for (Face face : faces) {
            rate[face.a()] += face.maximumConductance();
            rate[face.b()] += face.maximumConductance();
        }
        double stable = Double.MAX_VALUE;
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == null || rate[i] <= 0) continue;
            stable = Math.min(stable, 0.9 * cells[i].heatCapacity() / rate[i]);
        }
        return stable;
    }

    /** Public wrapper used by tests and the adapter. */
    public double stableStep() {
        return stableStep(faces());
    }
}
