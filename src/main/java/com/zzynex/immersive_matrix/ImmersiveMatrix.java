package com.zzynex.immersive_matrix;

import net.minecraft.core.Registry;
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
import net.minecraftforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ImmersiveMatrix.MOD_ID)
public class ImmersiveMatrix {
    public static final String MOD_ID = "immersive_matrix";
    public static final Logger LOGGER = LogManager.getLogger();

    // Железные ссылки на твое новое, честное измерение
    public static final ResourceKey<Level> MATRIX_DIM_KEY =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(MOD_ID, "matrix"));
    public static final ResourceKey<DimensionType> MATRIX_DIM_TYPE_KEY =
            ResourceKey.create(Registries.DIMENSION_TYPE, new ResourceLocation(MOD_ID, "matrix_type"));

    // Официальный отложенный регистратор для типов измерений Forge
    public static final DeferredRegister<DimensionType> DIMENSION_TYPES =
            DeferredRegister.create(Registries.DIMENSION_TYPE, MOD_ID);

    public ImmersiveMatrix() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Регистрируем все блоки, предметы, сущности и звуки нашего мода
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        MatrixDoorBlockEntity.BLOCK_ENTITIES.register(modEventBus);
        ModSounds.register(modEventBus);

        // Подключаем регистратор типов измерений к шине мода
        DIMENSION_TYPES.register(modEventBus);

        // ДЛИННЫЙ ОБХОД КРАША: Подключаем специальный низкоуровневый слушатель реестров
        modEventBus.addListener(this::onRegisterRegistries);

        MinecraftForge.EVENT_BUS.register(this);
    }

    // Этот подробный метод принудительно срабатывает в момент, когда Forge
    // инициализирует внутренние базы данных для Одиночной игры (Синглплеера).
    // Он жестко сшивает Java-код с датапаком до того, как лаунчер успеет выдать ошибку.
    private void onRegisterRegistries(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.DIMENSION_TYPE)) {
            event.register(Registries.DIMENSION_TYPE, new ResourceLocation(MOD_ID, "matrix_type"), () -> {
                // Возвращаем пустой технический контейнер, чтобы Forge легально
                // забронировал ID в реестре и подгрузил туда параметры из нашего JSON датапака
                return new DimensionType(
                        java.util.OptionalLong.empty(),
                        true, false, false, true,
                        1.0D, false, false,
                        -64, 384, 384,
                        net.minecraft.tags.BlockTags.INFINIBURN_OVERWORLD,
                        new ResourceLocation(MOD_ID, "matrix_effects"),
                        0.0F,
                        new DimensionType.MonsterSettings(false, false, net.minecraft.util.valueproviders.UniformInt.of(0, 7), 0)
                );
            });
            LOGGER.info("[Immersive Matrix] Подробная синхронизация Java реестров и JSON датапака успешно выполнена!");
        }
    }
}
