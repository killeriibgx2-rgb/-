package com.eternalsnow.block;

import net.minecraft.world.level.block.Block;

/**
 * v2.6.0: a cheap insulating panel. Classified as WOOL by BlockThermalCategory, so placing it
 * over a window/glass wall adds a real series thermal resistance and lengthens the room's tau.
 */
public class ThermalShutterBlock extends Block {
    public ThermalShutterBlock(Properties properties) {
        super(properties);
    }
}
