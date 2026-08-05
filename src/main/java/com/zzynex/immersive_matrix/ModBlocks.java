package com.zzynex.immersive_matrix;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ImmersiveMatrix.MOD_ID);

    // Регистрация самих блоков в реестре игры
    public static final RegistryObject<Block> MATRIX_DOOR_BLOCK = BLOCKS.register("matrix_door", MatrixDoorBlock::new);

    public static final RegistryObject<Block> BLACK = BLOCKS.register("black",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5.0F).sound(SoundType.STONE)));

    public static final RegistryObject<Block> GLITCH_BLOCK = BLOCKS.register("glitch_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F).sound(SoundType.METAL).noLootTable()));

    public static final RegistryObject<Block> BLU = BLOCKS.register("blu",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(3.0F).sound(SoundType.GLASS).lightLevel((state) -> 15)));

    public static final RegistryObject<Block> RED = BLOCKS.register("red",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.GLASS).lightLevel((state) -> 15)));

    public static final RegistryObject<Block> WHITE = BLOCKS.register("white",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(3.0F).sound(SoundType.GLASS).lightLevel((state) -> 15)));

    public static final RegistryObject<Block> YELLOW = BLOCKS.register("yellow",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(3.0F).sound(SoundType.GLASS).lightLevel((state) -> 15)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    // ИСПРАВЛЕНО: Каждый предмет теперь привязан СТРОГО к своему блоку! Никаких ложных дубликатов двери!
    public static void registerBlockItems(DeferredRegister<Item> itemsRegister) {
        itemsRegister.register("matrix_door", () -> new BlockItem(MATRIX_DOOR_BLOCK.get(), new Item.Properties()));
        itemsRegister.register("black", () -> new BlockItem(BLACK.get(), new Item.Properties()));
        itemsRegister.register("glitch_block", () -> new BlockItem(GLITCH_BLOCK.get(), new Item.Properties()));
        itemsRegister.register("blu", () -> new BlockItem(BLU.get(), new Item.Properties()));
        itemsRegister.register("red", () -> new BlockItem(RED.get(), new Item.Properties()));
        itemsRegister.register("white", () -> new BlockItem(WHITE.get(), new Item.Properties()));
        itemsRegister.register("yellow", () -> new BlockItem(YELLOW.get(), new Item.Properties()));
    }
}
