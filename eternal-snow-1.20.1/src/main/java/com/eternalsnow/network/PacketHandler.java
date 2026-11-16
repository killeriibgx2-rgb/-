package com.eternalsnow.network;

import com.eternalsnow.EternalSnowMod;
import com.eternalsnow.command.EternalSnowCommands;
import com.eternalsnow.config.ServerSnowSettings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class PacketHandler {
    private static final String PROTOCOL_VERSION = "8"; // v2.5.2: +2 room-heating settings fields
    private static boolean registered = false;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EternalSnowMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private PacketHandler() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        int id = 0;

        CHANNEL.registerMessage(
                id++,
                ClientboundSyncSettingsPacket.class,
                ClientboundSyncSettingsPacket::encode,
                ClientboundSyncSettingsPacket::decode,
                ClientboundSyncSettingsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                ServerboundUpdateSettingsPacket.class,
                ServerboundUpdateSettingsPacket::encode,
                ServerboundUpdateSettingsPacket::decode,
                ServerboundUpdateSettingsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                id++,
                ClientboundWeatherSyncPacket.class,
                ClientboundWeatherSyncPacket::encode,
                ClientboundWeatherSyncPacket::decode,
                ClientboundWeatherSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                ClientboundPlayerSurvivalPacket.class,
                ClientboundPlayerSurvivalPacket::encode,
                ClientboundPlayerSurvivalPacket::decode,
                ClientboundPlayerSurvivalPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                ServerboundForceWeatherPacket.class,
                ServerboundForceWeatherPacket::encode,
                ServerboundForceWeatherPacket::decode,
                ServerboundForceWeatherPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                id++,
                ClientboundOpenGolemScreenPacket.class,
                ClientboundOpenGolemScreenPacket::encode,
                ClientboundOpenGolemScreenPacket::decode,
                ClientboundOpenGolemScreenPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                ServerboundConfigureGolemPacket.class,
                ServerboundConfigureGolemPacket::encode,
                ServerboundConfigureGolemPacket::decode,
                ServerboundConfigureGolemPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                id++,
                ClientboundHeatGridPacket.class,
                ClientboundHeatGridPacket::encode,
                ClientboundHeatGridPacket::decode,
                ClientboundHeatGridPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                ClientboundOpenChimneyScreenPacket.class,
                ClientboundOpenChimneyScreenPacket::encode,
                ClientboundOpenChimneyScreenPacket::decode,
                ClientboundOpenChimneyScreenPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                ServerboundSetChimneyDamperPacket.class,
                ServerboundSetChimneyDamperPacket::encode,
                ServerboundSetChimneyDamperPacket::decode,
                ServerboundSetChimneyDamperPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }

    public static void sendToPlayer(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void broadcastToAll(Object msg) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }

    public static void syncSettingsToPlayer(ServerPlayer player, ServerSnowSettings settings) {
        boolean opAccess = EternalSnowCommands.canPlayerEditServerSettings(player);
        sendToPlayer(player, new ClientboundSyncSettingsPacket(settings, opAccess));
    }

    public static void broadcastSettings(MinecraftServer server, ServerSnowSettings settings) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncSettingsToPlayer(player, settings);
        }
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }
}
