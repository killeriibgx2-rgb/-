package com.eternalsnow.thermal.world;

import com.eternalsnow.thermal.engine.Material;
import com.eternalsnow.thermal.engine.Materials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Minecraft block → physical material and openings. */
public final class MaterialMapper {
    private MaterialMapper() {
    }

    public static Material of(BlockState state, LevelReader level, BlockPos pos) {
        if (state.isAir()) return Materials.AIR;
        Block block = state.getBlock();

        // Open doors are air; a closed door is a real leaf, not a hole.
        if (block instanceof DoorBlock) {
            if (isOpen(state)) return Materials.AIR;
            return state.is(BlockTags.WOODEN_DOORS) ? Materials.WOOD : Materials.STEEL;
        }
        if (block instanceof TrapDoorBlock) {
            if (isOpen(state)) return Materials.AIR;
            return state.is(BlockTags.WOODEN_TRAPDOORS) ? Materials.WOOD : Materials.STEEL;
        }
        if (block instanceof FenceGateBlock) {
            if (isOpen(state)) return Materials.AIR;
            return Materials.WOOD;
        }

        if (block == Blocks.WATER) return Materials.WATER;
        if (isIce(block)) return Materials.ICE;
        if (block == Blocks.SNOW_BLOCK || block == Blocks.POWDER_SNOW) return Materials.SNOW;
        if (isGlass(block)) return Materials.GLASS;
        if (block == Blocks.IRON_BLOCK || block == Blocks.GOLD_BLOCK
                || block == Blocks.COPPER_BLOCK || block == Blocks.NETHERITE_BLOCK
                || block == Blocks.IRON_BARS) {
            return Materials.STEEL;
        }
        if (state.is(BlockTags.WOOL) || state.is(BlockTags.WOOL_CARPETS)) return Materials.WOOL;
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
        if (block == Blocks.BRICKS || state.is(BlockTags.TERRACOTTA)) return Materials.BRICK;
        if (state.is(BlockTags.STONE_BUTTONS)) return Materials.STONE;

        // Torches, plants and similarly light, passable decoration add no block-scale mass.
        if (!state.isSolidRender(level, pos)) return Materials.AIR;
        // Unknown solid blocks are not treated as perfect insulators.
        return Materials.STONE;
    }

    /** Open doors are the first Minecraft aperture connected to the leak model. */
    public static boolean isOpenDoor(BlockState state) {
        return state.getBlock() instanceof DoorBlock && isOpen(state);
    }

    private static boolean isOpen(BlockState state) {
        return state.hasProperty(BlockStateProperties.OPEN)
                && state.getValue(BlockStateProperties.OPEN);
    }

    private static boolean isIce(Block block) {
        if (block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE) return true;
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        return "minecraft".equals(key.getNamespace()) && "frosted_ice".equals(key.getPath());
    }

    private static boolean isGlass(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        if (!"minecraft".equals(key.getNamespace())) return false;
        String path = key.getPath();
        return path.equals("glass") || path.equals("tinted_glass") || path.equals("glass_pane")
                || path.endsWith("_stained_glass") || path.endsWith("_stained_glass_pane");
    }
}
