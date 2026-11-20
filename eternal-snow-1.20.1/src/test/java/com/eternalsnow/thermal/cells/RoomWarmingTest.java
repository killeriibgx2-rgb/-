package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Owner's report: at a campfire the air read about +50 C, one block away it was cold, and a
 * brick room never became habitable.
 *
 * <p>A room is a closed box here — the whole outer layer is wall, and only that layer touches
 * the weather. That is the geometry a player builds.
 */
public class RoomWarmingTest {

    /** Air heat capacity as the world adapter assigns it (WorldCellThermal.AIR_HEAT_CAPACITY). */
    private static final double AIR_CAPACITY = 0.02;

    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    private static List<AdaptiveThermalTile> box(int tx,int ty,int tz,double wallK,double wallC){
        int sx=tx*8,sy=ty*8,sz=tz*8;
        var list=new ArrayList<AdaptiveThermalTile>();
        for(int ty0=0;ty0<ty;ty0++)for(int tz0=0;tz0<tz;tz0++)for(int tx0=0;tx0<tx;tx0++){
            var tile=new AdaptiveThermalTile(tx0*8,ty0*8,tz0*8,new TileTemperatureState(filled(-20)));
            double[] cap=filled(1),k=filled(1),g=filled(0);
            boolean[] air=new boolean[512];
            for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
                int gx=tx0*8+x,gy=ty0*8+y,gz=tz0*8+z;
                boolean shell=gx==0||gy==0||gz==0||gx==sx-1||gy==sy-1||gz==sz-1;
                int i=AdaptiveThermalTile.index(x,y,z);
                if(shell){cap[i]=wallC*20;k[i]=wallK;g[i]=Math.min(1,wallK);}
                else{air[i]=true;cap[i]=AIR_CAPACITY;}
            }
            WorldCellThermal.applyVents(air,g);
        tile.configure(true,cap,k,g,filled(1),filled(0));
            tile.fineMaterials(filled(1),air,3);
            list.add(tile);
        }
        return list;
    }

    /** @return {mean interior temperature after 15 simulated minutes, hottest cell} */
    private static double[] warm(int tx,int ty,int tz,double wallK,double wallC,double power){
        var tiles=box(tx,ty,tz,wallK,wallC);
        var graph=new AdaptiveThermalGraph(tiles);
        graph.setRoomAirMixing(20);
        var source=tiles.get(0);
        int cell=AdaptiveThermalTile.index(4,1,4);
        for(int step=0;step<1800;step++){
            graph.clearPower();
            graph.addPower(source,cell,power);
            graph.step(.5,-20);
        }
        double mean=0,peak=-1e9;int n=0;
        for(var t:tiles)for(int i=0;i<512;i++){
            double v=t.temperatures.get(i);
            if(t==source&&i==cell)continue;
            mean+=v;n++;peak=Math.max(peak,v);
        }
        return new double[]{mean/n,peak};
    }

    @Test public void oneCampfireMakesABrickRoomHabitable() {
        double[] r=warm(1,1,1,0.42,5.0,68*10);
        System.out.println(String.format(Locale.ROOT,
                "8x8x8 closed brick room, one campfire, -20 C outside: after 15 min mean=%.2f C peak=%.2f C",
                r[0],r[1]));
        assertTrue("a campfire must lift the room above freezing, mean "+r[0],r[0]>0);
        assertTrue("air next to the fire must stay breathable, peak "+r[1],r[1]<120);
    }

    @Test public void roomKeepsWarmingAndIsNotStuckAtTheColdEnd() {
        double quick=warm(1,1,1,0.42,5.0,68*10)[0];
        double late=warm(1,1,1,0.42,5.0,68*20)[0];
        assertTrue("more fire must mean a warmer room: "+quick+" !< "+late,late>quick);
    }

    @Test public void insulationMattersMoreThanRoomSize() {
        double brick=warm(1,1,1,0.42,5.0,68*10)[0];
        double wool=warm(1,1,1,0.05,0.8,68*10)[0];
        System.out.println(String.format(Locale.ROOT,"brick mean=%.2f C, wool mean=%.2f C",brick,wool));
        assertTrue("wool must hold heat better than brick: "+wool+" <= "+brick,wool>brick);
    }

    @Test public void bigRoomStaysCoolerThanASmallOne() {
        double small=warm(1,1,1,0.42,5.0,68*10)[0];
        double big=warm(2,1,2,0.42,5.0,68*10)[0];
        assertTrue("16x8x16 must be cooler than 8x8x8: "+big+" !< "+small,big<small);
    }
}
