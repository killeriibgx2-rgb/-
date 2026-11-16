package com.eternalsnow.network;

import com.eternalsnow.state.HeatScanClientData;
import com.eternalsnow.thermal.ThermalEngine;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v2.6.0: one compact packet carries BOTH the scan grid and the two look-anchored markers.
 * Everything heavy (block sampling, min/max palette, hottest/coldest search) runs on the SERVER:
 * the weak client only receives short arrays and a couple of positions.
 */
public final class ClientboundHeatGridPacket {
    public static final int RADIUS = 10;
    private static final int MAX_CELLS = 900;
    private static final int MARKER_RADIUS = 4;
    private static final long NO_POS = Long.MIN_VALUE;

    private final BlockPos origin;
    private final short[] packedOffsets;
    private final short[] deciTemps;
    private final int mode;
    private final float minC;
    private final float maxC;
    private final long hotPos;
    private final float hotC;
    private final long coldPos;
    private final float coldC;

    public ClientboundHeatGridPacket(BlockPos origin, short[] packedOffsets, short[] deciTemps,
                                     int mode, float minC, float maxC,
                                     long hotPos, float hotC, long coldPos, float coldC) {
        this.origin = origin;
        this.packedOffsets = packedOffsets;
        this.deciTemps = deciTemps;
        this.mode = mode;
        this.minC = minC;
        this.maxC = maxC;
        this.hotPos = hotPos;
        this.hotC = hotC;
        this.coldPos = coldPos;
        this.coldC = coldC;
    }

    /** Legacy constructor kept so older call sites stay source-compatible. */
    public ClientboundHeatGridPacket(BlockPos origin, short[] packedOffsets, short[] deciTemps) {
        this(origin, packedOffsets, deciTemps, 0, -25.0F, 55.0F, NO_POS, 0.0F, NO_POS, 0.0F);
    }

    public static ClientboundHeatGridPacket captureAround(ServerLevel level, BlockPos center) {
        return capture(level, center, null, 0);
    }

    public static ClientboundHeatGridPacket captureFor(ServerLevel level, ServerPlayer player, int mode) {
        return capture(level, player.blockPosition().above(), player, mode);
    }

