package com.eternalsnow.thermal.world;

import com.eternalsnow.thermal.engine.Material;
import com.eternalsnow.thermal.engine.Materials;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.BlockPos;

/**
 * Minecraft block → physical material.
 *
 * <p>This is the only place where game content meets the SI model, so it is
 * kept boring and explicit. Unknown solid blocks fall back to stone: being
 * wrong in the "heavier than it looks" direction is safer than pretending an
 * unknown block is a perfect insulator.
 */
public final class MaterialMapper {
    private MaterialMapper() {
    }

    public static Material of(BlockState state, LevelReader level, BlockPos pos) {
        if (state.isAir()) return Materials.AIR;
        Block block = state.getBlock();

        if (block == Blocks.WATER || block == Blocks.ICE || block == Blocks.BLUE_ICE
                || block == Blocks.PACKED_ICE) {
            return Materials.ICE;
        }
        if (block == Blocks.SNOW_BLOCK || block == Blocks.POWDER_SNOW) {
            return Materials.SNOW;
        }
        if (block == Blocks.GLASS || block == Blocks.TINTED_GLASS
                || state.is(BlockTags.IMPERMEABLE)) {
            return Materials.GLASS;
        }
        if (block == Blocks.IRON_BLOCK || block == Blocks.GOLD_BLOCK
                || block == Blocks.COPPER_BLOCK || block == Blocks.NETHERITE_BLOCK
                || block == Blocks.IRON_BARS) {
            return Materials.STEEL;
        }
        if (state.is(BlockTags.WOOL) || state.is(BlockTags.WOOL_CARPETS)) {
            return Materials.WOOL;
        }
        if (state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS)
                || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.FENCES) || state.is(BlockTags.WOODEN_FENCES)
                || state.is(BlockTags.LEAVES)) {
            return Materials.WOOD;
        }
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.STONE_BRICKS)
                || block == Blocks.COBBLESTONE || block == Blocks.DEEPSLATE
                || block == Blocks.OBSIDIAN) {
            return Materials.STONE;
        }
        if (block == Blocks.BRICKS || state.is(BlockTags.TERRACOTTA)) {
            return Materials.BRICK;
        }
        if (state.is(BlockTags.STONE_BUTTONS)) {
            return Materials.STONE;
        }
        // Passable-but-not-air blocks (torches, flowers, carpets) behave like air
        // thermally; they have no meaningful mass against a cubic metre.
        if (!state.isSolidRender(level, pos)) {
            return Materials.AIR;
        }
        return Materials.STONE;
    }

    /** True when the block currently lets air through in a way we model as a leak. */
    public static boolean isOpenAperture(BlockState state, LevelReader level, BlockPos pos) {
        if (state.isAir()) return true;
        if (state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN)) {
            return true;
        }
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return false;
        }
        return !state.isSolidRender(level, pos) && state.getFluidState().isEmpty();
    }
}
