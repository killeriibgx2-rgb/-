package com.zzynes.fly_drone;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class DroneControlPacket {
    private final float thrust;
    private final float side;
    private final float pitchChange;
    private final float yawChange;

    public DroneControlPacket(float thrust, float side, float pitchChange, float yawChange) {
        this.thrust = thrust;
        this.side = side;
        this.pitchChange = pitchChange;
        this.yawChange = yawChange;
    }

    public DroneControlPacket(FriendlyByteBuf buf) {
        this.thrust = buf.readFloat();
        this.side = buf.readFloat();
        this.pitchChange = buf.readFloat();
        this.yawChange = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeFloat(this.thrust);
        buf.writeFloat(this.side);
        buf.writeFloat(this.pitchChange);
        buf.writeFloat(this.yawChange);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.getCamera() instanceof FPVDroneEntity drone) {
                // Сервер принимает команды управления клавиатуры и применяет к физике дрона
                drone.thrustPower = this.thrust;
                drone.sidePower = this.side;

                // Плавно изменяем углы тангажа и рыскания
                drone.customPitch += this.pitchChange;
                drone.customYaw += this.yawChange;

                // Рассчитываем автоматический наклон крена (Roll) в сторону поворота для реалистичности
                drone.customRoll = this.side * -25.0F;
            }
        });
        return true;
    }
}
