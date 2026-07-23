package net.wizard.elemental_awakening.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.wizard.elemental_awakening.client.ManaHudOverlay;
import net.wizard.elemental_awakening.mana.PlayerManaProvider;

import java.util.function.Supplier;

public class ManaSyncDataPacket {
    private final float mana;

    // Конструктор для создания пакета с числом маны
    public ManaSyncDataPacket(float mana) {
        this.mana = mana;
    }

    // Читаем ману из сетевого буфера (когда пакет прилетел)
    public ManaSyncDataPacket(FriendlyByteBuf buf) {
        this.mana = buf.readFloat();
    }

    // Записываем ману в сетевой буфер (когда отправляем пакет)
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeFloat(this.mana);
    }

    // Этот метод выполняется строго на КЛИЕНТЕ, когда письмо получено!
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Берем нашего игрока на клиенте и насильно обновляем ему ману, чтобы полоска сдвинулась!
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.getCapability(PlayerManaProvider.PLAYER_MANA).ifPresent(manaCap -> {
                    manaCap.setMana(this.mana);
                });
            }
        });
        return true;
    }
}
