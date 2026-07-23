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

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(ElementalAwakening.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // 1. Твой рабочий пакет синхронизации маны (от Сервера к Клиенту)
        net.messageBuilder(ManaSyncDataPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ManaSyncDataPacket::new)
                .encoder(ManaSyncDataPacket::toBytes)
                .consumerMainThread(ManaSyncDataPacket::handle)
                .add();

        // 2. НОВЫЙ ПАКЕТ: Открытие главного меню выбора стихий (от Сервера к Клиенту)
        net.messageBuilder(OpenElementMenuPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenElementMenuPacket::decode)
                .encoder(OpenElementMenuPacket::encode)
                .consumerMainThread(OpenElementMenuPacket::handle)
                .add();

        // 3. НОВЫЙ ПАКЕТ: Передача выбранной стихии на сервер (от Клиента к Серверу)
        net.messageBuilder(SelectElementServerPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SelectElementServerPacket::decode)
                .encoder(SelectElementServerPacket::encode)
                .consumerMainThread(SelectElementServerPacket::handle)
                .add();
    }

    // Метод отправки пакета от Сервера к Клиенту (для синхронизации маны и открытия меню)
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.sendTo(message, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    // НОВЫЙ МЕТОД: Метод отправки пакета с Клиента на Сервер (для отправки выбора стихии)
    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}
