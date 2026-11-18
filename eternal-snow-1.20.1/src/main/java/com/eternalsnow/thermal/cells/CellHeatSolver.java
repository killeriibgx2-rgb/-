package com.eternalsnow.thermal.cells;

import java.util.Arrays;

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
 * Used by WorldCellThermal; the old room solver is no longer ticked.
 */
public final class CellHeatSolver {
    public record Edge(int a, int b, double conductance) {}

    private static final int MAX_SUBSTEPS = 10000;
    private double[] temperature, capacity, power, boundaryG, boundaryT, rates, deltaEnergy;
    private boolean[] loaded;
    private int[] edgeA, edgeB, activeEdges;
    private double[] edgeG;
    private int activeCount, lastSubsteps;
    private boolean topologyDirty=true;
    private double[] pairTransfer=new double[0], externalScale=new double[0], inverseCapacity;
    private double splitHalf=Double.NaN;
    private boolean splitDirty=true;
    private final it.unimi.dsi.fastutil.ints.IntOpenHashSet splitEdgesDirty=new it.unimi.dsi.fastutil.ints.IntOpenHashSet();
    private final it.unimi.dsi.fastutil.ints.IntOpenHashSet externalDirty=new it.unimi.dsi.fastutil.ints.IntOpenHashSet();
    public int edgeCount() { return edgeCount; }
    public int lastSubsteps() { return lastSubsteps; }
    private int cellCount, edgeCount;
    private it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap edgeIndices;

    public CellHeatSolver(double[] temperature, double[] capacity, Edge[] edges) {
        if (temperature.length != capacity.length) throw new IllegalArgumentException("Cell count mismatch");
        this.temperature = temperature.clone();
        this.capacity = capacity.clone();
        inverseCapacity=new double[capacity.length];
        edgeA=new int[edges.length];edgeB=new int[edges.length];edgeG=new double[edges.length];
        activeEdges=new int[0];
        int n = temperature.length; cellCount=n; edgeCount=edges.length;
        power = new double[n]; boundaryG = new double[n]; boundaryT = new double[n];
        rates = new double[0]; deltaEnergy = new double[0]; loaded = new boolean[n];
        Arrays.fill(loaded, true);
        for (int i = 0; i < n; i++) {
            finite(temperature[i]);
            positive(capacity[i]);inverseCapacity[i]=1/capacity[i];
        }
        // Long.hashCode((a << 32) | b) == a ^ b. Grid neighbours therefore
        // collapse into a handful of HashSet bins (all even a -> a+1 hash to 1).
        // The old boxed set spent seconds resizing/treeifying those bins on every
        // world rebuild. Sort primitive keys instead: bounded O(E log E), 8E bytes,
        // no hash collisions, boxed Longs or tree nodes; retain duplicate validation.
        long[] pairs = new long[edges.length];
        for (int i = 0; i < edges.length; i++) {
            Edge edge = edges[i];
            if (edge == null || edge.a < 0 || edge.b < 0 || edge.a >= n || edge.b >= n || edge.a == edge.b)
                throw new IllegalArgumentException("Invalid thermal edge");
            nonnegative(edge.conductance);
            edgeA[i]=edge.a;edgeB[i]=edge.b;edgeG[i]=edge.conductance;
            pairs[i] = ((long) Math.min(edge.a, edge.b) << 32) | Math.max(edge.a, edge.b);
        }
        Arrays.sort(pairs);
        for (int i = 1; i < pairs.length; i++) {
            if (pairs[i] == pairs[i - 1]) throw new IllegalArgumentException("Duplicate thermal edge");
        }
    }

    public int size() { return cellCount; }
    public double temperature(int cell) { return temperature[cell]; }
    public double[] temperatures() { return Arrays.copyOf(temperature,cellCount); }

