package com.eternalsnow.network;

import com.eternalsnow.EternalSnowMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Optional;

/** Minimal protocol for server-authoritative HUD readings. */
public final class ThermalNetwork {
    private static final String PROTOCOL = "1";
    private static boolean registered;

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EternalSnowMod.MOD_ID, "thermal"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private ThermalNetwork() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, BodyStatePacket.class,
                BodyStatePacket::encode, BodyStatePacket::decode, BodyStatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendToPlayer(ServerPlayer player, BodyStatePacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
