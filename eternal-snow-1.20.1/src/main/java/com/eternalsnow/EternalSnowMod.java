package com.eternalsnow;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Entry point. Intentionally almost empty: the thermal engine (package
 * {@code com.eternalsnow.thermal.engine}) has no Minecraft dependencies and is
 * covered by plain JUnit tests, so the mod class only has to wire it up.
 */
@Mod(EternalSnowMod.MOD_ID)
public final class EternalSnowMod {
    public static final String MOD_ID = "eternalsnow";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EternalSnowMod() {
        LOGGER.info("Eternal Snow 3.0 — thermal engine loading");
    }
}
