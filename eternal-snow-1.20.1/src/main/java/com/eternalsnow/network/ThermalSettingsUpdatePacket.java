package com.eternalsnow.network;

import com.eternalsnow.config.ThermalConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Client proposal for thermal settings; permission and ranges are checked on the server. */
public final class ThermalSettingsUpdatePacket {
    private final boolean enabled;
    private final double ambientCelsius;
    private final double speedMultiplier;
    private final int zoneRadius;
    private final boolean bodyEnabled;
    private final boolean doorInfiltration;

    public ThermalSettingsUpdatePacket(boolean enabled, double ambientCelsius, double speedMultiplier,
                                       int zoneRadius, boolean bodyEnabled, boolean doorInfiltration) {
        this.enabled = enabled;
        this.ambientCelsius = ambientCelsius;
        this.speedMultiplier = speedMultiplier;
        this.zoneRadius = zoneRadius;
        this.bodyEnabled = bodyEnabled;
        this.doorInfiltration = doorInfiltration;
    }

    public static void encode(ThermalSettingsUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.enabled);
        buffer.writeDouble(packet.ambientCelsius);
        buffer.writeDouble(packet.speedMultiplier);
        buffer.writeInt(packet.zoneRadius);
        buffer.writeBoolean(packet.bodyEnabled);
        buffer.writeBoolean(packet.doorInfiltration);
    }

    public static ThermalSettingsUpdatePacket decode(FriendlyByteBuf buffer) {
        return new ThermalSettingsUpdatePacket(buffer.readBoolean(), buffer.readDouble(),
                buffer.readDouble(), buffer.readInt(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(ThermalSettingsUpdatePacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            boolean allowed = ThermalSettingsAccess.canEdit(player);
            boolean applied = allowed && ThermalConfig.isValid(packet.ambientCelsius,
                    packet.speedMultiplier, packet.zoneRadius);
            if (applied) {
                ThermalConfig.applyFromPanel(packet.enabled, packet.ambientCelsius,
                        packet.speedMultiplier, packet.zoneRadius, packet.bodyEnabled,
                        packet.doorInfiltration);
            }
            ThermalNetwork.sendToPlayer(player, ThermalSettingsSyncPacket.current(allowed, applied));
        });
        context.setPacketHandled(true);
    }
}
