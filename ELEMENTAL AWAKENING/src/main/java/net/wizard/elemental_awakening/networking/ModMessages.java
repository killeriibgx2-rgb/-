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

        // 2. Пакет открытия главного меню выбора стихий (от Сервера к Клиенту)
        net.messageBuilder(OpenElementMenuPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(buf -> OpenElementMenuPacket.decode(buf))
                .encoder((pkt, buf) -> OpenElementMenuPacket.encode(pkt, buf))
                .consumerMainThread(OpenElementMenuPacket::handle)
                .add();

        // 3. Пакет передачи выбранной стихии на сервер (от Клиента к Серверу)
        net.messageBuilder(SelectElementServerPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SelectElementServerPacket::new)
                .encoder(SelectElementServerPacket::toBytes)
                .consumerMainThread(SelectElementServerPacket::handle)
                .add();

        // 4. Пакет передачи выбранного заклинания в палочку (от Клиента к Серверу)
        net.messageBuilder(SelectSpellServerPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SelectSpellServerPacket::new)
                .encoder(SelectSpellServerPacket::toBytes)
                .consumerMainThread(SelectSpellServerPacket::handle)
                .add();
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.sendTo(message, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}
