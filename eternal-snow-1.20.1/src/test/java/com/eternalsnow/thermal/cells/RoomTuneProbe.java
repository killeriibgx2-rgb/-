package com.eternalsnow.thermal.cells;

import java.util.*;
import org.junit.Test;

/**
 * Diagnostic probe: finds which parameter actually decides room temperature.
 * Prints a table; asserts nothing about balance. Removed once values are chosen.
 */
public class RoomTuneProbe {
    private static final java.util.List<String> LINES=new java.util.ArrayList<>();
    private static void say(String line){
        System.out.println(line);LINES.add(line);
        try{
            var file=java.nio.file.Path.of("release","thermal-trace.txt").toAbsolutePath();
            java.nio.file.Files.createDirectories(file.getParent());
            java.nio.file.Files.write(file,LINES);
        }catch(Exception ignored){}
    }
    private static double[] filled(double v){double[] a=new double[512];Arrays.fill(a,v);return a;}

    private static List<AdaptiveThermalTile> box(int tx,int ty,int tz,double wallK,double wallC,double airMix){
        int sx=tx*8,sy=ty*8,sz=tz*8;var list=new ArrayList<AdaptiveThermalTile>();
        for(int a=0;a<ty;a++)for(int b=0;b<tz;b++)for(int c=0;c<tx;c++){
            var tile=new AdaptiveThermalTile(c*8,a*8,b*8,new TileTemperatureState(filled(-20)));
            double[] cap=filled(1),k=filled(1),g=filled(0);boolean[] air=new boolean[512];
            for(int y=0;y<8;y++)for(int z=0;z<8;z++)for(int x=0;x<8;x++){
                int gx=c*8+x,gy=a*8+y,gz=b*8+z;
                boolean shell=gx==0||gy==0||gz==0||gx==sx-1||gy==sy-1||gz==sz-1;
                int i=AdaptiveThermalTile.index(x,y,z);
                if(shell){cap[i]=wallC*20;k[i]=wallK;g[i]=Math.min(1,wallK);}else air[i]=true;
            }
            tile.configure(true,cap,k,g,filled(1),filled(0));
            tile.fineMaterials(filled(1),air,airMix);
            list.add(tile);
        }
        return list;
    }

    /** @return {mean of all cells at hour 1, mean at hour 2, seconds to reach 0 C, mean at end} */
    private static double[] run(int tx,int ty,int tz,double wallK,double wallC,double airMix,
                                double watts,double flame,double ambient){
        return run(tx,ty,tz,wallK,wallC,airMix,watts,flame,ambient,true);
    }
    /** flameBoundary=false injects watts directly, the way the 2.12 code did. */
    private static double[] run(int tx,int ty,int tz,double wallK,double wallC,double airMix,
                                double watts,double flame,double ambient,boolean flameBoundary){
        var tiles=box(tx,ty,tz,wallK,wallC,airMix);
        var graph=new AdaptiveThermalGraph(tiles);
        graph.setRoomAirMixing(airMix);
        if(flameBoundary)graph.setSourceAirCap((float)flame);
        else graph.setSourceAirCap(1e6f);
        var src=tiles.get(0);int cell=AdaptiveThermalTile.index(4,1,4);
        double at1h=Double.NaN,at2h=Double.NaN,zero=Double.NaN;
        for(int step=1;step<=1800;step++){
            graph.clearPower();graph.addPower(src,cell,watts);graph.step(.5,ambient);
            double mean=0;int n=0;
            for(var t:tiles)for(int i=0;i<512;i++){mean+=t.temperatures.get(i);n++;}
            mean/=n;
            if(Double.isNaN(zero)&&mean>=0)zero=step/2.0;
            if(step==120)at1h=mean;
            if(step==600)at2h=mean;    // 5 min
            if(step==1800)zero=mean;   // 15 min reused as "final"
        }
        return new double[]{at1h,at2h,zero,at2h};
    }

    @Test public void probe() {
        System.out.println("=== PROBE room tempering (outside -20 C, campfire) ===");
        System.out.println("wallK scale | airMix | room   | 1h     | 2h     | reached 0C");
        for(double leak:new double[]{1,0.2,0.1,0.05,0.033}){
            for(double mix:new double[]{3,20}){
                for(int[] size:new int[][]{{1,1,1},{2,1,2}}){
                    var r=run(size[0],size[1],size[2],0.42*leak,5.0,mix,68*60,60,-20);
                    String room=size[0]+"x"+size[1]+"x"+size[2];
                    System.out.println(String.format(Locale.ROOT,
                        "leak x%5.3f | mix%3.0f | %-8s| %6.2f | %6.2f | %s",
                        leak,mix,room,r[0],r[1],Double.isNaN(r[2])?"never":String.format(Locale.ROOT,"%.0fs",r[2])));
                }
            }
        }
    }

    /** Grid over leak, wall heat capacity and source strength; reports mean at 1/5/15 min. */
    @Test public void grid() {
        System.out.println("=== GRID: 3x2x3 tiles = 24x16x24 brick room, outside -20 C ===");
        System.out.println("leak  wallC  watts   | 1min   5min   15min");
        for(double leak:new double[]{0.3,0.12,0.06,0.03}){
            for(double wallC:new double[]{5.0,2.5,1.0}){
                for(double watts:new double[]{68*60,68*200}){
                    var r=run(3,2,3,0.42*leak,wallC,20,watts,60,-20);
                    System.out.println(String.format(Locale.ROOT,
                        "%5.3f %5.1f %7.0f | %6.2f %6.2f %6.2f",
                        leak,wallC,watts,r[0],r[1],r[2]));
                }
            }
        }
    }

    @Test public void directPower() {
        System.out.println("=== DIRECT WATTS vs FLAME BOUNDARY, 1h warm-up ===");
        System.out.println("mode    leak  mix  room    watts    1h      final");
        for(boolean fb:new boolean[]{true,false}){
            for(double leak:new double[]{0.1,0.03}){
                for(double mix:new double[]{20,200}){
                    for(double w:new double[]{68,68*60}){
                        var r=run(1,1,1,0.42*leak,5.0,mix,w,60,-20,fb);
                        System.out.println(String.format(Locale.ROOT,
                            "%-7s %4.2f %4.0f 8x8x8 %8.0f %6.2f %6.2f",
                            fb?"flame":"watts",leak,mix,w,r[1],r[2]));
                    }
                }
            }
        }
    }

    @Test public void converge() {
        say("=== CONVERGE: 8x8x8 brick, outside -20C, campfire ===");
        say("leakDiv power cap | 1min   5min   15min");
        for(double div:new double[]{30,100,300}){
            for(double pmul:new double[]{1,5,25}){
                for(double cap:new double[]{60,150}){
                    var r=run(1,1,1,0.42/div,5.0,20,68*pmul,cap,-20,true);
                    say(String.format(Locale.ROOT,
                        "%7.0f %5.0f %4.0f | %6.2f %6.2f %6.2f",div,68*pmul,cap,r[0],r[1],r[2]));
                }
            }
        }
    }
}
