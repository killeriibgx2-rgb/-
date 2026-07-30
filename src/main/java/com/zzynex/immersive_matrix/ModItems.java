package com.zzynex.immersive_matrix;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    // Создаем регистратор предметов под наш Mod ID
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ImmersiveMatrix.MOD_ID);

    // --- РЕГИСТРАЦИЯ БЛОКОВ КАК ПРЕДМЕТОВ (чтобы их можно было брать в руки) ---

    public static final RegistryObject<Item> BLACK_BLOCK_ITEM = ITEMS.register("black",
            () -> new BlockItem(ModBlocks.BLACK_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> GLITCH_BLOCK_ITEM = ITEMS.register("glitch_block",
            () -> new BlockItem(ModBlocks.GLITCH_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> BLU_BLOCK_ITEM = ITEMS.register("blu",
            () -> new BlockItem(ModBlocks.BLU_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> RED_BLOCK_ITEM = ITEMS.register("red",
            () -> new BlockItem(ModBlocks.RED_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> WHITE_BLOCK_ITEM = ITEMS.register("white",
            () -> new BlockItem(ModBlocks.WHITE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> YELLOW_BLOCK_ITEM = ITEMS.register("yellow",
            () -> new BlockItem(ModBlocks.YELLOW_BLOCK.get(), new Item.Properties()));

    // --- СВЯЗЫВАЕМ ПРЕДМЕТ ДВЕРИ С ЕЁ БЛОКОМ В МИРЕ ---
    // Используем наш кастомный MatrixDoorItem для динамического переименования в инвентаре
    public static final RegistryObject<Item> MATRIX_DOOR = ITEMS.register("matrix_door",
            () -> new MatrixDoorItem(ModBlocks.MATRIX_DOOR_BLOCK.get(), new Item.Properties().stacksTo(1)));

    // Метод для подключения предметов к главной шине мода
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
