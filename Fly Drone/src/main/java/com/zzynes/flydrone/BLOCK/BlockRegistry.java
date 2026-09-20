package com.zzynes.flydrone.BLOCK;

import com.zzynes.flydrone.DRONE.FlyDroneMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockRegistry {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FlyDroneMod.MOD_ID);

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, FlyDroneMod.MOD_ID);

    public static final RegistryObject<Item> CLOTH_WHITE = ITEMS.register("cloth_white",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CLOTH_GREEN = ITEMS.register("cloth_green",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CLOTH_SAND = ITEMS.register("cloth_sand",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Block> SANDBAG_DIRT_GREEN = BLOCKS.register("sandbag_dirt_green",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIRT).sound(SoundType.GRAVEL).noOcclusion()));

    public static final RegistryObject<Block> SANDBAG_SAND_GREEN = BLOCKS.register("sandbag_sand_green",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.SAND).sound(SoundType.SAND).noOcclusion()));

    public static final RegistryObject<Block> SANDBAG_DIRT_SAND = BLOCKS.register("sandbag_dirt_sand",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIRT).sound(SoundType.GRAVEL).noOcclusion()));

    public static final RegistryObject<Block> SANDBAG_SAND_SAND = BLOCKS.register("sandbag_sand_sand",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.SAND).sound(SoundType.SAND).noOcclusion()));

    public static final RegistryObject<Item> SANDBAG_DIRT_GREEN_ITEM = ITEMS.register("sandbag_dirt_green",
            () -> new BlockItem(SANDBAG_DIRT_GREEN.get(), new Item.Properties()));

    public static final RegistryObject<Item> SANDBAG_SAND_GREEN_ITEM = ITEMS.register("sandbag_sand_green",
            () -> new BlockItem(SANDBAG_SAND_GREEN.get(), new Item.Properties()));

    public static final RegistryObject<Item> SANDBAG_DIRT_SAND_ITEM = ITEMS.register("sandbag_dirt_sand",
            () -> new BlockItem(SANDBAG_DIRT_SAND.get(), new Item.Properties()));

    public static final RegistryObject<Item> SANDBAG_SAND_SAND_ITEM = ITEMS.register("sandbag_sand_sand",
            () -> new BlockItem(SANDBAG_SAND_SAND.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        BLOCKS.register(eventBus);
    }
}