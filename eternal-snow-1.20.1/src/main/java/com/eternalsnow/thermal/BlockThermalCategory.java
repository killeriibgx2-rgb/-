package com.eternalsnow.thermal;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

public enum BlockThermalCategory {
    WOOL("wool", 0.05F, 0.8F),
    LOG("log", 0.12F, 5.5F),
    PLANKS("planks", 0.24F, 2.5F),
    SNOW("snow", 0.18F, 1.5F),
    EARTH("earth", 0.38F, 4.0F),
    BRICK("brick", 0.42F, 5.0F),
    STONE("stone", 0.72F, 8.0F),
    GLASS("glass", 0.90F, 1.2F),
    METAL("metal", 0.95F, 6.0F);

    public static final BlockThermalCategory[] VALUES = values();

    public final String id;
    public final float defaultConductivity;
    public final float defaultCapacity;

    BlockThermalCategory(String id, float defaultConductivity, float defaultCapacity) {
        this.id = id;
        this.defaultConductivity = defaultConductivity;
        this.defaultCapacity = defaultCapacity;
    }

    public String getTranslationKey() {
        return "eternalsnow.thermal.cat." + this.id;
    }

    public static BlockThermalCategory classify(BlockState state) {
        if (state.is(BlockTags.WOOL) || state.is(BlockTags.WOOL_CARPETS) || state.is(Blocks.HAY_BLOCK)) {
            return WOOL;
        }
        if (state.is(BlockTags.LOGS)) {
            return LOG;
        }
        if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock) {
            return state.is(Blocks.IRON_DOOR) || state.is(Blocks.IRON_TRAPDOOR) ? METAL : PLANKS;
        }
        if (state.is(BlockTags.PLANKS)
                || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.WOODEN_FENCES)
                || state.is(Blocks.BOOKSHELF)
                || state.is(Blocks.BARREL)
                || state.is(Blocks.CRAFTING_TABLE)
                || state.is(Blocks.CHEST)) {
            return PLANKS;
        }
        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)) {
            return SNOW;
        }
        if (state.getBlock() instanceof AbstractGlassBlock
                || state.getBlock() instanceof StainedGlassPaneBlock
                || state.getBlock() instanceof HalfTransparentBlock
                || state.is(BlockTags.ICE)
                || state.is(Blocks.GLASS_PANE)) {
            return GLASS;
        }
        if (state.is(Blocks.IRON_BLOCK)
                || state.is(Blocks.COPPER_BLOCK)
                || state.is(Blocks.EXPOSED_COPPER)
                || state.is(Blocks.WEATHERED_COPPER)
                || state.is(Blocks.OXIDIZED_COPPER)
                || state.is(Blocks.CUT_COPPER)
                || state.is(Blocks.GOLD_BLOCK)
                || state.is(Blocks.NETHERITE_BLOCK)
                || state.is(Blocks.CHAIN)
                || state.getBlock() instanceof IronBarsBlock
                || state.is(BlockTags.ANVIL)) {
            return METAL;
        }
        if (state.is(Blocks.BRICKS)
                || state.is(Blocks.BRICK_SLAB)
                || state.is(Blocks.BRICK_STAIRS)
                || state.is(Blocks.MUD_BRICKS)
                || state.is(Blocks.NETHER_BRICKS)
                || state.is(BlockTags.TERRACOTTA)) {
            return BRICK;
        }
        if (state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.MUD)) {
            return EARTH;
        }
        return STONE;
    }
}
