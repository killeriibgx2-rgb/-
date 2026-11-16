package com.eternalsnow.network;

import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.state.EternalSnowState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientboundSyncSettingsPacket {
    private final ServerSnowSettings settings;
    private final boolean opAccess;

    public ClientboundSyncSettingsPacket(ServerSnowSettings settings, boolean opAccess) {
        this.settings = settings.copy();
        this.opAccess = opAccess;
    }

    public static void encode(ClientboundSyncSettingsPacket msg, FriendlyByteBuf buf) {
        msg.settings.writeToBuf(buf);
        buf.writeBoolean(msg.opAccess);
    }

    public static ClientboundSyncSettingsPacket decode(FriendlyByteBuf buf) {
        ServerSnowSettings s = new ServerSnowSettings();
        s.readFromBuf(buf);
        boolean op = buf.readBoolean();
        return new ClientboundSyncSettingsPacket(s, op);
    }

    public static void handle(ClientboundSyncSettingsPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> EternalSnowState.updateClientSyncedSettings(msg.settings, msg.opAccess));
        ctx.setPacketHandled(true);
    }
}
