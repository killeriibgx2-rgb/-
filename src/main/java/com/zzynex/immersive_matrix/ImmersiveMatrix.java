package com.zzynex.immersive_matrix;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ImmersiveMatrix.MOD_ID)
public class ImmersiveMatrix {
    public static final String MOD_ID = "immersive_matrix";
    public static final Logger LOGGER = LogManager.getLogger();

    // Железные статические ссылки на твое новое честное измерение Матрицы
    public static final ResourceKey<Level> MATRIX_DIM_KEY =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(MOD_ID, "matrix"));
    public static final ResourceKey<DimensionType> MATRIX_DIM_TYPE_KEY =
            ResourceKey.create(Registries.DIMENSION_TYPE, new ResourceLocation(MOD_ID, "matrix_type"));

    // Безопасный отложенный регистратор Forge для типов измерений
    public static final DeferredRegister<DimensionType> DIMENSION_TYPES =
            DeferredRegister.create(Registries.DIMENSION_TYPE, MOD_ID);

    public ImmersiveMatrix() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 1. Регистрируем блоки мода
        ModBlocks.register(modEventBus);

        // 2. ЖЕЛЕЗНЫЙ ТРИГГЕР: Запускаем регистратор предметов, который свяжет
        // цветные неоновые кубы со своими блоками и включит русский язык!
        ModItems.register(modEventBus);

        // 3. Регистрируем вкладку креатива, BlockEntity и кастомные звуки сирены
        ModCreativeTabs.register(modEventBus);
        MatrixDoorBlockEntity.BLOCK_ENTITIES.register(modEventBus);
        ModSounds.register(modEventBus);

        // 4. Подключаем типы измерений датапака к шине мода
        DIMENSION_TYPES.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }
}
