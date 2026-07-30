package com.zzynex.immersive_matrix;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    // Создаем регистратор для вкладок творческого режима под наш Mod ID
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ImmersiveMatrix.MOD_ID);

    // Регистрируем саму вкладку Immersive Matrix
    public static final RegistryObject<CreativeModeTab> IMMERSIVE_MATRIX_TAB = CREATIVE_MODE_TABS.register("immersive_matrix_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.MATRIX_DOOR.get())) // Иконкой вкладки будет наша дверь
                    .title(Component.translatable("creativetab.immersive_matrix_tab")) // Ссылка на перевод названия
                    .displayItems((itemDisplayParameters, output) -> {
                        // Добавляем предметы во вкладку строго в нужном нам порядке
                        output.accept(ModItems.MATRIX_DOOR.get());   // Сначала Дверь
                        output.accept(ModItems.BLACK_BLOCK_ITEM.get());  // Затем супер-черный блок
                        output.accept(ModItems.GLITCH_BLOCK_ITEM.get()); // Затем блок-глитч
                        output.accept(ModItems.BLU_BLOCK_ITEM.get());    // Синий блок
                        output.accept(ModItems.RED_BLOCK_ITEM.get());    // Красный блок
                        output.accept(ModItems.WHITE_BLOCK_ITEM.get());  // Белый блок
                        output.accept(ModItems.YELLOW_BLOCK_ITEM.get()); // Желтый блок
                    })
                    .build());

    // Метод для подключения регистратора к главной шине мода
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
