package net.wizard.elemental_awakening.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.wizard.elemental_awakening.mana.PlayerManaProvider;
import java.util.function.Supplier;

public class SelectElementServerPacket {
    private final int elementId;

    // Конструктор для создания пакета на клиенте
    public SelectElementServerPacket(int elementId) {
        this.elementId = elementId;
    }

    // Конструктор для чтения пакета из буфера Forge (тот самый ::new в ModMessages)
    public SelectElementServerPacket(FriendlyByteBuf buf) {
        this.elementId = buf.readInt();
    }

    // Метод для записи пакета в буфер Forge (тот самый ::toBytes в ModMessages)
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.elementId);
    }

    public static void handle(SelectElementServerPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(PlayerManaProvider.PLAYER_MANA).ifPresent(mana -> {
                    // ЖЕСТКАЯ RPG-БЛОКИРОВКА: Меняем стихию только если она равна 0 (еще не выбрана)
                    if (mana.getElement() == 0) {
                        mana.setElement(pkt.elementId);

                        // АВТОМАТИЧЕСКАЯ ВЫДАЧА СТАРТОВОГО ЗАКЛИНАНИЯ В ПАЛОЧКУ ИГРОКА
                        ItemStack heldItem = player.getMainHandItem();
                        if (!heldItem.isEmpty()) {
                            if (pkt.elementId == 1) {
                                heldItem.getOrCreateTag().putString("SelectedSpell", "earth_stena"); // Земля
                            } else if (pkt.elementId == 2) {
                                heldItem.getOrCreateTag().putString("SelectedSpell", "fire_ball");   // Огонь
                            } else if (pkt.elementId == 3) {
                                heldItem.getOrCreateTag().putString("SelectedSpell", "water_heal");  // Вода
                            } else if (pkt.elementId == 4) {
                                heldItem.getOrCreateTag().putString("SelectedSpell", "air_dash");    // Воздух
                            }
                        }

                        // Синхронизируем ману и обновляем HUD клиента
                        ModMessages.sendToPlayer(new ManaSyncDataPacket(mana.getMana()), player);
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
