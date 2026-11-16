package com.eternalsnow.network;

import com.eternalsnow.state.HeatScanClientData;
import com.eternalsnow.thermal.ThermalEngine;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientboundHeatGridPacket {
    public static final int RADIUS = 10;

    private final BlockPos origin;
    private final short[] packedOffsets;
    private final short[] deciTemps;

    public ClientboundHeatGridPacket(BlockPos origin, short[] packedOffsets, short[] deciTemps) {
        this.origin = origin;
        this.packedOffsets = packedOffsets;
        this.deciTemps = deciTemps;
    }

    public static ClientboundHeatGridPacket captureAround(ServerLevel level, BlockPos center) {
        ShortArrayList offsets = new ShortArrayList(2200);
        ShortArrayList temps = new ShortArrayList(2200);
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int rSq = RADIUS * RADIUS;

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int xzSq = dx * dx + dz * dz;
                if (xzSq > rSq) {
                    continue;
                }
                int wx = cx + dx;
                int wz = cz + dz;
                LevelChunk chunk = level.getChunkSource().getChunkNow(wx >> 4, wz >> 4);
                if (chunk == null) {
                    continue;
                }
                for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                    if (xzSq + dy * dy > rSq) {
                        continue;
                    }
                    int wy = cy + dy;
                    if (wy < level.getMinBuildHeight() || wy >= level.getMaxBuildHeight()) {
                        continue;
                    }
                    mPos.set(wx, wy, wz);
                    BlockState state = chunk.getBlockState(mPos);
                    if (!ThermalEngine.isAirOrOpenPassage(state)) {
                        continue;
                    }

                    float tempC = ThermalEngine.getLocalTemperatureC(level, mPos);
                    boolean warmed = ThermalEngine.getDeltaHeatAt(level, mPos) >= 0.35F;

                    // Pack dx, dy, dz (-10..+10 -> 0..20 = 5 bits each) + bit 15 = warmed flag
                    int packed = ((dx + RADIUS) & 0x1F)
                            | (((dy + RADIUS) & 0x1F) << 5)
                            | (((dz + RADIUS) & 0x1F) << 10)
                            | (warmed ? 0x8000 : 0);
                    int deciT = Mth.clamp(Math.round(tempC * 10.0F), -1000, 3000);

                    offsets.add((short) packed);
                    temps.add((short) deciT);
                }
            }
        }

        return new ClientboundHeatGridPacket(center, offsets.toShortArray(), temps.toShortArray());
    }

    public static void encode(ClientboundHeatGridPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.origin);
        int len = msg.packedOffsets.length;
        buf.writeVarInt(len);
        for (int i = 0; i < len; i++) {
            buf.writeShort(msg.packedOffsets[i]);
            buf.writeShort(msg.deciTemps[i]);
        }
    }

    public static ClientboundHeatGridPacket decode(FriendlyByteBuf buf) {
        BlockPos origin = buf.readBlockPos();
        int len = Mth.clamp(buf.readVarInt(), 0, 10000);
        short[] offsets = new short[len];
        short[] temps = new short[len];
        for (int i = 0; i < len; i++) {
            offsets[i] = buf.readShort();
            temps[i] = buf.readShort();
        }
        return new ClientboundHeatGridPacket(origin, offsets, temps);
    }

    public static void handle(ClientboundHeatGridPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> HeatScanClientData.update(msg.origin, msg.packedOffsets, msg.deciTemps));
        ctx.setPacketHandled(true);
    }
}
