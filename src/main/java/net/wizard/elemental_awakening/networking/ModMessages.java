package net.wizard.elemental_awakening.networking;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.wizard.elemental_awakening.ElementalAwakening;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    // Уникальный ID для нашего сетевого пакета
    private static int id() {
        return packetId++;
    }

    // Регистрация сетевого канала связи мода
    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(ElementalAwakening.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // Регистрируем наш пакет синхронизации маны в Forge
        net.messageBuilder(ManaSyncDataPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ManaSyncDataPacket::new)
                .encoder(ManaSyncDataPacket::toBytes)
                .consumerMainThread(ManaSyncDataPacket::handle)
                .add();
    }

    // Метод, который мы будем вызывать на сервере, чтобы мгновенно отправить ману на экран игрока
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.sendTo(message, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}
