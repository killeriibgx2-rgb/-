package com.zzynes.fly_drone;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(FlyDroneMod.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(ToggleFlightPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ToggleFlightPacket::new)
                .encoder(ToggleFlightPacket::toBytes)
                .consumerMainThread(ToggleFlightPacket::handle)
                .add();

        net.messageBuilder(DroneControlPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(DroneControlPacket::new)
                .encoder(DroneControlPacket::toBytes)
                .consumerMainThread(DroneControlPacket::handle)
                .add();

        // РЕГИСТРИРУЕМ НАШ НОВЫЙ БОЕВОЙ ПАКЕТ ДЛЯ ЛКМ/ПКМ
        net.messageBuilder(DroneAttackPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(DroneAttackPacket::new)
                .encoder(DroneAttackPacket::toBytes)
                .consumerMainThread(DroneAttackPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}