    private static ClientboundHeatGridPacket capture(ServerLevel level, BlockPos center,
                                                     @javax.annotation.Nullable ServerPlayer player, int mode) {
        boolean walls = mode >= 2;
        ShortArrayList offsets = new ShortArrayList(MAX_CELLS);
        ShortArrayList temps = new ShortArrayList(MAX_CELLS);
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos nPos = new BlockPos.MutableBlockPos();

        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int rSq = RADIUS * RADIUS;
        float lo = Float.MAX_VALUE;
        float hi = -Float.MAX_VALUE;

        outer:
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
                    boolean air = ThermalEngine.isAirOrOpenPassage(state);
                    if (walls) {
                        // Walls only: solid blocks that actually have a face exposed to air.
                        if (air || !hasAirNeighbour(level, wx, wy, wz, nPos)) {
                            continue;
                        }
                    } else if (!air) {
                        continue;
                    }

                    float tempC = ThermalEngine.getLocalTemperatureC(level, mPos);
                    boolean warmed = ThermalEngine.getDeltaHeatAt(level, mPos) >= 0.35F;

                    int packed = ((dx + RADIUS) & 0x1F)
                            | (((dy + RADIUS) & 0x1F) << 5)
                            | (((dz + RADIUS) & 0x1F) << 10)
                            | (warmed ? 0x8000 : 0);
                    int deciT = Mth.clamp(Math.round(tempC * 10.0F), -1000, 3000);

                    offsets.add((short) packed);
                    temps.add((short) deciT);
                    if (tempC < lo) lo = tempC;
                    if (tempC > hi) hi = tempC;
                    if (offsets.size() >= MAX_CELLS) {
                        break outer;
                    }
                }
            }
        }

        if (lo == Float.MAX_VALUE) {
            lo = com.eternalsnow.state.EternalSnowState.worldAmbientTempC;
            hi = lo + 1.0F;
        }
        if (hi - lo < 0.6F) {
            float mid = (hi + lo) * 0.5F;
            lo = mid - 0.3F;
            hi = mid + 0.3F;
        }

        long hotP = NO_POS, coldP = NO_POS;
        float hotT = 0.0F, coldT = 0.0F;
        if (player != null) {
            HitResult hit = player.pick(8.0D, 1.0F, false);
            if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult bhr) {
                BlockPos target = bhr.getBlockPos();
                float mh = -Float.MAX_VALUE, mc = Float.MAX_VALUE;
                for (int dx = -MARKER_RADIUS; dx <= MARKER_RADIUS; dx++) {
                    for (int dy = -MARKER_RADIUS; dy <= MARKER_RADIUS; dy++) {
                        for (int dz = -MARKER_RADIUS; dz <= MARKER_RADIUS; dz++) {
                            if (dx * dx + dy * dy + dz * dz > MARKER_RADIUS * MARKER_RADIUS) {
                                continue;
                            }
                            int x = target.getX() + dx, y = target.getY() + dy, z = target.getZ() + dz;
                            mPos.set(x, y, z);
                            BlockState st = level.getBlockState(mPos);
                            if (ThermalEngine.isAirOrOpenPassage(st)) {
                                continue;
                            }
                            float t = ThermalEngine.getLocalTemperatureC(level, mPos);
                            if (t > mh) {
                                mh = t;
                                hotP = mPos.asLong();
                                hotT = t;
                            }
                            if (t < mc) {
                                mc = t;
                                coldP = mPos.asLong();
                                coldT = t;
                            }
                        }
                    }
                }
            }
        }

        return new ClientboundHeatGridPacket(center, offsets.toShortArray(), temps.toShortArray(),
                mode, lo, hi, hotP, hotT, coldP, coldT);
    }

    private static boolean hasAirNeighbour(ServerLevel level, int x, int y, int z,
                                           BlockPos.MutableBlockPos tmp) {
        for (Direction d : Direction.values()) {
            tmp.set(x + d.getStepX(), y + d.getStepY(), z + d.getStepZ());
            if (ThermalEngine.isAirOrOpenPassage(level.getBlockState(tmp))) {
                return true;
            }
        }
        return false;
    }

    public static void encode(ClientboundHeatGridPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.origin);
        int len = msg.packedOffsets.length;
        buf.writeVarInt(len);
        for (int i = 0; i < len; i++) {
            buf.writeShort(msg.packedOffsets[i]);
            buf.writeShort(msg.deciTemps[i]);
        }
        buf.writeByte(msg.mode);
        buf.writeFloat(msg.minC);
        buf.writeFloat(msg.maxC);
        buf.writeLong(msg.hotPos);
        buf.writeFloat(msg.hotC);
        buf.writeLong(msg.coldPos);
        buf.writeFloat(msg.coldC);
    }

    public static ClientboundHeatGridPacket decode(FriendlyByteBuf buf) {
        BlockPos origin = buf.readBlockPos();
        int len = Mth.clamp(buf.readVarInt(), 0, 2000);
        short[] offsets = new short[len];
        short[] temps = new short[len];
        for (int i = 0; i < len; i++) {
            offsets[i] = buf.readShort();
            temps[i] = buf.readShort();
        }
        int mode = buf.readByte() & 0xFF;
        float minC = buf.readFloat();
        float maxC = buf.readFloat();
        long hotPos = buf.readLong();
        float hotC = buf.readFloat();
        long coldPos = buf.readLong();
        float coldC = buf.readFloat();
        return new ClientboundHeatGridPacket(origin, offsets, temps, mode, minC, maxC,
                hotPos, hotC, coldPos, coldC);
    }

    public static void handle(ClientboundHeatGridPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> HeatScanClientData.update(msg.origin, msg.packedOffsets, msg.deciTemps,
                msg.mode, msg.minC, msg.maxC, msg.hotPos, msg.hotC, msg.coldPos, msg.coldC));
        ctx.setPacketHandled(true);
    }
}
