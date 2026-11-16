package com.eternalsnow.network;

import com.eternalsnow.block.FireplaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ServerboundSetChimneyDamperPacket {
    private final BlockPos fireplaceMasterPos;
    private final int damperPercent;

    public ServerboundSetChimneyDamperPacket(BlockPos fireplaceMasterPos, int damperPercent) {
        this.fireplaceMasterPos = fireplaceMasterPos;
        this.damperPercent = Mth.clamp(damperPercent, 0, 100);
    }

    public static void encode(ServerboundSetChimneyDamperPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.fireplaceMasterPos);
        buf.writeVarInt(msg.damperPercent);
    }

    public static ServerboundSetChimneyDamperPacket decode(FriendlyByteBuf buf) {
        return new ServerboundSetChimneyDamperPacket(
                buf.readBlockPos(),
                buf.readVarInt()
        );
    }

    public static void handle(ServerboundSetChimneyDamperPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null || !(sender.level() instanceof ServerLevel sl)) {
                return;
            }
            // Allow adjusting from anywhere along a tall 48-block chimney tower!
            if (sender.distanceToSqr(
                    msg.fireplaceMasterPos.getX() + 0.5D,
                    sender.getY(),
                    msg.fireplaceMasterPos.getZ() + 0.5D) > 256.0D) {
                return;
            }
            if (!sl.hasChunkAt(msg.fireplaceMasterPos)) {
                return;
            }
            if (sl.getBlockEntity(msg.fireplaceMasterPos) instanceof FireplaceBlockEntity be) {
                int oldPct = be.getDamperPercent();
                be.setDamperPercent(msg.damperPercent, sender);
                if (oldPct != be.getDamperPercent()) {
                    sl.playSound(null, sender.blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS,
                            0.35F, 0.85F + (be.getDamperPercent() / 100.0F) * 0.45F);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
