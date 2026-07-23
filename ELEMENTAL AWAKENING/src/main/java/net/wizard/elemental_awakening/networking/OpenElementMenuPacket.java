package net.wizard.elemental_awakening.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.wizard.elemental_awakening.client.MainMenuScreen;
import net.minecraft.client.Minecraft;
import java.util.function.Supplier;

// Этот пакет сервер отправляет игроку, чтобы принудительно открыть экран выбора стихии
public class OpenElementMenuPacket {

    // Конструктор пакета (пустой, так как внутри пакета нет данных вроде чисел или текста)
    public OpenElementMenuPacket() {}

    // Этот метод упаковывает пакет для отправки по сети через интернет (оставляем пустым)
    public static void encode(OpenElementMenuPacket pkt, FriendlyByteBuf buf) {}

    // Этот метод распаковывает пакет, когда он прилетел на компьютер игрока
    public static OpenElementMenuPacket decode(FriendlyByteBuf buf) {
        return new OpenElementMenuPacket();
    }

    // Тут находится главная логика, которая сработает на стороне клиента (у игрока)
    public static void handle(OpenElementMenuPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Команда Minecraft.getInstance().setScreen открывает интерфейс на весь экран.
            // MainMenuScreen — это класс нашего нового меню, который мы напишем чуть позже.
            // Сейчас слово MainMenuScreen будет гореть КРАСНЫМ цветом — это нормально, не пугайся!
            Minecraft.getInstance().setScreen(new MainMenuScreen());
        });
        ctx.get().setPacketHandled(true);
    }
}
