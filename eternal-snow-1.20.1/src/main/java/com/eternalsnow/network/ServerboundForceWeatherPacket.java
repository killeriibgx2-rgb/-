package com.eternalsnow.network;

import com.eternalsnow.command.EternalSnowCommands;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.weather.WeatherEventManager;
import com.eternalsnow.weather.WeatherEventType;
import com.eternalsnow.world.SnowAccumulationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ServerboundForceWeatherPacket {
    // actionId: 0..4 = WeatherEventType, 100 = clear snow radius 24
    private final int actionId;

    public ServerboundForceWeatherPacket(int actionId) {
        this.actionId = actionId;
    }

    public static void encode(ServerboundForceWeatherPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.actionId);
    }

    public static ServerboundForceWeatherPacket decode(FriendlyByteBuf buf) {
        return new ServerboundForceWeatherPacket(buf.readVarInt());
    }

    public static void handle(ServerboundForceWeatherPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                return;
            }
            MinecraftServer server = sender.getServer();
            if (server == null || !EternalSnowCommands.canPlayerEditServerSettings(sender)) {
                return;
            }

            if (msg.actionId == 100) {
                int cleared = SnowAccumulationManager.clearSnowAroundPlayer(sender, 24);
                sender.displayClientMessage(Component.translatable("eternalsnow.msg.cleared_snow", cleared), true);
                return;
            }

            WeatherEventType ev = WeatherEventType.byId(msg.actionId);
            WeatherEventManager.forceEvent(server, ev, 9600);
            PacketHandler.broadcastToAll(new ClientboundWeatherSyncPacket(
                    EternalSnowState.weatherEventId,
                    EternalSnowState.windYawDeg,
                    EternalSnowState.windSpeed,
                    EternalSnowState.snowIntensity,
                    EternalSnowState.worldAmbientTempC,
                    EternalSnowState.eventTicksRemaining
            ));
            sender.displayClientMessage(
                    Component.translatable("eternalsnow.msg.weather_forced", Component.translatable(ev.getTranslationKey())),
                    true
            );
        });
        ctx.setPacketHandled(true);
    }
}
