package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Owner's report: a 5x5x3 plank room at -12 C, one campfire in the middle.
 * <ul>
 *   <li>it warms too slowly;</li>
 *   <li>breaking one ceiling block makes the air under the hole outdoor temperature at once;</li>
 *   <li>when the fire goes out the whole room is at outdoor temperature immediately, and back
 *       again the instant it is relit.</li>
 * </ul>
 * A room must have inertia — minutes to warm, many minutes to cool — and opening one ceiling
 * block must not teleport the weather into the room.
 */
public class RoomInertiaTest {

    private static final double OUTSIDE = -12;
    private static final double CAMPFIRE = 680;     // heatCampfire 68 x roomSourcePower 10
    private static final double AIR = WorldCellThermal.AIR_HEAT_CAPACITY;

    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    /**
     * 5x5x3 interior with a one-block plank shell, standing in the ground. Couplings come from
     * the real rule, so this test fails if the rule changes back.
     */
    private static AdaptiveThermalTile room(boolean holeInCeiling){
        var tile=new AdaptiveThermalTile(0,0,0,new TileTemperatureState(filled(OUTSIDE)));
        double[] cap=filled(1),k=filled(1),g=filled(0);
        boolean[] air=new boolean[512];
        for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
            int i=AdaptiveThermalTile.index(x,y,z);
            if(x>=1&&x<=5&&z>=1&&z<=5&&y>=1&&y<=3){air[i]=true;cap[i]=AIR;g[i]=0;}
        }
        double plankG=WorldCellThermal.weatherCoupling(false,true,64,64,2.5);
        for(int y=1;y<=3;y++)for(int z=1;z<=5;z++){
            plank(cap,k,g,0,y,z,plankG);plank(cap,k,g,6,y,z,plankG);
        }
        for(int y=1;y<=3;y++)for(int x=1;x<=5;x++){
            plank(cap,k,g,x,y,0,plankG);plank(cap,k,g,x,y,6,plankG);
        }
        for(int z=1;z<=5;z++)for(int x=1;x<=5;x++){
            plank(cap,k,g,x,0,z,plankG);plank(cap,k,g,x,4,z,plankG);
        }
        // Surrounding soil: a reservoir, coupled at its own (buried) strength.
        double soilG=WorldCellThermal.weatherCoupling(false,false,32,64,8);
        for(int i=0;i<512;i++){
            if(cap[i]!=1||air[i])continue;
            cap[i]=8*20;k[i]=8;g[i]=soilG;
        }
        if(holeInCeiling){
            int i=AdaptiveThermalTile.index(3,4,3);
            air[i]=true;cap[i]=AIR;k[i]=1;
            g[i]=WorldCellThermal.weatherCoupling(true,true,64,64,1);
        }
        tile.configure(true,cap,k,g,filled(1),filled(0));
        tile.fineMaterials(filled(1),air,3);
        return tile;
    }
    private static void plank(double[] cap,double[] k,double[] g,int x,int y,int z,double coupling){
        int i=AdaptiveThermalTile.index(x,y,z);
        cap[i]=2.5*20;k[i]=2.5;g[i]=coupling;
    }

    private static double air(AdaptiveThermalTile t){
        double sum=0;int n=0;
        for(int i=0;i<512;i++)if(t.air[i]){sum+=t.temperatures.get(i);n++;}
        return sum/n;
    }
    private static AdaptiveThermalGraph graph(AdaptiveThermalTile t){
        var g=new AdaptiveThermalGraph(List.of(t));
        g.setRoomAirMixing(20);g.setMixingSweeps(12);
        return g;
    }
    private static void fire(AdaptiveThermalGraph g,AdaptiveThermalTile t,double power,int steps){
        int cell=AdaptiveThermalTile.index(3,1,3);
        for(int s=0;s<steps;s++){g.clearPower();g.addPower(t,cell,power);g.step(.5,OUTSIDE);}
    }
    private static void cool(AdaptiveThermalGraph g,int steps){
        for(int s=0;s<steps;s++){g.clearPower();g.step(.5,OUTSIDE);}
    }

    @Test public void theRoomTakesMinutesToWarm(){
        var t=room(false);var g=graph(t);
        fire(g,t,CAMPFIRE,240);                        // two minutes
        double twoMinutes=air(t);
        fire(g,t,CAMPFIRE,1560);                       // fifteen total
        double fifteenMinutes=air(t);
        System.out.println(String.format(Locale.ROOT,
            "Plank room 5x5x3 at %.0f C: 2 min=%.1f C, 15 min=%.1f C",
            OUTSIDE,twoMinutes,fifteenMinutes));
        assertTrue("the room must be noticeably warmer after two minutes, was "+twoMinutes,
                twoMinutes>OUTSIDE+3);
        assertTrue("and warmer still after fifteen, "+fifteenMinutes,
                fifteenMinutes>twoMinutes+3);
        assertTrue("but a campfire must not cook a plank room, "+fifteenMinutes,
                fifteenMinutes<40);
    }

    @Test public void theRoomKeepsHeatAfterTheFireDies(){
        var t=room(false);var g=graph(t);
        fire(g,t,CAMPFIRE,1800);
        double warm=air(t);
        cool(g,40);                                    // twenty seconds
        double after20s=air(t);
        cool(g,1560);                                  // fifteen minutes total
        double after15min=air(t);
        System.out.println(String.format(Locale.ROOT,
            "Fire out: was %.1f C, 20 s later=%.1f C, 15 min later=%.1f C",
            warm,after20s,after15min));
        assertTrue("twenty seconds after the fire dies the room must still be warm, was "
                + after20s, after20s>warm-2);
        assertTrue("it must keep warming the room for a while, was "+after15min,
                after15min>OUTSIDE+1);
        assertTrue("and it must cool eventually, was "+after15min, after15min<warm);
    }

    @Test public void oneHoleInTheCeilingDoesNotFreezeTheRoom(){
        var closed=room(false);
        var holed=room(true);
        var gc=graph(closed);
        var gh=graph(holed);
        fire(gc,closed,CAMPFIRE,1800);
        fire(gh,holed,CAMPFIRE,1800);
        cool(gh,60);                                   // thirty seconds with the hole open
        double underHole=holed.temperatures.get(AdaptiveThermalTile.index(3,3,3));
        double wholeRoom=air(holed);
        System.out.println(String.format(Locale.ROOT,
            "Hole open: block under it=%.1f C, room mean=%.1f C (closed room was %.1f C)",
            underHole,wholeRoom,air(closed)));
        assertTrue("one missing ceiling block must not teleport the weather inside, was "
                + underHole, underHole>OUTSIDE+5);
        assertTrue("and the rest of the room must stay warm, was "+wholeRoom,
                wholeRoom>OUTSIDE+3);
    }
}
