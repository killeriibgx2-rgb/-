package com.eternalsnow.network;

import com.eternalsnow.entity.FestiveCopperGolemEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ServerboundConfigureGolemPacket {
    private final int entityId;
    private final int workRadius;
    private final boolean triggerPreview;

    public ServerboundConfigureGolemPacket(int entityId, int workRadius, boolean triggerPreview) {
        this.entityId = entityId;
        this.workRadius = workRadius;
        this.triggerPreview = triggerPreview;
    }

    public static void encode(ServerboundConfigureGolemPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.workRadius);
        buf.writeBoolean(msg.triggerPreview);
    }

    public static ServerboundConfigureGolemPacket decode(FriendlyByteBuf buf) {
        return new ServerboundConfigureGolemPacket(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean()
        );
    }

    public static void handle(ServerboundConfigureGolemPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                return;
            }
            Entity ent = sender.level().getEntity(msg.entityId);
            if (!(ent instanceof FestiveCopperGolemEntity golem)) {
                return;
            }
            if (sender.distanceToSqr(golem) > 48.0D * 48.0D) {
                return;
            }
            golem.setWorkRadius(msg.workRadius);
            if (msg.triggerPreview) {
                golem.triggerZonePreview();
            }
            sender.displayClientMessage(
                    Component.translatable("eternalsnow.msg.golem_configured", golem.getWorkRadius()),
                    true
            );
        });
        ctx.setPacketHandled(true);
    }
}
