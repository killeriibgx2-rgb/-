package com.zzynes.fly_drone;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(FlyDroneMod.MOD_ID)
public class FlyDroneMod {
    public static final String MOD_ID = "fly_drone";
    public static final Logger LOGGER = LogManager.getLogger();

    public FlyDroneMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        modEventBus.addListener(this::entityAttributesEvent);

        MinecraftForge.EVENT_BUS.register(this);

        // ВКЛЮЧАЕМ НАШ СЕТЕВОЙ КАНАЛ С ПАКЕТАМИ
        ModMessages.register();

        LOGGER.info("Fly Drone Mod от Zzynes: Предметы, Сущности и Сетевой код готовы!");
    }

    private void entityAttributesEvent(EntityAttributeCreationEvent event) {
        event.put(ModEntities.FPV_DRONE.get(), FPVDroneEntity.createAttributes().build());
    }
}
