package com.zzynes.fly_drone;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FlyDroneMod.MOD_ID)
public class FlyDroneMod {
    public static final String MOD_ID = "fly_drone";

    public FlyDroneMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Регистрируем все компоненты мода в шине
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        // ЖЕСТКИЙ ФИКС: Подключаем создание атрибутов здоровья строго на шину MOD!
        modEventBus.addListener(this::onAttributeCreation);

        MinecraftForge.EVENT_BUS.register(this);
    }

    // Этот метод передает игре параметры здоровья дрона ДО загрузки мира, убирая краш реестров!
    private void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.FPV_DRONE.get(), FPVDroneEntity.createAttributes().build());
    }
}
