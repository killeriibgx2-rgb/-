package com.zzynes.flydrone.DRONE;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FlyDroneMod.MOD_ID);

    public static final RegistryObject<SoundEvent> AIR_SIREN =
            SOUND_EVENTS.register("item.air_siren",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(FlyDroneMod.MOD_ID, "item.air_siren")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}