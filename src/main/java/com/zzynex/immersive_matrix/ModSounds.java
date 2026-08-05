package com.zzynex.immersive_matrix;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    // Создаем регистратор звуков под наш Mod ID
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ImmersiveMatrix.MOD_ID);

    // Регистрируем наш трек разрушения
    public static final RegistryObject<SoundEvent> DIMENSION_COLLAPSE = SOUND_EVENTS.register("dimension_collapse",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ImmersiveMatrix.MOD_ID, "dimension_collapse")));

    // Метод для подключения к главной шине мода
    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
