package com.zzynes.fly_drone;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public class ToggleFlightPacket {
    private final UUID droneUuid;

    public ToggleFlightPacket(UUID droneUuid) {
        this.droneUuid = droneUuid;
    }

    public ToggleFlightPacket(FriendlyByteBuf buf) {
        this.droneUuid = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.droneUuid);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                // Ищем наш дрон в мире по его UUID
                Entity entity = level.getEntity(droneUuid);

                if (entity instanceof FPVDroneEntity drone) {
                    // Запоминаем в пульт/игрока, что полет начался
                    // Переносим камеру игрока на сущность дрона на стороне сервера
                    player.setCamera(drone);

                    // Включаем принудительную прогрузку чанка, в котором стоит дрон
                    ForgeChunkManager.forceChunk(level, FlyDroneMod.MOD_ID, player,
                            drone.chunkPosition().x, drone.chunkPosition().z, true, true);

                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§aСвязь установлена. Камера FPV активна!"), true);
                } else {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cДрон слишком далеко или уничтожен!"), true);
                }
            }
        });
        return true;
    }
}
