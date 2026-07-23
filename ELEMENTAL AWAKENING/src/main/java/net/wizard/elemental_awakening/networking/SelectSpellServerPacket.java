package net.wizard.elemental_awakening.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

// Этот пакет шлется на сервер, когда игрок кликает по иконке заклинания в меню способностей
public class SelectSpellServerPacket {
    private final String spellId;

    public SelectSpellServerPacket(String spellId) {
        this.spellId = spellId;
    }

    // Чтение из буфера сети Forge
    public SelectSpellServerPacket(FriendlyByteBuf buf) {
        this.spellId = buf.readUtf(Short.MAX_VALUE);
    }

    // Запись в буфер сети Forge
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.spellId);
    }

    public static void handle(SelectSpellServerPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // Достаем предмет, который сейчас находится в главной руке игрока (наша палочка)
                ItemStack heldItem = player.getMainHandItem();
                if (!heldItem.isEmpty()) {
                    // Записываем в NBT-тег предмета ID выбранного заклинания навсегда!
                    heldItem.getOrCreateTag().putString("SelectedSpell", pkt.spellId);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
