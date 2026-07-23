package net.wizard.elemental_awakening.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.wizard.elemental_awakening.mana.PlayerManaProvider;
import java.util.function.Supplier;

// Этот пакет отправляется от клиента (игрока) на сервер, когда он нажал на кнопку стихии в меню
public class SelectElementServerPacket {
    // В этой переменной мы передаем число — ID выбранной стихии (1 - Земля, 2 - Огонь и т.д.)
    private final int elementId;

    // Конструктор, чтобы упаковать ID стихии в пакет
    public SelectElementServerPacket(int elementId) {
        this.elementId = elementId;
    }

    // Записываем число elementId в буфер для отправки по интернету
    public static void encode(SelectElementServerPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.elementId);
    }

    // Читаем число elementId из буфера, когда пакет прилетел на сервер
    public static SelectElementServerPacket decode(FriendlyByteBuf buf) {
        return new SelectElementServerPacket(buf.readInt());
    }

    // Главная логика, которая выполняется строго на сервере
    public static void handle(SelectElementServerPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Получаем игрока, от которого пришел этот клик
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // Достаем Капабилити маны и стихий этого игрока
                player.getCapability(PlayerManaProvider.PLAYER_MANA).ifPresent(mana -> {

                    // ЖЕСТКАЯ ПРОВЕРКА ДЛЯ БЛОКИРОВКИ:
                    // Если у игрока стихия РАВНА 0 (то есть он еще обычный человек), то мы разрешаем выбор
                    if (mana.getElement() == 0) {
                        // Навсегда записываем выбранную стихию в файлы игрока
                        mana.setElement(pkt.elementId);

                        // ЛОГИКА NBT ДЛЯ ПАЛОЧКИ:
                        // Проверяем, что у игрока в главной руке сейчас находится наш магический посох (палочка)
                        ItemStack heldItem = player.getMainHandItem();
                        if (!heldItem.isEmpty()) {
                            // Записываем в NBT-тег предмета выбранное заклинание.
                            // Если игрок выбрал 1 (Землю), даем ему стартовую Стену Земли ("earth_stena")
                            if (pkt.elementId == 1) {
                                heldItem.getOrCreateTag().putString("SelectedSpell", "earth_stena");
                            }
                            // Сюда в будущем допишем стартовые скиллы для 2 (Огня), 3 (Воды) и 4 (Воздуха)
                        }
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
