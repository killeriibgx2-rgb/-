package com.zzynes.fly_drone;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FlyDroneMod.MOD_ID);

    // ИСПРАВЛЕНО: Перевели пульт и батарею на стандартный базовый класс Item, чтобы убрать ошибку Cannot resolve symbol!
    public static final RegistryObject<Item> REMOTE = ITEMS.register("remote",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BATTERY = ITEMS.register("battery",
            () -> new Item(new Item.Properties().durability(200).stacksTo(1)));

    public static final RegistryObject<Item> CI4 = ITEMS.register("ci4",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RPG_SNAR = ITEMS.register("rpg_snar",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COPPER_WIRE = ITEMS.register("copper_wire",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> STAL_MOLOT = ITEMS.register("stal_molot",
            () -> new Item(new Item.Properties().durability(50).stacksTo(1)));

    // РЕСУРСЫ ДЛЯ КРАФТОВ
    public static final RegistryObject<Item> IRON_PLATE = ITEMS.register("iron_plate",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LITHIUM_PLATE = ITEMS.register("lithium_plate",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PROPELLER_FPV = ITEMS.register("propeller_fpv",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MOTOR_FPV = ITEMS.register("motor_fpv",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CAMERA_FPV = ITEMS.register("camera_fpv",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MICROCONTROLLER = ITEMS.register("microcontroller",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SANDWICH_PANEL = ITEMS.register("sandwich_panel",
            () -> new Item(new Item.Properties()));

    // ПРЕДМЕТЫ ДЛЯ НАШИХ БЛОКОВ
    public static final RegistryObject<Item> LITHIUM_ORE = ITEMS.register("lithium_ore",
            () -> new BlockItem(ModBlocks.LITHIUM_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> DEEPSLATE_LITHIUM_ORE = ITEMS.register("deepslate_lithium_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_LITHIUM_ORE.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
