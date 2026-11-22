package com.eternalsnow.registry;

import com.eternalsnow.EternalSnowMod;
import com.eternalsnow.item.ThermalScannerItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Item registry for the engine's tools. */
public final class ModItems {
    private ModItems() {
    }

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EternalSnowMod.MOD_ID);

    public static final RegistryObject<Item> THERMAL_SCANNER = ITEMS.register("thermal_scanner",
            () -> new ThermalScannerItem(new Item.Properties()));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModItems::addCreativeTabItems);
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(THERMAL_SCANNER.get());
        }
    }
}
