package com.zzynes.fly_drone;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FlyDroneMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> FLY_DRONE_TAB = CREATIVE_MODE_TABS.register("fly_drone_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("§bFly Drone FPV"))
                    .icon(() -> new ItemStack(ModItems.REMOTE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.REMOTE.get());
                        output.accept(ModItems.BATTERY.get());
                        output.accept(ModItems.CI4.get());
                        output.accept(ModItems.RPG_SNAR.get());
                        output.accept(ModItems.COPPER_WIRE.get());
                        output.accept(ModItems.STAL_MOLOT.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
