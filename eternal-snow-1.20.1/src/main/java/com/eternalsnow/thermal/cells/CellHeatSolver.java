package com.eternalsnow.thermal.cells;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Server-side numerical core, independent of Minecraft and room detection.
 * C dT/dt = P + sum(Gij*(Tj-Ti)) + Goutside*(Toutside-T).
 * All cells (air or solid) use the same law. The world adapter supplies materials,
 * six-face adjacency and exposure; this class never infers outdoor exposure from T.
 *
 * Units must be consistent: temperature in C, time in seconds, capacity in energy/C,
 * conductance in energy/(s*C), source power in energy/s. Celsius differences are Kelvin
 * differences; the reference for totalEnergy is 0 C, not absolute thermodynamic zero.
 *
 * No per-step allocations. A pair is evaluated once from the same substep snapshot,
 * with exactly opposite energy transfers. Stable explicit substeps make passive updates
 * convex combinations, so even tiny air capacities cannot overshoot their neighbours.
 * Not yet connected to ThermalEngine: activation requires the world adapter.
 */
public final class CellHeatSolver {
    public record Edge(int a, int b, double conductance) {}

    private static final int MAX_SUBSTEPS = 10000;
    private final double[] temperature, capacity, power, boundaryG, boundaryT, rates, deltaEnergy;
    private final boolean[] loaded;
    private final Edge[] edges;

    public CellHeatSolver(double[] temperature, double[] capacity, Edge[] edges) {
        if (temperature.length != capacity.length) throw new IllegalArgumentException("Cell count mismatch");
        this.temperature = temperature.clone();
        this.capacity = capacity.clone();
        this.edges = edges.clone();
        int n = temperature.length;
        power = new double[n]; boundaryG = new double[n]; boundaryT = new double[n];
        rates = new double[n]; deltaEnergy = new double[n]; loaded = new boolean[n];
        Arrays.fill(loaded, true);
        for (int i = 0; i < n; i++) {
            finite(temperature[i]);
            positive(capacity[i]);
        }
        Set<Long> pairs = new HashSet<>();
        for (Edge edge : this.edges) {
            if (edge == null || edge.a < 0 || edge.b < 0 || edge.a >= n || edge.b >= n || edge.a == edge.b)
                throw new IllegalArgumentException("Invalid thermal edge");
            nonnegative(edge.conductance);
            long pair = ((long) Math.min(edge.a, edge.b) << 32) | Math.max(edge.a, edge.b);
            if (!pairs.add(pair)) throw new IllegalArgumentException("Duplicate thermal edge");
        }
    }

    public int size() { return temperature.length; }
    public double temperature(int cell) { return temperature[cell]; }
    public double[] temperatures() { return temperature.clone(); }

    /** Replacing a block uses LOCAL AIR temperature supplied by the world adapter. */
    public void replace(int cell, double localAirTemperature, double newCapacity) {
        finite(localAirTemperature); positive(newCapacity);
        temperature[cell] = localAirTemperature;
        capacity[cell] = newCapacity;
        power[cell] = 0;
        boundaryG[cell] = 0;
    }

    /** Source on/off changes power only; it never resets stored heat. */
    public void setPower(int cell, double value) { nonnegative(value); power[cell] = value; }

    /** Only externally exposed cells receive a weather boundary; interiors use G=0. */
    public void setOutside(int cell, double conductance, double outsideTemperature) {
        nonnegative(conductance); finite(outsideTemperature);
        boundaryG[cell] = conductance;
        boundaryT[cell] = outsideTemperature;
    }

    /** An unloaded cell exchanges NO energy, consumes NO power, and performs NO catch-up. */
    public void setLoaded(int cell, boolean value) { loaded[cell] = value; }

    public void step(double seconds) {
        nonnegative(seconds);
        if (seconds == 0) return;
        Arrays.fill(rates, 0);
        for (Edge edge : edges) {
            if (loaded[edge.a] && loaded[edge.b]) {
                rates[edge.a] += edge.conductance;
                rates[edge.b] += edge.conductance;
            }
        }
        double stableStep = seconds;
        for (int i = 0; i < size(); i++) {
            if (!loaded[i]) continue;
            rates[i] += boundaryG[i];
            if (rates[i] > 0) stableStep = Math.min(stableStep, 0.9 * capacity[i] / rates[i]);
        }
        double required = Math.ceil(seconds / stableStep);
        if (!Double.isFinite(required) || required > MAX_SUBSTEPS)
            throw new IllegalArgumentException("Thermal step too large; schedule smaller steps, not elapsed unload time");
        int count = Math.max(1, (int) required);
        double dt = seconds / count;
        for (int sub = 0; sub < count; sub++) {
            Arrays.fill(deltaEnergy, 0);
            for (Edge edge : edges) {
                if (!loaded[edge.a] || !loaded[edge.b]) continue;
                double q = edge.conductance * (temperature[edge.b] - temperature[edge.a]) * dt;
                deltaEnergy[edge.a] += q;
                deltaEnergy[edge.b] -= q;
            }
            for (int i = 0; i < size(); i++) {
                if (loaded[i]) deltaEnergy[i] += dt * (power[i] + boundaryG[i] * (boundaryT[i] - temperature[i]));
            }
            for (int i = 0; i < size(); i++) {
                if (loaded[i]) temperature[i] += deltaEnergy[i] / capacity[i];
            }
        }
    }

    public double totalEnergy() {
        double energy = 0;
        for (int i = 0; i < size(); i++) energy += temperature[i] * capacity[i];
        return energy;
    }

    private static void finite(double v) {
        if (!Double.isFinite(v)) throw new IllegalArgumentException("Non-finite thermal value");
    }
    private static void nonnegative(double v) {
        finite(v);
        if (v < 0) throw new IllegalArgumentException("Negative thermal parameter");
    }
    private static void positive(double v) {
        finite(v);
        if (v <= 0) throw new IllegalArgumentException("Non-positive heat capacity");
    }
}
