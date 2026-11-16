package com.eternalsnow.network;

import com.eternalsnow.client.ClientScreenHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientboundOpenChimneyScreenPacket {
    private final BlockPos fireplaceMasterPos;
    private final int damperPercent;
    private final int fuelMilli;
    private final boolean lit;
    private final boolean chimneySkyValid;

    public ClientboundOpenChimneyScreenPacket(
            BlockPos fireplaceMasterPos,
            int damperPercent,
            int fuelMilli,
            boolean lit,
            boolean chimneySkyValid
    ) {
        this.fireplaceMasterPos = fireplaceMasterPos;
        this.damperPercent = damperPercent;
        this.fuelMilli = fuelMilli;
        this.lit = lit;
        this.chimneySkyValid = chimneySkyValid;
    }

    public static void encode(ClientboundOpenChimneyScreenPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.fireplaceMasterPos);
        buf.writeVarInt(msg.damperPercent);
        buf.writeVarInt(msg.fuelMilli);
        buf.writeBoolean(msg.lit);
        buf.writeBoolean(msg.chimneySkyValid);
    }

    public static ClientboundOpenChimneyScreenPacket decode(FriendlyByteBuf buf) {
        return new ClientboundOpenChimneyScreenPacket(
                buf.readBlockPos(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }

    public static void handle(ClientboundOpenChimneyScreenPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientScreenHooks.openChimneyDamper(
                msg.fireplaceMasterPos,
                msg.damperPercent,
                msg.fuelMilli,
                msg.lit,
                msg.chimneySkyValid
        )));
        ctx.setPacketHandled(true);
    }
}
