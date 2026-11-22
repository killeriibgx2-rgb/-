package com.eternalsnow.network;

import com.eternalsnow.config.ThermalConfig;
import com.eternalsnow.thermal.world.ClientThermalSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Server-authoritative settings snapshot and apply result. */
public final class ThermalSettingsSyncPacket {
    private final boolean allowed;
    private final boolean applied;
    private final boolean enabled;
    private final double ambientCelsius;
    private final double speedMultiplier;
    private final int zoneRadius;
    private final boolean bodyEnabled;
    private final boolean doorInfiltration;

    public ThermalSettingsSyncPacket(boolean allowed, boolean applied, boolean enabled,
                                     double ambientCelsius, double speedMultiplier, int zoneRadius,
                                     boolean bodyEnabled, boolean doorInfiltration) {
        this.allowed = allowed;
        this.applied = applied;
        this.enabled = enabled;
        this.ambientCelsius = ambientCelsius;
        this.speedMultiplier = speedMultiplier;
        this.zoneRadius = zoneRadius;
        this.bodyEnabled = bodyEnabled;
        this.doorInfiltration = doorInfiltration;
    }

    public static ThermalSettingsSyncPacket current(boolean allowed, boolean applied) {
        return new ThermalSettingsSyncPacket(allowed, applied,
                ThermalConfig.ENABLED.get(), ThermalConfig.AMBIENT_CELSIUS.get(),
                ThermalConfig.SPEED_MULTIPLIER.get(), ThermalConfig.ZONE_RADIUS.get(),
                ThermalConfig.BODY_ENABLED.get(), ThermalConfig.DOOR_INFILTRATION.get());
    }

    public static void encode(ThermalSettingsSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.allowed);
        buffer.writeBoolean(packet.applied);
        buffer.writeBoolean(packet.enabled);
        buffer.writeDouble(packet.ambientCelsius);
        buffer.writeDouble(packet.speedMultiplier);
        buffer.writeInt(packet.zoneRadius);
        buffer.writeBoolean(packet.bodyEnabled);
        buffer.writeBoolean(packet.doorInfiltration);
    }

    public static ThermalSettingsSyncPacket decode(FriendlyByteBuf buffer) {
        return new ThermalSettingsSyncPacket(buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readDouble(), buffer.readDouble(), buffer.readInt(),
                buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(ThermalSettingsSyncPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientThermalSettings.update(packet.allowed, packet.applied,
                packet.enabled, packet.ambientCelsius, packet.speedMultiplier, packet.zoneRadius,
                packet.bodyEnabled, packet.doorInfiltration));
        context.setPacketHandled(true);
    }
}