    /** Append without rebuilding old cell state or topology. Amortized array growth. */
    public int addCell(double initialTemperature,double heatCapacity) {
        finite(initialTemperature);positive(heatCapacity);
        if(cellCount==temperature.length) {
            int n=Math.max(16,cellCount+Math.max(1,cellCount/2));
            temperature=Arrays.copyOf(temperature,n);capacity=Arrays.copyOf(capacity,n);
            inverseCapacity=Arrays.copyOf(inverseCapacity,n);
            power=Arrays.copyOf(power,n);boundaryG=Arrays.copyOf(boundaryG,n);
            boundaryT=Arrays.copyOf(boundaryT,n);
            if(rates.length>0)rates=Arrays.copyOf(rates,n);
            if(deltaEnergy.length>0)deltaEnergy=Arrays.copyOf(deltaEnergy,n);
            loaded=Arrays.copyOf(loaded,n);
        }
        int i=cellCount++;temperature[i]=initialTemperature;capacity[i]=heatCapacity;inverseCapacity[i]=1/heatCapacity;loaded[i]=true;topologyDirty=true;externalDirty.add(i);
        return i;
    }
    public void setCapacity(int cell,double value) {
        positive(value);if(capacity[cell]!=value){capacity[cell]=value;inverseCapacity[cell]=1/value;splitDirty=true;}
    }
    private static long pair(int a,int b) {return ((long)Math.min(a,b)<<32)|Math.max(a,b);}
    /** Existing edge is updated, new edge is added once; primitive mixed hashing avoids Long.hashCode. */
    public void setConnection(int a,int b,double conductance) {
        if(a<0||b<0||a>=cellCount||b>=cellCount||a==b)throw new IllegalArgumentException("Invalid thermal edge");
        nonnegative(conductance);
        if(edgeIndices==null) {
            edgeIndices=new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap(edgeCount);
            edgeIndices.defaultReturnValue(-1);
            for(int e=0;e<edgeCount;e++)edgeIndices.put(pair(edgeA[e],edgeB[e]),e);
        }
        long key=pair(a,b);int index=edgeIndices.get(key);
        if(index>=0) {
            if(edgeG[index]!=conductance){edgeG[index]=conductance;topologyDirty=true;splitEdgesDirty.add(index);}
            return;
        }
        if(edgeCount==edgeA.length) {
            int n=Math.max(16,edgeCount+Math.max(1,edgeCount/2));
            edgeA=Arrays.copyOf(edgeA,n);edgeB=Arrays.copyOf(edgeB,n);
            edgeG=Arrays.copyOf(edgeG,n);
            if(activeEdges.length>0)activeEdges=Arrays.copyOf(activeEdges,n);
        }
        edgeIndices.put(key,edgeCount);edgeA[edgeCount]=a;edgeB[edgeCount]=b;
        edgeG[edgeCount]=conductance;splitEdgesDirty.add(edgeCount++);topologyDirty=true;
    }

    /** Replacing a block uses LOCAL AIR temperature supplied by the world adapter. */
    public void replace(int cell, double localAirTemperature, double newCapacity) {
        finite(localAirTemperature); positive(newCapacity);
        temperature[cell] = localAirTemperature;
        setCapacity(cell,newCapacity);
        power[cell] = 0;
        boundaryG[cell] = 0;externalDirty.add(cell);
    }

    /** Source on/off changes power only; it never resets stored heat. */
    public void setPower(int cell, double value) { nonnegative(value); power[cell] = value; }

    /** Only externally exposed cells receive a weather boundary; interiors use G=0. */
    public void setOutside(int cell, double conductance, double outsideTemperature) {
        nonnegative(conductance); finite(outsideTemperature);
        if(boundaryG[cell]!=conductance)externalDirty.add(cell);
        boundaryG[cell] = conductance;
        boundaryT[cell] = outsideTemperature;
    }

    /** An unloaded cell exchanges NO energy, consumes NO power, and performs NO catch-up. */
    public void setLoaded(int cell, boolean value) {
        if(loaded[cell]!=value){loaded[cell]=value;topologyDirty=true;}
    }
    private void refreshTopology() {
        if(!topologyDirty)return;
        if(rates.length<temperature.length)rates=new double[temperature.length];
        if(activeEdges.length<edgeCount)activeEdges=new int[edgeA.length];
        Arrays.fill(rates,0,cellCount,0);
        activeCount=0;
        for(int e=0;e<edgeCount;e++) {
            int a=edgeA[e],b=edgeB[e];
            if(loaded[a] && loaded[b]) {
                activeEdges[activeCount++]=e;rates[a]+=edgeG[e];rates[b]+=edgeG[e];
            }
        }
        topologyDirty=false;
    }

