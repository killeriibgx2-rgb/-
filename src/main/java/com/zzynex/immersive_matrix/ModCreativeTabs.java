package com.zzynex.immersive_matrix;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ImmersiveMatrix.MOD_ID);

    public static final RegistryObject<CreativeModeTab> IMMERSIVE_MATRIX_TAB = CREATIVE_MODE_TABS.register("immersive_matrix_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(ImmersiveMatrix.MOD_ID, "matrix_door"))))
                    // ЖЕЛЕЗНЫЙ РУСИФИКАТОР: Вшиваем красивое русское название прямо в Java, минуя любые баги кэша JSON ресурсов!
                    .title(Component.literal("Иммерсивная Матрица"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ForgeRegistries.ITEMS.getValue(new ResourceLocation(ImmersiveMatrix.MOD_ID, "matrix_door")));
                        output.accept(ModBlocks.BLACK.get());
                        output.accept(ModBlocks.GLITCH_BLOCK.get());
                        output.accept(ModBlocks.BLU.get());
                        output.accept(ModBlocks.RED.get());
                        output.accept(ModBlocks.WHITE.get());
                        output.accept(ModBlocks.YELLOW.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
