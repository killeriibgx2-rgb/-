package com.eternalsnow.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class ClientScreenHooks {
    private ClientScreenHooks() {
    }

    public static void openGolemConfig(
            int entityId,
            BlockPos anchorPos,
            int radius,
            boolean hasShovel,
            int sweepW,
            int sweepL,
            int sweepH
    ) {
        Minecraft.getInstance().setScreen(new GolemConfigScreen(
                entityId, anchorPos, radius, hasShovel, sweepW, sweepL, sweepH
        ));
    }

    public static void openChimneyDamper(
            BlockPos fireplaceMasterPos,
            int damperPercent,
            int fuelMilli,
            boolean lit,
            boolean chimneySkyValid
    ) {
        Minecraft.getInstance().setScreen(new ChimneyDamperScreen(
                fireplaceMasterPos, damperPercent, fuelMilli, lit, chimneySkyValid
        ));
    }
}
