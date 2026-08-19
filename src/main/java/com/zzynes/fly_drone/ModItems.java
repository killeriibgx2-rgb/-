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

    // --- НАШ ДРОН (Предмет) ---
    public static final RegistryObject<Item> FPV_DRONE_ITEM = ITEMS.register("fpv_drone",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // --- ГАДЖЕТЫ И ПУЛЬТЫ ---
    // Наш пульт теперь официально использует класс RemoteItem!
    public static final RegistryObject<Item> REMOTE = ITEMS.register("remote",
            () -> new RemoteItem(new Item.Properties()));

    // --- БАТАРЕИ И ПРОВОДА ---
    public static final RegistryObject<Item> BATTERY = ITEMS.register("battery",
            () -> new Item(new Item.Properties().stacksTo(1).durability(200)));

    public static final RegistryObject<Item> BATTERY_CELL = ITEMS.register("battery_cell",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COPPER_WIRE = ITEMS.register("copper_wire",
            () -> new Item(new Item.Properties()));

    // --- ВЗРЫВЧАТКА И СНАРЯДЫ ---
    public static final RegistryObject<Item> CI4 = ITEMS.register("ci4",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> RPG_SNAR = ITEMS.register("rpg_snar",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // --- ИНСТРУМЕНТЫ ---
    public static final RegistryObject<Item> STAL_MOLOT = ITEMS.register("stal_molot",
            () -> new HammerItem(new Item.Properties()));

    // --- ЛИТИЕВЫЕ МАТЕРИАЛЫ ---
    public static final RegistryObject<Item> LITHIUM_INGOT = ITEMS.register("lithium_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LITHIUM_RUD = ITEMS.register("lithium_rud",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LITHIUM_ORE_ITEM = ITEMS.register("lithium_ore",
            () -> new BlockItem(ModBlocks.LITHIUM_ORE.get(), new Item.Properties()));

    // --- КОМПОНЕНТЫ ДРОНА ---
    public static final RegistryObject<Item> CAMERA_FPV = ITEMS.register("camera_fpv",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COPPER_PLATE = ITEMS.register("copper_plate",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> IRON_PLATE = ITEMS.register("iron_plate",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LITHIUM_PLATE = ITEMS.register("lithium_plate",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MICROCONTROLLER = ITEMS.register("microcontrooler",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MOTOR_CASING = ITEMS.register("motor_casing",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MOTOR_SHAFT = ITEMS.register("motor_shaft",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MOTOR_WINDING = ITEMS.register("motor_winding",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MOTOR_FPV = ITEMS.register("motor_fpv",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PROPELLER_FPV = ITEMS.register("propeller_fpv",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SENDVICH_PANEL = ITEMS.register("sendvich_panel",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
