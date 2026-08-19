package com.zzynes.fly_drone;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class DroneAttackPacket {
    private final int buttonType; // 1 - ЛКМ (РПГ), 2 - ПКМ (C4)

    public DroneAttackPacket(int buttonType) {
        this.buttonType = buttonType;
    }

    public DroneAttackPacket(FriendlyByteBuf buf) {
        this.buttonType = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.buttonType);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.getCamera() instanceof FPVDroneEntity drone) {
                // Если пилот нажал ЛКМ — стреляем из РПГ
                if (this.buttonType == 1) {
                    drone.fireAttachedWeapon();
                }
                // Если пилот нажал ПКМ — взрываем C4
                if (this.buttonType == 2) {
                    drone.detonateC4();
                }
            }
        });
        return true;
    }
}
