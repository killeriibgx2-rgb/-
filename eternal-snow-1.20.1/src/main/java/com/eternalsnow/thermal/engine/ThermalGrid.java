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

    public ThermalGrid(int sizeX, int sizeY, int sizeZ) {
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            throw new IllegalArgumentException("grid dimensions must be positive");
        }
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.cells = new ThermalCell[sizeX * sizeY * sizeZ];
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
                    if (x + 1 < sizeX) addFace(faces, a, index(x + 1, y, z));
                    if (y + 1 < sizeY) addFace(faces, a, index(x, y + 1, z));
                    if (z + 1 < sizeZ) addFace(faces, a, index(x, y, z + 1));
                }
            }
        }
        return faces;
    }

    private void addFace(List<Face> into, int a, int b) {
        if (cells[b] == null) return;
        Material ma = cells[a].material();
        Material mb = cells[b].material();
        boolean airA = ma == Materials.AIR;
        boolean airB = mb == Materials.AIR;
        double conductance;
        if (airA && airB) {
            // Air-to-air through a 1 m cell gap: treat as a convective link
            // rather than a solid contact, otherwise open air would conduct
            // like a solid of λ=0.026 with no mixing at all.
            conductance = Transfer.convectionConductance();
        } else if (airA || airB) {
            conductance = Transfer.convectionConductance();
        } else {
            conductance = Transfer.conductionConductance(ma, mb);
        }
        into.add(new Face(a, b, conductance));
    }

    /** Conductance, W/K, between two adjacent cells. */
    public double faceConductance(int x, int y, int z, Axis axis) {
        int dx = axis == Axis.X ? 1 : 0;
        int dy = axis == Axis.Y ? 1 : 0;
        int dz = axis == Axis.Z ? 1 : 0;
        int a = index(x, y, z);
        int b = index(x + dx, y + dy, z + dz);
        Material ma = cell(a).material();
        Material mb = cell(b).material();
        if (ma == Materials.AIR || mb == Materials.AIR) {
            return Transfer.convectionConductance();
        }
        return Transfer.conductionConductance(ma, mb);
    }

    public enum Axis { X, Y, Z }

    private record Face(int a, int b, double conductance) {
    }

    /**
     * Advances the simulation by {@code seconds} of model time.
     *
     * <p>Explicit forward Euler with sub-stepping capped at
     * {@code 0.9·C/G} per cell, which is the standard stability limit for this
     * stencil. Sub-stepping is not an approximation of physics, it is exactly
     * the same integration with a smaller Δt — so a stable result is
     * trustworthy at any step size.
     *
     * @return number of sub-steps taken
     */
    public int step(double seconds) {
        if (!(seconds > 0) || !Double.isFinite(seconds)) {
            throw new IllegalArgumentException("seconds must be finite and > 0");
        }
        List<Face> faces = faces();
        double stable = stableStep(faces);
        int substeps = Math.max(1, (int) Math.ceil(seconds / stable));
        double dt = seconds / substeps;

        double[] delta = new double[cells.length];
        for (int sub = 0; sub < substeps; sub++) {
            java.util.Arrays.fill(delta, 0.0);
            for (int i = 0; i < cells.length; i++) {
                ThermalCell cell = cells[i];
                if (cell == null) continue;
                // External power first: it is the only non-conservative term,
                // and keeping it out of the face loop makes the audit exact.
                delta[i] += cell.externalPowerWatts() * dt;
            }
            for (Face face : faces) {
                double flow = face.conductance()
                        * (cells[face.b()].temperatureKelvin() - cells[face.a()].temperatureKelvin())
                        * dt;
                delta[face.a()] += flow;
                delta[face.b()] -= flow;
            }
            for (int i = 0; i < cells.length; i++) {
                ThermalCell cell = cells[i];
                if (cell == null) continue;
                cell.setTemperatureKelvin(cell.temperatureKelvin() + delta[i] / cell.heatCapacity());
                cell.consumePower();
            }
        }
        return substeps;
    }

    /**
     * Largest Δt that keeps every cell within 90% of its stability limit:
     * {@code Δt ≤ 0.9 · C_i / Σ G_i}.
     */
    private double stableStep(List<Face> faces) {
        double[] rate = new double[cells.length];
        for (Face face : faces) {
            rate[face.a()] += face.conductance();
            rate[face.b()] += face.conductance();
        }
        double stable = Double.MAX_VALUE;
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == null || rate[i] <= 0) continue;
            stable = Math.min(stable, 0.9 * cells[i].heatCapacity() / rate[i]);
        }
        return stable == Double.MAX_VALUE ? Double.MAX_VALUE : stable;
    }

    /** Public wrapper used by tests and the adapter. */
    public double stableStep() {
        return stableStep(faces());
    }
}
