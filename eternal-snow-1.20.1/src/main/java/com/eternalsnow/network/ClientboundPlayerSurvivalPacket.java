package com.eternalsnow.network;

import com.eternalsnow.state.EternalSnowState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientboundPlayerSurvivalPacket {
    private final float localTempC;
    private final float bodyTempC;
    private final float bodyTempState;
    private final int thirstLevel;
    private final float thirstSaturation;
    private final long lookedCampfirePos;
    private final int lookedCampfireTicks;
    private final boolean lookedCampfireWind;

    public ClientboundPlayerSurvivalPacket(
            float localTempC,
            float bodyTempC,
            float bodyTempState,
            int thirstLevel,
            float thirstSaturation,
            long lookedCampfirePos,
            int lookedCampfireTicks,
            boolean lookedCampfireWind
    ) {
        this.localTempC = localTempC;
        this.bodyTempC = bodyTempC;
        this.bodyTempState = bodyTempState;
        this.thirstLevel = thirstLevel;
        this.thirstSaturation = thirstSaturation;
        this.lookedCampfirePos = lookedCampfirePos;
        this.lookedCampfireTicks = lookedCampfireTicks;
        this.lookedCampfireWind = lookedCampfireWind;
    }

    public static void encode(ClientboundPlayerSurvivalPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.localTempC);
        buf.writeFloat(msg.bodyTempC);
        buf.writeFloat(msg.bodyTempState);
        buf.writeVarInt(msg.thirstLevel);
        buf.writeFloat(msg.thirstSaturation);
        buf.writeLong(msg.lookedCampfirePos);
        buf.writeVarInt(msg.lookedCampfireTicks);
        buf.writeBoolean(msg.lookedCampfireWind);
    }

    public static ClientboundPlayerSurvivalPacket decode(FriendlyByteBuf buf) {
        return new ClientboundPlayerSurvivalPacket(
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readVarInt(),
                buf.readFloat(),
                buf.readLong(),
                buf.readVarInt(),
                buf.readBoolean()
        );
    }

    public static void handle(ClientboundPlayerSurvivalPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            EternalSnowState.clientLocalTempC = msg.localTempC;
            EternalSnowState.clientBodyTempC = msg.bodyTempC;
            EternalSnowState.clientBodyTempState = msg.bodyTempState;
            EternalSnowState.clientThirstLevel = msg.thirstLevel;
            EternalSnowState.clientThirstSaturation = msg.thirstSaturation;
            EternalSnowState.clientLookedCampfirePos = msg.lookedCampfirePos;
            EternalSnowState.clientLookedCampfireTicks = msg.lookedCampfireTicks;
            EternalSnowState.clientLookedCampfireWind = msg.lookedCampfireWind;
        });
        ctx.setPacketHandled(true);
    }
}
