package com.eternalsnow.network;

import com.eternalsnow.command.EternalSnowCommands;
import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.world.EternalSnowSavedData;
import com.eternalsnow.world.SnowAccumulationManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ServerboundUpdateSettingsPacket {
    private final ServerSnowSettings settings;

    public ServerboundUpdateSettingsPacket(ServerSnowSettings settings) {
        this.settings = settings.copy();
    }

    public static void encode(ServerboundUpdateSettingsPacket msg, net.minecraft.network.FriendlyByteBuf buf) {
        msg.settings.writeToBuf(buf);
    }

    public static ServerboundUpdateSettingsPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        ServerSnowSettings s = new ServerSnowSettings();
        s.readFromBuf(buf);
        return new ServerboundUpdateSettingsPacket(s);
    }

    public static void handle(ServerboundUpdateSettingsPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                return;
            }
            MinecraftServer server = sender.getServer();
            if (server == null) {
                return;
            }
            if (!EternalSnowCommands.canPlayerEditServerSettings(sender)) {
                PacketHandler.syncSettingsToPlayer(sender, EternalSnowState.getServerSettings());
                return;
            }

            applySettingsOnServer(server, msg.settings);
        });
        ctx.setPacketHandled(true);
    }

    public static void applySettingsOnServer(MinecraftServer server, ServerSnowSettings updated) {
        updated.clamp();
        EternalSnowState.getServerSettings().copyFrom(updated);
        EternalSnowSavedData.get(server).updateSettings(updated);

        for (ServerLevel level : server.getAllLevels()) {
            SnowAccumulationManager.enforceWeatherOnLevel(level);
            level.updateSkyBrightness();
        }

        PacketHandler.broadcastSettings(server, EternalSnowState.getServerSettings());
    }
}
