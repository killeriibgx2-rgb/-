package com.eternalsnow.thermal.cells;

import net.minecraft.nbt.CompoundTag;

/** 8^3 retained temperatures plus a uniform coarse update. No elapsed/unloaded time. */
public final class TileTemperatureState {
    public static final int SIDE=8, CELLS=512;
    final double[] base;
    double scale=1, offset;
    private double minimum,maximum;
    private void extrema(){minimum=Double.POSITIVE_INFINITY;maximum=Double.NEGATIVE_INFINITY;for(double t:base){minimum=Math.min(minimum,t);maximum=Math.max(maximum,t);}}
    public TileTemperatureState(double[] temperatures) {
        if(temperatures.length!=CELLS)throw new IllegalArgumentException("Tile size");
        base=temperatures.clone();
        for(double t:base)if(!Double.isFinite(t))throw new IllegalArgumentException("Temperature");extrema();
    }
    public double get(int index) {return base[index]*scale+offset;}
    public void normalize() {
        if(scale==1 && offset==0)return;
        for(int i=0;i<CELLS;i++)base[i]=get(i);
        scale=1;offset=0;extrema();
    }
    public void set(int index,double value) {
        if(!Double.isFinite(value))throw new IllegalArgumentException("Temperature");
        normalize();base[index]=value;extrema();
    }
    /** Preserve C-weighted energy while allowing sub-tile contrasts to relax. */
    public void coarseUpdate(double targetMean,double baseMean,double decay) {
        if(!Double.isFinite(targetMean)||!Double.isFinite(baseMean)||decay<0||decay>1)
            throw new IllegalArgumentException("Coarse update");
        double low=Math.min(minimum*scale+offset,targetMean),high=Math.max(maximum*scale+offset,targetMean);
        double next=scale*decay;
        if(baseMean>minimum)next=Math.min(next,(targetMean-low)/(baseMean-minimum));
        if(maximum>baseMean)next=Math.min(next,(high-targetMean)/(maximum-baseMean));
        scale=Math.max(0,next);offset=targetMean-baseMean*scale;
    }
    void refreshExtrema(){extrema();}
    public CompoundTag save() {
        CompoundTag tag=new CompoundTag();long[] bits=new long[CELLS];
        for(int i=0;i<CELLS;i++)bits[i]=Double.doubleToLongBits(base[i]);
        tag.putLongArray("base",bits);tag.putDouble("scale",scale);tag.putDouble("offset",offset);
        return tag;
    }
    public static TileTemperatureState load(CompoundTag tag) {
        long[] bits=tag.getLongArray("base");
        if(bits.length!=CELLS)throw new IllegalArgumentException("Tile size");
        double[] temperatures=new double[CELLS];
        for(int i=0;i<CELLS;i++)temperatures[i]=Double.longBitsToDouble(bits[i]);
        var result=new TileTemperatureState(temperatures);
        result.scale=tag.getDouble("scale");result.offset=tag.getDouble("offset");
        if(!Double.isFinite(result.scale)||result.scale<0||result.scale>1||!Double.isFinite(result.offset))
            throw new IllegalArgumentException("Tile transform");
        return result;
    }
}
