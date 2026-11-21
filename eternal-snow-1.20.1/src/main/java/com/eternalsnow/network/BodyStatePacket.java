package com.eternalsnow.network;

import com.eternalsnow.thermal.world.ClientBodyState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Server → client body and local-air temperatures for the HUD. */
public final class BodyStatePacket {
    private final boolean enabled;
    private final double coreCelsius;
    private final double airCelsius;

    public BodyStatePacket(boolean enabled, double coreCelsius, double airCelsius) {
        this.enabled = enabled;
        this.coreCelsius = coreCelsius;
        this.airCelsius = airCelsius;
    }

    public static BodyStatePacket disabled() {
        return new BodyStatePacket(false, 0.0, 0.0);
    }

    public static void encode(BodyStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.enabled);
        buffer.writeDouble(packet.coreCelsius);
        buffer.writeDouble(packet.airCelsius);
    }

    public static BodyStatePacket decode(FriendlyByteBuf buffer) {
        return new BodyStatePacket(buffer.readBoolean(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(BodyStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientBodyState.update(
                packet.enabled, packet.coreCelsius, packet.airCelsius));
        context.setPacketHandled(true);
    }
}