    public void step(double seconds) {
        nonnegative(seconds);
        if (seconds == 0) return;
        refreshTopology();
        if(deltaEnergy.length<temperature.length)deltaEnergy=new double[temperature.length];
        double stableStep = seconds;
        for (int i = 0; i < size(); i++) {
            if (!loaded[i]) continue;
            double rate=rates[i]+boundaryG[i];
            if (rate > 0) stableStep = Math.min(stableStep, 0.9 * capacity[i] / rate);
        }
        double required = Math.ceil(seconds / stableStep);
        if (!Double.isFinite(required) || required > MAX_SUBSTEPS)
            throw new IllegalArgumentException("Thermal step too large; schedule smaller steps, not elapsed unload time");
        int count = Math.max(1, (int) required);
        lastSubsteps=count;
        double dt = seconds / count;
        for (int sub = 0; sub < count; sub++) {
            // Initialise from external power directly: no zero-fill + later extra pass.
            for(int i=0;i<cellCount;i++)
                deltaEnergy[i]=loaded[i]?dt*(power[i]+boundaryG[i]*(boundaryT[i]-temperature[i])):0;
            for(int active=0;active<activeCount;active++) {
                int e=activeEdges[active],a=edgeA[e],b=edgeB[e];
                double q=edgeG[e]*(temperature[b]-temperature[a])*dt;
                deltaEnergy[a]+=q;deltaEnergy[b]-=q;
            }
            for (int i = 0; i < size(); i++) {
                if (loaded[i]) temperature[i] += deltaEnergy[i] / capacity[i];
            }
        }
    }

    /**
     * Symmetric exact-pair splitting. Each pair transfer is analytic and conservative,
     * and each weather/source operator is solved analytically too. Two 0.25s slices
     * replace the global explicit CFL loop for a 0.5s world step. This is a different,
     * second-order integrator, not a cap on unstable Euler substeps.
     */
    public void stepConservative(double seconds) {
        nonnegative(seconds);if(seconds==0)return;
        double slices=Math.ceil(seconds/.25);
        if(slices>MAX_SUBSTEPS)throw new IllegalArgumentException("Thermal step too large");
        int count=Math.max(1,(int)slices);double half=seconds/count/2;
        prepareSplit(half);lastSubsteps=count;
        for(int sub=0;sub<count;sub++) {
            externalHalf();
            for(int e=0;e<edgeCount;e++)exchange(e);
            for(int e=edgeCount-1;e>=0;e--)exchange(e);
            externalHalf();
        }
    }
    private void prepareSplit(double half) {
        boolean all=splitDirty || splitHalf!=half;
        splitHalf=half;
        if(pairTransfer.length<edgeA.length)pairTransfer=Arrays.copyOf(pairTransfer,edgeA.length);
        if(externalScale.length<temperature.length)externalScale=Arrays.copyOf(externalScale,temperature.length);
        if(all) {
            for(int e=0;e<edgeCount;e++)preparePair(e);
            for(int i=0;i<cellCount;i++)prepareExternal(i);
        } else {
            for(int e:splitEdgesDirty)preparePair(e);
            for(int i:externalDirty)prepareExternal(i);
        }
        splitEdgesDirty.clear();externalDirty.clear();splitDirty=false;
    }
    private void preparePair(int e) {
        double inverse=inverseCapacity[edgeA[e]]+inverseCapacity[edgeB[e]];
        pairTransfer[e]=-Math.expm1(-edgeG[e]*inverse*splitHalf)/inverse;
    }
    private void prepareExternal(int i) {
        externalScale[i]=boundaryG[i]==0?splitHalf/capacity[i]
                :-Math.expm1(-boundaryG[i]*splitHalf/capacity[i])/boundaryG[i];
    }
    private void exchange(int e) {
        int a=edgeA[e],b=edgeB[e];if(!loaded[a]||!loaded[b])return;
        double difference=temperature[b]-temperature[a];if(difference==0)return;
        double q=pairTransfer[e]*difference;
        temperature[a]+=q*inverseCapacity[a];temperature[b]-=q*inverseCapacity[b];
    }
    private void externalHalf() {
        for(int i=0;i<cellCount;i++)if(loaded[i])
            temperature[i]+=externalScale[i]*(power[i]+boundaryG[i]*(boundaryT[i]-temperature[i]));
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
