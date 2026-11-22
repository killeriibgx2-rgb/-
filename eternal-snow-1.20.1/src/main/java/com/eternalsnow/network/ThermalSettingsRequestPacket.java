package com.eternalsnow.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Client request for the current server's editable settings. */
public final class ThermalSettingsRequestPacket {
    public static void encode(ThermalSettingsRequestPacket packet, FriendlyByteBuf buffer) {
        // No fields.
    }

    public static ThermalSettingsRequestPacket decode(FriendlyByteBuf buffer) {
        return new ThermalSettingsRequestPacket();
    }

    public static void handle(ThermalSettingsRequestPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            boolean allowed = ThermalSettingsAccess.canEdit(player);
            ThermalNetwork.sendToPlayer(player, ThermalSettingsSyncPacket.current(allowed, false));
        });
        context.setPacketHandled(true);
    }
}
