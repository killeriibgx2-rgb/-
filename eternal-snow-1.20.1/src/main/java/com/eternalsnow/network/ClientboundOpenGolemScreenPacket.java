package com.eternalsnow.network;

import com.eternalsnow.client.ClientScreenHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientboundOpenGolemScreenPacket {
    private final int entityId;
    private final BlockPos anchorPos;
    private final int workRadius;
    private final boolean hasShovel;
    private final int sweepW;
    private final int sweepL;
    private final int sweepH;

    public ClientboundOpenGolemScreenPacket(
            int entityId,
            BlockPos anchorPos,
            int workRadius,
            boolean hasShovel,
            int sweepW,
            int sweepL,
            int sweepH
    ) {
        this.entityId = entityId;
        this.anchorPos = anchorPos;
        this.workRadius = workRadius;
        this.hasShovel = hasShovel;
        this.sweepW = sweepW;
        this.sweepL = sweepL;
        this.sweepH = sweepH;
    }

    public static void encode(ClientboundOpenGolemScreenPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeBlockPos(msg.anchorPos);
        buf.writeVarInt(msg.workRadius);
        buf.writeBoolean(msg.hasShovel);
        buf.writeVarInt(msg.sweepW);
        buf.writeVarInt(msg.sweepL);
        buf.writeVarInt(msg.sweepH);
    }

    public static ClientboundOpenGolemScreenPacket decode(FriendlyByteBuf buf) {
        return new ClientboundOpenGolemScreenPacket(
                buf.readVarInt(),
                buf.readBlockPos(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public static void handle(ClientboundOpenGolemScreenPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientScreenHooks.openGolemConfig(
                        msg.entityId,
                        msg.anchorPos,
                        msg.workRadius,
                        msg.hasShovel,
                        msg.sweepW,
                        msg.sweepL,
                        msg.sweepH
                )
        ));
        ctx.setPacketHandled(true);
    }
}
