package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Owner's report, second half. Outdoors at -2.2 C the ground "in a large area" around torches
 * was warm. Air is fine now, but ground is a solid: it is not covered by the air mixing rule and
 * it stores what it receives. These tests pin how far heat actually leaks into the ground.
 */
public class SurfaceHeatSpillTest {

    private static final double AIR_CAPACITY = 0.02;
    private static final double TORCH_POWER = 21;
    private static final int SIZE = 32;
    /** Solid ground next to a flame as the world adapter couples it. */
    private static final double GROUND_G = 1.0;

    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    /** @return {ground temperature at the torch, ground 8 blocks away, air at the torch} */
    private static double[] run(int torches,double groundG){
        var list=new ArrayList<AdaptiveThermalTile>();
        for(int tz=0;tz<SIZE/8;tz++)for(int tx=0;tx<SIZE/8;tx++){
            var tile=new AdaptiveThermalTile(tx*8,0,tz*8,new TileTemperatureState(filled(-2.2)));
            double[] cap=filled(1),k=filled(1),g=filled(0);
            boolean[] air=new boolean[512];
            for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
                int i=AdaptiveThermalTile.index(x,y,z);
                if(y==0){cap[i]=4*20;k[i]=4;g[i]=groundG;}
                else{air[i]=true;cap[i]=AIR_CAPACITY;g[i]=1;}
            }
            tile.configure(true,cap,k,g,filled(1),filled(0));
            tile.fineMaterials(filled(1),air,3);
            list.add(tile);
        }
        var graph=new AdaptiveThermalGraph(list);
        graph.setRoomAirMixing(20);
        graph.setMixingSweeps(12);
        int[] spots={15,17,19,16,18,14,20,13,21,12,22};
        for(int step=0;step<3600;step++){
            graph.clearPower();
            for(int i=0;i<torches;i++){
                int gx=spots[i],gz=15+(i%3)-1;
                graph.addPower(list.get((gx>>3)+4*(gz>>3)),AdaptiveThermalTile.index(gx&7,1,gz&7),
                        TORCH_POWER);
            }
            graph.step(.5,-2.2);
        }
        var at=list.get((16>>3)+4*(15>>3));
        var away=list.get((24>>3)+4*(15>>3));
        return new double[]{
            at.temperatures.get(AdaptiveThermalTile.index(0,0,7)),
            away.temperatures.get(AdaptiveThermalTile.index(0,0,7)),
            at.temperatures.get(AdaptiveThermalTile.index(0,1,7))};
    }

    @Test public void groundFarFromTorchesStaysCold(){
        double[] r=run(8,GROUND_G);
        System.out.println(String.format(Locale.ROOT,
            "Ground: at the torches=%.2f C, eight blocks away=%.2f C, air at torch=%.2f C (outside -2.2)",
            r[0],r[1],r[2]));
        assertTrue("ground eight blocks from torches must not be warm, was "+r[1],r[1]<0.5);
    }

    @Test public void groundUnderTheTorchesDoesNotCook(){
        double[] r=run(8,GROUND_G);
        assertTrue("ground under a torch must not be cooked, was "+r[0],r[0]<40);
    }

    /**
     * And the fix must not go the other way either: heat still has to leave the ground, so an
     * exposed block cannot be insulated from the weather by accident.
     */
    @Test public void exposedGroundStaysAtTheAirTemperature(){
        // A surface block touches the weather on one face, so its coupling is a fraction of its
        // conductivity — small, but far from zero: the ground must follow the weather.
        double g=WorldCellThermal.weatherCoupling(false,true,64,64,4.0);
        System.out.println(String.format(Locale.ROOT,"Exposed ground coupling = %.3f",g));
        assertTrue("exposed ground must follow the weather, got "+g,g>0.02);
        assertTrue("and must not be a short circuit, got "+g,g<1);
    }
}
