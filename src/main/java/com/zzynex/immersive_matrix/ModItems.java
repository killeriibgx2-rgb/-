package com.zzynex.immersive_matrix;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ImmersiveMatrix.MOD_ID);

    public static void register(IEventBus eventBus) {
        // Принудительно вызываем сшивку предметов и блоков из ModBlocks
        ModBlocks.registerBlockItems(ITEMS);

        ITEMS.register(eventBus);
    }
}
