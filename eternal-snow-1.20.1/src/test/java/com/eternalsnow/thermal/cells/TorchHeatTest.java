package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Owner's report: seven to eleven torches in a basement gave +150 C, and outdoors the ground
 * around torches warmed to +3 C while the air was -2.2 C.
 *
 * <p>Both are one problem seen from two sides: heat has no way out. In a basement the only sink
 * is the weather conductance, and for air it is small. The report also says the basement is
 * <em>warm everywhere</em>, so mixing is doing its job and the energy balance is the fault.
 */
public class TorchHeatTest {

    private static final double AIR_CAPACITY = 0.02;
    /** One torch as the world adapter injects it: heatTorch 7 x roomSourcePower 10. */
    private static final double TORCH_POWER = 70;
    private static final int W = 16, H = 8, D = 16;

    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    private static int localIndex(int x,int y,int z){return AdaptiveThermalTile.index(x&7,y,z&7);}
    private static int tileIndex(int x,int z){return (x>>3)+2*(z>>3);}

    private static List<AdaptiveThermalTile> build(double wallK,double wallC,double groundC,
                                                   boolean floorOpen){
        var list=new ArrayList<AdaptiveThermalTile>();
        for(int tz=0;tz<D/8;tz++)for(int tx=0;tx<W/8;tx++){
            var tile=new AdaptiveThermalTile(tx*8,0,tz*8,new TileTemperatureState(filled(-2.2)));
            double[] cap=filled(1),k=filled(1),g=filled(0);
            boolean[] air=new boolean[512];
            for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
                int gx=tx*8+x,gz=tz*8+z;
                int i=AdaptiveThermalTile.index(x,y,z);
                boolean edge=gx==0||gz==0||gx==W-1||gz==D-1;
                if(y==H-1){cap[i]=wallC*20;k[i]=wallK;g[i]=Math.min(1,wallK);}
                else if(y==0){cap[i]=groundC;k[i]=1;g[i]=floorOpen?1:0;}
                else if(edge){cap[i]=wallC*20;k[i]=wallK;g[i]=Math.min(1,wallK);}
                else{air[i]=true;cap[i]=AIR_CAPACITY;}
            }
            tile.configure(true,cap,k,g,filled(1),filled(0));
            tile.fineMaterials(filled(1),air,3);
            list.add(tile);
        }
        return list;
    }

    /** @return {mean air, peak air, mean floor} after 30 simulated minutes. */
    static double[] warm(int torches,double wallK,double wallC,double groundC,boolean floorOpen,
                         double ambient){
        var tiles=build(wallK,wallC,groundC,floorOpen);
        var graph=new AdaptiveThermalGraph(tiles);
        graph.setRoomAirMixing(20);
        graph.setMixingSweeps(12);
        int[][] spots={{3,3},{11,3},{3,11},{11,11},{7,7},{6,6},{9,9},{4,8},{8,4},{12,12},{5,5}};
        for(int step=0;step<3600;step++){
            graph.clearPower();
            for(int i=0;i<torches&&i<spots.length;i++){
                int gx=spots[i][0],gz=spots[i][1];
                graph.addPower(tiles.get(tileIndex(gx,gz)),localIndex(gx,1,gz),TORCH_POWER);
            }
            graph.step(.5,ambient);
        }
        double airSum=0,floorSum=0,peak=-1e9;int airN=0,floorN=0;
        for(var t:tiles)for(int i=0;i<512;i++){
            boolean isAir=t.air!=null&&t.air[i];
            if(isAir){double v=t.temperatures.get(i);airSum+=v;airN++;peak=Math.max(peak,v);}
            if(y0(t,i)){floorSum+=t.temperatures.get(i);floorN++;}
        }
        return new double[]{airSum/airN,peak,floorN==0?0:floorSum/floorN};
    }

    private static boolean y0(AdaptiveThermalTile t,int i){return (i>>6)==0&&!(t.air!=null&&t.air[i]);}

    @Test public void aBasementWithTorchesStaysHabitable(){
        double[] r=warm(8,5.0,5.0,4.0,false,-2.2);
        System.out.println(String.format(Locale.ROOT,
            "Basement, 8 torches: air mean=%.1f C, peak=%.1f C, floor=%.1f C",r[0],r[1],r[2]));
        assertTrue("a cellar with torches must not reach +150 C, peak was "+r[1],r[1]<60);
        assertTrue("but it must stay warmer than the weather, mean "+r[0],r[0]>-2.2);
    }

    @Test public void moreTorchesAreWarmerNotCooler(){
        double eight=warm(8,5.0,5.0,4.0,false,-2.2)[0];
        double eleven=warm(11,5.0,5.0,4.0,false,-2.2)[0];
        System.out.println(String.format(Locale.ROOT,
            "Basement 8 vs 11 torches: mean %.1f vs %.1f C",eight,eleven));
        assertTrue("more torches must not cool the room",eleven>=eight-0.01);
    }

    @Test public void openGroundDoesNotCookNextToATorch(){
        double[] out=warm(1,0.05,1.0,4.0,true,-2.2);
        System.out.println(String.format(Locale.ROOT,
            "Outdoor patch, 1 torch: air mean=%.1f C, peak=%.1f C, ground=%.1f C",
            out[0],out[1],out[2]));
        assertTrue("air far from a torch must not be warmed by degrees outdoors, mean "+out[0],
                out[0]<1.0);
    }
}
