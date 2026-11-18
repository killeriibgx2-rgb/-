package com.eternalsnow.registry;

import com.eternalsnow.block.InsulatedDoorBlock;
import com.eternalsnow.block.InsulatedTrapDoorBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraftforge.registries.RegistryObject;
import java.util.ArrayList;
import java.util.List;

public final class InsulatedRegistry {
    private InsulatedRegistry() {}
    public static final List<RegistryObject<Item>> ITEMS = new ArrayList<>();
    public static void init() {} // force registration before the register events
    private static void add(String name, Block original, BlockSetType type, boolean door) {
        RegistryObject<Block> block = ModRegistry.BLOCKS.register("insulated_" + name,
                () -> door ? new InsulatedDoorBlock(BlockBehaviour.Properties.copy(original), type)
                           : new InsulatedTrapDoorBlock(BlockBehaviour.Properties.copy(original), type));
        ITEMS.add(ModRegistry.ITEMS.register("insulated_" + name,
                () -> door ? new DoubleHighBlockItem(block.get(), new Item.Properties())
                           : new BlockItem(block.get(), new Item.Properties())));
    }
    static {
        add("oak_door", Blocks.OAK_DOOR, BlockSetType.OAK, true);
        add("oak_trapdoor", Blocks.OAK_TRAPDOOR, BlockSetType.OAK, false);
        add("spruce_door", Blocks.SPRUCE_DOOR, BlockSetType.SPRUCE, true);
        add("spruce_trapdoor", Blocks.SPRUCE_TRAPDOOR, BlockSetType.SPRUCE, false);
        add("birch_door", Blocks.BIRCH_DOOR, BlockSetType.BIRCH, true);
        add("birch_trapdoor", Blocks.BIRCH_TRAPDOOR, BlockSetType.BIRCH, false);
        add("jungle_door", Blocks.JUNGLE_DOOR, BlockSetType.JUNGLE, true);
        add("jungle_trapdoor", Blocks.JUNGLE_TRAPDOOR, BlockSetType.JUNGLE, false);
        add("acacia_door", Blocks.ACACIA_DOOR, BlockSetType.ACACIA, true);
        add("acacia_trapdoor", Blocks.ACACIA_TRAPDOOR, BlockSetType.ACACIA, false);
        add("dark_oak_door", Blocks.DARK_OAK_DOOR, BlockSetType.DARK_OAK, true);
        add("dark_oak_trapdoor", Blocks.DARK_OAK_TRAPDOOR, BlockSetType.DARK_OAK, false);
        add("mangrove_door", Blocks.MANGROVE_DOOR, BlockSetType.MANGROVE, true);
        add("mangrove_trapdoor", Blocks.MANGROVE_TRAPDOOR, BlockSetType.MANGROVE, false);
        add("cherry_door", Blocks.CHERRY_DOOR, BlockSetType.CHERRY, true);
        add("cherry_trapdoor", Blocks.CHERRY_TRAPDOOR, BlockSetType.CHERRY, false);
        add("bamboo_door", Blocks.BAMBOO_DOOR, BlockSetType.BAMBOO, true);
        add("bamboo_trapdoor", Blocks.BAMBOO_TRAPDOOR, BlockSetType.BAMBOO, false);
        add("crimson_door", Blocks.CRIMSON_DOOR, BlockSetType.CRIMSON, true);
        add("crimson_trapdoor", Blocks.CRIMSON_TRAPDOOR, BlockSetType.CRIMSON, false);
        add("warped_door", Blocks.WARPED_DOOR, BlockSetType.WARPED, true);
        add("warped_trapdoor", Blocks.WARPED_TRAPDOOR, BlockSetType.WARPED, false);
        add("iron_door", Blocks.IRON_DOOR, BlockSetType.IRON, true);
        add("iron_trapdoor", Blocks.IRON_TRAPDOOR, BlockSetType.IRON, false);
    }
}
