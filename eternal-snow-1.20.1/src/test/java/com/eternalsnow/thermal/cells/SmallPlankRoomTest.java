package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Owner's report: a 5x5x3 plank room at -12 C, one campfire in the middle.
 * <ul>
 *   <li>it warms too slowly;</li>
 *   <li>breaking one ceiling block makes the air under the hole outdoor temperature at once;</li>
 *   <li>when the fire goes out the room drops to outdoor temperature at once, and comes back
 *       when it is lit again.</li>
 * </ul>
 * A room must have inertia: minutes to warm, many minutes to cool, and no jump when the fire
 * changes state.
 */
public class SmallPlankRoomTest {

    private static final double OUTSIDE = -12;
    private static final double CAMPFIRE = 680;     // heatCampfire 68 x roomSourcePower 10

    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    /** 5x5 interior with three blocks of headroom, plank shell, all inside one 8^3 tile. */
    private static AdaptiveThermalTile room(double airCapacity,double wallCoupling){
        var tile=new AdaptiveThermalTile(0,0,0,new TileTemperatureState(filled(OUTSIDE)));
        double[] cap=filled(1),k=filled(1),g=filled(0);
        boolean[] air=new boolean[512];
        for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
            int i=AdaptiveThermalTile.index(x,y,z);
            boolean inside=x>=1&&x<=5&&z>=1&&z<=5&&y>=1&&y<=3;
            if(inside){air[i]=true;cap[i]=airCapacity;}
            else{
                boolean shell=x==0||x==6||z==0||z==6||y==0||y==4;
                if(!shell)continue;                       // solid rock far away: leave it out
                cap[i]=0.24*20;k[i]=2.5;                  // planks
                g[i]=wallCoupling;
            }
        }
        tile.configure(true,cap,k,g,filled(1),filled(0));
        tile.fineMaterials(filled(1),air,3);
        return tile;
    }

    private static double[] curve(double airCapacity,double wallCoupling){
        var tile=room(airCapacity,wallCoupling);
        var graph=new AdaptiveThermalGraph(List.of(tile));
        graph.setRoomAirMixing(20);
        graph.setMixingSweeps(12);
        int fire=AdaptiveThermalTile.index(3,1,3);
        // 15 minutes with the fire, then 30 minutes without it.
        double[] out=new double[4];
        for(int step=0;step<1800;step++){graph.clearPower();graph.addPower(tile,fire,CAMPFIRE);graph.step(.5,OUTSIDE);}
        out[0]=air(tile);                        // right when the fire goes out
        for(int step=0;step<20;step++){graph.clearPower();graph.step(.5,OUTSIDE);}
        out[1]=air(tile);                        // ten seconds later: must not have jumped
        for(int step=0;step<3580;step++){graph.clearPower();graph.step(.5,OUTSIDE);}
        out[2]=air(tile);                        // thirty minutes later
        for(int step=0;step<600;step++){graph.clearPower();graph.addPower(tile,fire,CAMPFIRE);graph.step(.5,OUTSIDE);}
        out[3]=air(tile);                        // five minutes after relighting
        return out;
    }

    private static double air(AdaptiveThermalTile tile){
        double sum=0;int n=0;
        for(int i=0;i<512;i++)if(tile.air[i]){sum+=tile.temperatures.get(i);n++;}
        return sum/n;
    }

    @Test public void report(){
        double[] now=curve(WorldCellThermal.AIR_HEAT_CAPACITY,1.0);
        System.out.println(String.format(Locale.ROOT,
            "ROOM after 15 min fire=%.1f C | 10 s after it dies=%.1f C | 30 min later=%.1f C | 5 min after relight=%.1f C (outside %.0f)",
            now[0],now[1],now[2],now[3],OUTSIDE));
    }
}
