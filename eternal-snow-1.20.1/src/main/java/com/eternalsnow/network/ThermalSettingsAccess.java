package com.eternalsnow.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Settings edits are currently restricted to the integrated single-player admin. */
final class ThermalSettingsAccess {
    private ThermalSettingsAccess() {
    }

    static boolean canEdit(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || !server.isSingleplayer()) return false;

        // In a normal solo world there is only the owner. If opened to LAN,
        // only players granted operator status may continue editing settings.
        return server.getPlayerList().getPlayerCount() == 1
                || server.getPlayerList().isOp(player.getGameProfile());
    }
}
