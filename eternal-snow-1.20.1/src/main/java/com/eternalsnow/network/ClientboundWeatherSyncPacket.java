package com.eternalsnow.network;

import com.eternalsnow.state.EternalSnowState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientboundWeatherSyncPacket {
    private final int weatherEventId;
    private final float windYawDeg;
    private final float windSpeed;
    private final float snowIntensity;
    private final float worldAmbientTempC;
    private final int eventTicksRemaining;

    public ClientboundWeatherSyncPacket(
            int weatherEventId,
            float windYawDeg,
            float windSpeed,
            float snowIntensity,
            float worldAmbientTempC,
            int eventTicksRemaining
    ) {
        this.weatherEventId = weatherEventId;
        this.windYawDeg = windYawDeg;
        this.windSpeed = windSpeed;
        this.snowIntensity = snowIntensity;
        this.worldAmbientTempC = worldAmbientTempC;
        this.eventTicksRemaining = eventTicksRemaining;
    }

    public static void encode(ClientboundWeatherSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.weatherEventId);
        buf.writeFloat(msg.windYawDeg);
        buf.writeFloat(msg.windSpeed);
        buf.writeFloat(msg.snowIntensity);
        buf.writeFloat(msg.worldAmbientTempC);
        buf.writeVarInt(msg.eventTicksRemaining);
    }

    public static ClientboundWeatherSyncPacket decode(FriendlyByteBuf buf) {
        return new ClientboundWeatherSyncPacket(
                buf.readVarInt(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readVarInt()
        );
    }

    public static void handle(ClientboundWeatherSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            EternalSnowState.weatherEventId = msg.weatherEventId;
            EternalSnowState.windYawDeg = msg.windYawDeg;
            EternalSnowState.windSpeed = msg.windSpeed;
            EternalSnowState.snowIntensity = msg.snowIntensity;
            // v2.5.3: write a CLIENT-ONLY field. In singleplayer the client and the logical
            // server share one JVM, so overwriting worldAmbientTempC here would feed a stale
            // value back into the server-side thermal solver.
            EternalSnowState.clientStreetTempC = msg.worldAmbientTempC;
            EternalSnowState.eventTicksRemaining = msg.eventTicksRemaining;
        });
        ctx.setPacketHandled(true);
    }
}
