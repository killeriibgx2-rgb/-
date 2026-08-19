package com.zzynes.fly_drone;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FlyDroneMod.MOD_ID);

    // ИСПРАВЛЕНО: Передали точные параметры (type, level) в фабрику дрона для совместимости с реестрами мира 1.20.1!
    public static final RegistryObject<EntityType<FPVDroneEntity>> FPV_DRONE = ENTITY_TYPES.register("fpv_drone",
            () -> EntityType.Builder.<FPVDroneEntity>of((type, level) -> new FPVDroneEntity(type, level), MobCategory.MISC)
                    .sized(0.5F, 0.3F)
                    .build("fpv_drone"));

    // ИСПРАВЛЕНО: Передали параметры (type, level) в фабрику летящей ракеты РПГ
    public static final RegistryObject<EntityType<RPGRocketEntity>> RPG_ROCKET = ENTITY_TYPES.register("rpg_rocket",
            () -> EntityType.Builder.<RPGRocketEntity>of((type, level) -> new RPGRocketEntity(type, level), MobCategory.MISC)
                    .sized(0.2F, 0.2F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("rpg_rocket"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
