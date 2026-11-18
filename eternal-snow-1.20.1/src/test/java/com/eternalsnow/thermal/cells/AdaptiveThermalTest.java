package com.eternalsnow.thermal.cells;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class AdaptiveThermalTest {
    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}
    private static AdaptiveThermalTile tile(int x,int y,int z,double temperature,boolean fine) {
        var t=new AdaptiveThermalTile(x,y,z,new TileTemperatureState(filled(temperature)));
        t.configure(fine,filled(1),filled(1),filled(0),filled(1),filled(0));return t;
    }
    @Test public void negativeCoordinatesAndLocalIndicesDoNotOverlap() {
        assertEquals(BlockPos.asLong(-8,-8,-16),WorldCellThermal.tileKey(new BlockPos(-1,-1,-9)));
        Set<Integer> indices=new HashSet<>();
        for(int x=0;x<8;x++)for(int y=0;y<8;y++)for(int z=0;z<8;z++)assertTrue(indices.add(AdaptiveThermalTile.index(x,y,z)));
        assertEquals(512,indices.size());
    }
    @Test public void uniformFineGridHasExpectedContacts() {
        var a=tile(0,0,0,20,true);var b=tile(8,0,0,-20,true);
        var g=new AdaptiveThermalGraph(List.of(a,b));
        assertEquals(1024,g.solver.size());assertEquals(2*3*7*8*8+64,g.solver.edgeCount());
        double energy=g.solver.totalEnergy();g.clearPower();g.step(.5,-30);
        assertEquals(energy,g.solver.totalEnergy(),1e-8);
        assertTrue(a.temperatures.get(7)<20);assertTrue(b.temperatures.get(0)>-20);
    }
    @Test public void fineCoarseBoundaryTransfersEqualOppositeEnergy() {
        for(boolean firstFine:new boolean[]{false,true}) {
            var a=tile(0,0,0,40,firstFine);var b=tile(8,0,0,-20,!firstFine);
            var g=new AdaptiveThermalGraph(List.of(a,b));assertEquals(513,g.solver.size());
            double before=g.solver.totalEnergy();
            for(int step=0;step<100;step++){g.clearPower();g.step(.5,-30);}
            assertEquals(before,g.solver.totalEnergy(),1e-7);
            assertTrue(a.mean()<40);assertTrue(b.mean()>-20);
            for(int i=0;i<512;i++)assertTrue(a.temperatures.get(i)<=40+1e-9&&b.temperatures.get(i)>=-20-1e-9);
        }
    }
    @Test public void refineCoarsenRetainsWeightedEnergyAndSourcePower() {
        var a=tile(0,0,0,-20,true);double[] capacities=filled(1);capacities[0]=80;
        a.temperatures.set(0,100);a.configure(true,capacities,filled(1),filled(0),filled(1),filled(0));
        double expected=80*100-511*20;
        for(int cycle=0;cycle<100;cycle++) {
            a.configure(cycle%2==0,capacities,filled(1),filled(0),filled(1),filled(0));
            var g=new AdaptiveThermalGraph(List.of(a));assertEquals(expected,g.solver.totalEnergy(),1e-6);
            g.clearPower();g.addPower(a,0,68);g.step(.5,-30);expected+=34;
            double actual=0;for(int i=0;i<512;i++)actual+=capacities[i]*a.temperatures.get(i);
            assertEquals(expected,actual,1e-6);
        }
    }
    @Test public void coarseResidualCannotCreateNewLocalExtrema() {
        double[] initial=filled(0);initial[0]=100;
        var state=new TileTemperatureState(initial);double mean=100.0/512;
        state.coarseUpdate(.01,mean,1);
        double sum=0;
        for(int i=0;i<512;i++){assertTrue(state.get(i)>=0);assertTrue(state.get(i)<=100);sum+=state.get(i);}
        assertEquals(.01*512,sum,1e-9);
    }
    @Test public void mixedMaterialWallContactRemainsOriginalAtFineResolution() {
        var air=tile(0,0,0,20,true);var wall=tile(8,0,0,-20,true);
        boolean[] airFlags=new boolean[512];Arrays.fill(airFlags,true);
        air.configure(true,filled(1),filled(3),filled(0),filled(1),filled(0));
        air.fineMaterials(filled(1),airFlags,3);
        wall.configure(true,filled(50),filled(.24),filled(0),filled(1),filled(0));
        var graph=new AdaptiveThermalGraph(List.of(air,wall));
        assertEquals(AdaptiveThermalTile.contact(1,.24,.5,.5,1),2*.24/1.24,1e-12);
        double e=graph.solver.totalEnergy();graph.step(.5,-30);assertEquals(e,graph.solver.totalEnergy(),1e-6);
    }
    @Test public void legacyMigrationPreservesOtherTileAndOldValuesExactly() {
        var original=new CellThermalSavedData();long chunk=0;
        long p=BlockPos.asLong(1,1,1),other=BlockPos.asLong(9,1,1);
        original.chunk(chunk).put(p,73.123456789);original.chunk(chunk).put(other,-31.25);
        CompoundTag legacy=original.save(new CompoundTag());legacy.putInt("version",1);legacy.remove("tiles");
        var data=CellThermalSavedData.load(legacy);
        var migrated=data.migrateTile(BlockPos.asLong(0,0,0),filled(-20));
        assertEquals(73.123456789,migrated.get(AdaptiveThermalTile.index(1,1,1)),0);
        assertFalse(data.chunk(chunk).containsKey(p));assertEquals(-31.25,data.chunk(chunk).get(other),0);
        var again=CellThermalSavedData.load(data.save(new CompoundTag()));
        assertEquals(73.123456789,again.tiles.get(0L).get(AdaptiveThermalTile.index(1,1,1)),0);
        assertEquals(-31.25,again.chunk(chunk).get(other),0);
    }
    @Test public void coarseTransformSurvivesSaveAndPauseWithoutCatchUp() {
        var t=tile(0,0,0,-20,false);var g=new AdaptiveThermalGraph(List.of(t));
        g.addPower(t,0,68);g.step(.5,-30);
        var data=new CellThermalSavedData();data.putTile(0,t.temperatures);
        for(int i=0;i<10;i++)data=CellThermalSavedData.load(data.save(new CompoundTag()));
        for(int i=0;i<512;i++)assertEquals(t.temperatures.get(i),data.tiles.get(0L).get(i),0);
    }
    @Test public void removedChunkDoesNotExchangeWithRemainingGraph() {
        var hot=tile(0,0,0,40,false);var cold=tile(8,0,0,-20,false);
        var original=new AdaptiveThermalGraph(List.of(hot,cold));original.step(.5,-30);
        double paused=cold.mean();var active=new AdaptiveThermalGraph(List.of(hot));
        active.addPower(hot,0,68);for(int i=0;i<100;i++)active.step(.5,-30);
        assertEquals(paused,cold.mean(),0);
        var resumed=new AdaptiveThermalGraph(List.of(hot,cold));resumed.step(.5,-30);assertTrue(cold.mean()>paused);
    }
    @Test(timeout=30000) public void twoMillionRetainedTemperaturesDoNotBecomeTwoMillionNodes() {
        var tiles=new ArrayList<AdaptiveThermalTile>();
        for(int z=0;z<16;z++)for(int y=0;y<16;y++)for(int x=0;x<16;x++)tiles.add(tile(x*8,y*8,z*8,-20,false));
        var graph=new AdaptiveThermalGraph(tiles);
        assertEquals(4096,graph.solver.size());assertEquals(11520,graph.solver.edgeCount());
        double initial=graph.solver.totalEnergy();graph.clearPower();graph.addPower(tiles.get(0),0,68);
        long start=System.nanoTime();
        for(int step=0;step<1200;step++)graph.step(.5,-20+10*Math.sin(step/100.0));
        assertEquals(initial+68*600,graph.solver.totalEnergy(),1e-4);
        assertEquals(4096,graph.solver.size());assertEquals(4096*512,tiles.size()*512);
        System.out.println("Adaptive 2M retained / 4096 nodes / 10 simulated minutes ms="+(System.nanoTime()-start)/1e6);
    }
    @Test(timeout=30000) public void observerZoneAndDistantReservoirsRemainBoundedDuringWeather() {
        var tiles=new ArrayList<AdaptiveThermalTile>();
        for(int z=0;z<6;z++)for(int y=0;y<6;y++)for(int x=0;x<6;x++) {
            var t=tile(x*8,y*8,z*8,-20,x<3&&y<3&&z<3);
            double[] outside=filled(0);if(y==5)for(int i=448;i<512;i++)outside[i]=1;
            t.configure(t.detailed,filled(1),filled(1),outside,filled(1),filled(0));tiles.add(t);
        }
        var g=new AdaptiveThermalGraph(tiles);int n=g.solver.size();assertEquals(27*512+189,n);
        for(int i=0;i<120;i++)g.step(.5,-30+10*Math.sin(i/20.0));
        assertEquals(n,g.solver.size());
        for(var t:tiles)for(int i=0;i<512;i++)assertTrue(t.temperatures.get(i)>=-40.000001&&t.temperatures.get(i)<=-19.999999);
    }
    @Test(timeout=60000) public void largeMixedDomainSurvivesGrowthWeatherAndObserverMoves() {
        var tiles=new ArrayList<AdaptiveThermalTile>();
        for(int z=0;z<16;z++)for(int y=0;y<16;y++)for(int x=0;x<16;x++)
            tiles.add(tile(x*8,y*8,z*8,-20,false));
        long start=System.nanoTime();
        for(int cycle=0;cycle<12;cycle++) {
            // Move a 3-cubed detailed observer region, rebuild after growth and material edits.
            for(var t:tiles) {
                boolean detail=t.x/8>=cycle&&t.x/8<cycle+3&&t.y<24&&t.z<24;
                t.configure(detail,filled(1),filled(cycle%2==0?1:2),filled(.01),filled(1),filled(0));
            }
            tiles.add(tile(128,cycle*8,0,-20,false));
            var g=new AdaptiveThermalGraph(tiles);
            assertEquals(27*511+tiles.size(),g.solver.size());
            for(int step=0;step<100;step++) {
                g.clearPower();for(int source=0;source<1471;source++)g.addPower(tiles.get(source),0,10);
                g.step(.5,-30+10*Math.sin((cycle*100+step)/100.0));
            }
            for(var t:tiles)for(int cell=0;cell<512;cell++) {
                assertTrue(Double.isFinite(t.temperatures.get(cell)));
                assertTrue(t.temperatures.get(cell)>=-40.000001);
            }
        }
        System.out.println("Adaptive mixed 2M retained, 1471 sources, 12 rebuilds, 10 simulated minutes ms="+(System.nanoTime()-start)/1e6);
    }
    private double heatedRoom(boolean insulated) {
        var t=tile(0,0,0,-20,true);
        double[] c=filled(1),k=filled(1),g=filled(0);
        for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++) {
            int i=AdaptiveThermalTile.index(x,y,z);
            if(x==0||x==7||y==0||y==7||z==0||z==7){g[i]=1;if(insulated){c[i]=20;k[i]=.05;}}
        }
        t.configure(true,c,k,g,filled(1),filled(0));var graph=new AdaptiveThermalGraph(List.of(t));
        graph.addPower(t,AdaptiveThermalTile.index(3,3,3),68);
        for(int step=0;step<600;step++)graph.step(.5,-20);
        return t.temperatures.get(AdaptiveThermalTile.index(4,3,3));
    }
    @Test public void detailedInsulatedRoomWarmsMoreThanExposedAir() {
        double closed=heatedRoom(true),open=heatedRoom(false);
        assertTrue(closed>open);assertTrue(closed>-20);
    }
    @Test public void inPlaceMaterialEditKeepsTemperaturesAndEnergy() {
        var a=tile(0,0,0,20,true);var b=tile(8,0,0,-20,false);
        var g=new AdaptiveThermalGraph(List.of(a,b));
        g.addPower(a,0,68);
        for(int i=0;i<20;i++)g.step(.5,-30);
        double aTemperature=g.solver.temperature(a.node+5),bMean=b.mean();
        assertTrue(g.canRefreshInPlace(tile(0,0,0,0,true)));
        assertFalse(g.canRefreshInPlace(tile(0,0,0,0,false)));
        // A single edited block replaces this tile's materials; nothing else may move.
        var edited=tile(0,0,0,20,true);
        double[] capacities=filled(1);capacities[5]=9;
        edited.configure(true,capacities,filled(2),filled(0),filled(1),filled(0));
        g.refreshInPlace(edited);
        // Editing capacity legitimately changes stored E=C*T; temperatures must not reset.
        assertEquals(aTemperature,g.solver.temperature(a.node+5),0);
        assertEquals(bMean,b.mean(),0);
        assertEquals(513,g.solver.size());
        double afterRefresh=g.solver.totalEnergy();
        g.clearPower();
        for(int i=0;i<50;i++)g.step(.5,-30);
        assertEquals(afterRefresh,g.solver.totalEnergy(),1e-7);
        assertTrue(b.mean()>-20);
    }
    @Test(timeout=60000) public void inPlaceEditIsCheaperThanFullRebuild() {
        var tiles=new ArrayList<AdaptiveThermalTile>();
        for(int z=0;z<16;z++)for(int y=0;y<16;y++)for(int x=0;x<16;x++)
            tiles.add(tile(x*8,y*8,z*8,-20,x<3&&y<3&&z<3));
        var graph=new AdaptiveThermalGraph(tiles);
        long rebuildStart=System.nanoTime();
        for(int cycle=0;cycle<20;cycle++)new AdaptiveThermalGraph(tiles);
        long rebuildNanos=System.nanoTime()-rebuildStart;
        // Prepare edited tiles up front so the loop times only the graph operation.
        var edited=new ArrayList<AdaptiveThermalTile>();
        for(var t:tiles) {
            var copy=tile(t.x,t.y,t.z,-20,t.detailed);
            copy.configure(t.detailed,filled(1),filled(2),filled(0),filled(1),filled(0));
            edited.add(copy);
        }
        long refreshStart=System.nanoTime();
        // One block edit touches one tile; the rest of the graph stays untouched.
        for(int cycle=0;cycle<20;cycle++)graph.refreshInPlace(edited.get(cycle%edited.size()));
        long refreshNanos=System.nanoTime()-refreshStart;
        System.out.println("Adaptive 4096 tiles: full rebuild x20="+rebuildNanos/1e6
                +"ms, in-place tile edit x20="+refreshNanos/1e6+"ms");
        assertTrue("in-place edit should beat a full rebuild",refreshNanos<rebuildNanos);
    }
}
