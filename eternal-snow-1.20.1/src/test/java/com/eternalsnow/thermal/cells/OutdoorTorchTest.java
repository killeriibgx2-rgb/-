package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Owner's report, second half: outdoors at -2.2 C the air "far from the torches" was up to
 * +3 C over a large area. A torch must warm the blocks it stands in, not the field around it.
 */
public class OutdoorTorchTest {

    private static final double AIR_CAPACITY = 0.02;
    private static final double TORCH_POWER = 21;      // heatTorch 2.1 x roomSourcePower 10
    private static final int SIZE = 32;

    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    /** Open field: grass ground under sky, air above it, a torch on the ground in the middle. */
    private static Map<String,Double> run(int torches){
        var list=new ArrayList<AdaptiveThermalTile>();
        for(int tz=0;tz<SIZE/8;tz++)for(int tx=0;tx<SIZE/8;tx++){
            var tile=new AdaptiveThermalTile(tx*8,0,tz*8,new TileTemperatureState(filled(-2.2)));
            double[] cap=filled(1),k=filled(1),g=filled(0);
            boolean[] air=new boolean[512];
            for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
                int i=AdaptiveThermalTile.index(x,y,z);
                if(y==0){cap[i]=4*20;k[i]=4;g[i]=WorldCellThermal.AIR_OPEN_COUPLING;}                  // exposed ground
                else{air[i]=true;cap[i]=AIR_CAPACITY;g[i]=WorldCellThermal.AIR_OPEN_COUPLING;}          // open sky
            }
            WorldCellThermal.applyVents(air,g);
        tile.configure(true,cap,k,g,filled(1),filled(0));
            tile.fineMaterials(filled(1),air,3);
            list.add(tile);
        }
        var graph=new AdaptiveThermalGraph(list);
        graph.setRoomAirMixing(20);
        graph.setMixingSweeps(12);
        int[] spots={14,16,18,15,17,13,19,12,20,11,21};
        for(int step=0;step<3600;step++){
            graph.clearPower();
            for(int i=0;i<torches;i++){
                int gx=spots[i],gz=14+i%3;
                graph.addPower(list.get((gx>>3)+4*(gz>>3)),AdaptiveThermalTile.index(gx&7,1,gz&7),
                        TORCH_POWER);
            }
            graph.step(.5,-2.2);
        }
        double near=0;int nearN=0,farN=0;double far=0;
        for(int gz=0;gz<SIZE;gz++)for(int gx=0;gx<SIZE;gx++){
            var t=list.get((gx>>3)+4*(gz>>3));
            double v=t.temperatures.get(AdaptiveThermalTile.index(gx&7,1,gz&7));
            if(Math.abs(gx-16)<=2&&Math.abs(gz-15)<=2){near+=v;nearN++;}      // at the torches
            else if(Math.abs(gx-16)>=10){far+=v;farN++;}                     // ten-plus blocks away
        }
        var out=new HashMap<String,Double>();
        out.put("near",near/nearN);out.put("far",far/farN);
        return out;
    }

    @Test public void aTorchDoesNotWarmTheWholeField(){
        var eight=run(8);
        System.out.println(String.format(Locale.ROOT,
            "Outdoors, 8 torches: air at the torches=%.2f C, ten blocks away=%.2f C (outside -2.2)",
            eight.get("near"),eight.get("far")));
        assertTrue("air ten blocks from a torch must not be heated by degrees, was "
                + eight.get("far"), eight.get("far") < 0.5);
    }

    @Test public void aTorchStillWarmsItsOwnBlock(){
        var eight=run(8);
        assertTrue("a torch must heat the air it stands in, was "+eight.get("near"),
                eight.get("near") > -2.2);
    }
}
