package com.eternalsnow;

import com.eternalsnow.config.ThermalConfig;
import com.eternalsnow.registry.ModItems;
import com.eternalsnow.network.ThermalNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Entry point.
 *
 * <p>The thermal engine itself has no Minecraft dependencies and is covered by
 * plain JUnit tests. This class only wires it up: config, the item registry and
 * the server tick hook.
 */
@Mod(EternalSnowMod.MOD_ID)
public final class EternalSnowMod {
    public static final String MOD_ID = "eternalsnow";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EternalSnowMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ThermalConfig.SPEC,
                "eternalsnow-thermal.toml");

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modBus);
        modBus.addListener(this::onCommonSetup);
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Eternal Snow 3.0 — thermal engine starting");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ThermalNetwork::register);
    }
}
