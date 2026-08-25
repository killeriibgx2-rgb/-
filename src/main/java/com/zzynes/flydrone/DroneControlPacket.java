package com.zzynes.flydrone;

import com.zzynes.flydrone.entity.FpvDroneEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DroneControlPacket {
    private final float throttle;
    private final float pitchRate;
    private final float rollRate;
    private final float yawRate;

    public DroneControlPacket(float throttle, float pitchRate, float rollRate, float yawRate) {
        this.throttle = throttle;
        this.pitchRate = pitchRate;
        this.rollRate = rollRate;
        this.yawRate = yawRate;
    }

    public static void encode(DroneControlPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.throttle);
        buf.writeFloat(msg.pitchRate);
        buf.writeFloat(msg.rollRate);
        buf.writeFloat(msg.yawRate);
    }

    public static DroneControlPacket decode(FriendlyByteBuf buf) {
        return new DroneControlPacket(
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    public static void handle(DroneControlPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Entity riding = player.getVehicle();
            if (riding instanceof FpvDroneEntity drone) {
                drone.setThrottle(msg.throttle);
                drone.addPitchRate(msg.pitchRate);
                drone.addRollRate(msg.rollRate);
                drone.addYawRate(msg.yawRate);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}