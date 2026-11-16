package com.eternalsnow.client;

import com.eternalsnow.EternalSnowMod;
import com.eternalsnow.config.ClientSnowConfig;
import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.weather.WeatherEventType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import org.joml.Matrix4f;

import java.util.SplittableRandom;

/**
 * Maximum-Performance Stateful World-Space 3D Snowflake Pool (v2.5.0):
 * - Uses Dense ACTIVE_LIST + FREE_STACK (swap-erase in O(1)) so ticks & renders NEVER iterate inactive pool slots!
 * - Also spawns atmospheric low ground Spindrift («Позёмка», Д-3) skimming across snowdrifts during Gale & Blizzard!
 */
public final class WindSnowRenderer {
    private static final ResourceLocation VOXEL_SNOW_TEX = new ResourceLocation(
            EternalSnowMod.MODID, "textures/environment/voxel_snow.png"
    );

    private static final int MAX_POOL = 16384;

    private static final int[] ACTIVE_LIST = new int[MAX_POOL];
    private static final int[] FREE_STACK = new int[MAX_POOL];
    private static int activeCount = 0;
    private static int freeTop = MAX_POOL;

    private static final double[] POS_X = new double[MAX_POOL];
    private static final double[] POS_Y = new double[MAX_POOL];
    private static final double[] POS_Z = new double[MAX_POOL];
    private static final double[] PREV_X = new double[MAX_POOL];
    private static final double[] PREV_Y = new double[MAX_POOL];
    private static final double[] PREV_Z = new double[MAX_POOL];

    private static final float[] SPEED_MULT = new float[MAX_POOL];
    private static final float[] HX = new float[MAX_POOL];
    private static final float[] HY = new float[MAX_POOL];
    private static final float[] HZ = new float[MAX_POOL];
    private static final float[] UV_U0 = new float[MAX_POOL];
    private static final int[] ROT_OFFSET = new int[MAX_POOL];
    private static final int[] ROT_RATE = new int[MAX_POOL];
    private static final int[] SWIRL_PHASE = new int[MAX_POOL];
    private static final short[] AGE = new short[MAX_POOL];
    private static final short[] MAX_AGE = new short[MAX_POOL];

    private static final SplittableRandom RNG = new SplittableRandom(0x2026E7E24A15L);

    private static final int ROT_STEPS = 256;
    private static final int ROT_MASK = ROT_STEPS - 1;
    private static final float[] BASIS_UX = new float[ROT_STEPS];
    private static final float[] BASIS_UY = new float[ROT_STEPS];
    private static final float[] BASIS_UZ = new float[ROT_STEPS];
    private static final float[] BASIS_VX = new float[ROT_STEPS];
    private static final float[] BASIS_VY = new float[ROT_STEPS];
    private static final float[] BASIS_VZ = new float[ROT_STEPS];
    private static final float[] BASIS_WX = new float[ROT_STEPS];
    private static final float[] BASIS_WY = new float[ROT_STEPS];
    private static final float[] BASIS_WZ = new float[ROT_STEPS];

    private static final int SWIRL_STEPS = 64;
    private static final int SWIRL_MASK = SWIRL_STEPS - 1;
    private static final float[] SWIRL_SIN = new float[SWIRL_STEPS];
    private static final float[] SWIRL_COS = new float[SWIRL_STEPS];

    static {
        for (int r = 0; r < ROT_STEPS; r++) {
            float t = (r / (float) ROT_STEPS) * ((float) Math.PI * 2.0F);
            float yaw = t * 2.0F;
            float pitch = t * 3.0F + 0.55F;
            float roll = t * 1.0F + 1.15F;

            float cy = Mth.cos(yaw), sy = Mth.sin(yaw);
            float cp = Mth.cos(pitch), sp = Mth.sin(pitch);
            float cr = Mth.cos(roll), sr = Mth.sin(roll);

            BASIS_UX[r] = cy * cr + sy * sp * sr;
            BASIS_UY[r] = cp * sr;
            BASIS_UZ[r] = -sy * cr + cy * sp * sr;

            BASIS_VX[r] = -cy * sr + sy * sp * cr;
            BASIS_VY[r] = cp * cr;
            BASIS_VZ[r] = sy * sr + cy * sp * cr;

            BASIS_WX[r] = sy * cp;
            BASIS_WY[r] = -sp;
            BASIS_WZ[r] = cy * cp;
        }

        for (int k = 0; k < SWIRL_STEPS; k++) {
            float a = (k / (float) SWIRL_STEPS) * ((float) Math.PI * 2.0F);
            SWIRL_SIN[k] = Mth.sin(a);
            SWIRL_COS[k] = Mth.cos(a);
        }

        long s = 0x5EED2026CAFEBABEL;
        for (int i = 0; i < MAX_POOL; i++) {
            FREE_STACK[i] = (MAX_POOL - 1) - i;
            s = s * 6364136223846793005L + 1442695040888963407L;
            SPEED_MULT[i] = 0.82F + (((s >>> 40) & 0xFFFFFF) / (float) 0x1000000) * 0.38F;

            s = s * 6364136223846793005L + 1442695040888963407L;
            float baseHalf = 0.036F + (((s >>> 40) & 0xFFFFFF) / (float) 0x1000000) * 0.032F;
            s = s * 6364136223846793005L + 1442695040888963407L;
            float a1 = 0.84F + (((s >>> 40) & 0xFFFFFF) / (float) 0x1000000) * 0.32F;
            s = s * 6364136223846793005L + 1442695040888963407L;
            float a2 = 0.82F + (((s >>> 40) & 0xFFFFFF) / (float) 0x1000000) * 0.34F;
            HX[i] = baseHalf * a1;
            HY[i] = baseHalf * a2;
            HZ[i] = baseHalf;

            s = s * 6364136223846793005L + 1442695040888963407L;
            ROT_OFFSET[i] = (int) ((s >>> 40) & ROT_MASK);
            s = s * 6364136223846793005L + 1442695040888963407L;
            int dir = ((s >>> 39) & 1) == 0 ? 1 : -1;
            ROT_RATE[i] = dir * (1 + (int) ((s >>> 42) & 0x3));

            s = s * 6364136223846793005L + 1442695040888963407L;
            SWIRL_PHASE[i] = (int) ((s >>> 40) & SWIRL_MASK);

            s = s * 6364136223846793005L + 1442695040888963407L;
            UV_U0[i] = (((s >>> 40) & 0x7) * 0.0625F);
        }
    }

    private WindSnowRenderer() {
    }

    public static void resetCache() {
        activeCount = 0;
        freeTop = MAX_POOL;
        for (int i = 0; i < MAX_POOL; i++) {
            FREE_STACK[i] = (MAX_POOL - 1) - i;
        }
    }

    private static void killAtDenseIndex(int denseIdx) {
        int slot = ACTIVE_LIST[denseIdx];
        FREE_STACK[freeTop++] = slot;
        activeCount--;
        ACTIVE_LIST[denseIdx] = ACTIVE_LIST[activeCount];
    }

    private static int allocSlot() {
        if (freeTop <= 0) {
            return -1;
        }
        int slot = FREE_STACK[--freeTop];
        ACTIVE_LIST[activeCount++] = slot;
        return slot;
    }

    private static int computeTargetActiveFlakes(WeatherEventType ev, float liveSnowIntensity) {
        if (ev == WeatherEventType.CLEAR_FROST && liveSnowIntensity < 0.04F) {
            return 0;
        }
        int base = Mth.clamp(Math.round(ClientSnowConfig.snowParticleCount * 0.68F), 300, MAX_POOL);
        if (ClientSnowConfig.autoWeatherParticles) {
            float weatherScale = switch (ev) {
                case CLEAR_FROST -> 0.0F;
                case LIGHT_SNOW -> 0.45F;
                case HEAVY_SNOW -> 1.05F;
                case GALE -> 1.15F;
                case BLIZZARD -> 1.85F;
            };
            float fade = Mth.clamp(liveSnowIntensity * 1.45F, 0.0F, 1.25F);
            if (ev == WeatherEventType.CLEAR_FROST) {
                return Mth.clamp(Math.round(base * liveSnowIntensity * 0.7F), 0, MAX_POOL);
            }
            return Mth.clamp(Math.round(base * Math.max(weatherScale * 0.65F, weatherScale * fade)), 0, MAX_POOL);
        } else {
            return (ev == WeatherEventType.CLEAR_FROST)
                    ? Mth.clamp(Math.round(base * liveSnowIntensity), 0, MAX_POOL)
                    : base;
        }
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) {
            return;
        }
        ClientLevel level = mc.level;
        Player player = mc.player;
        if (level == null || player == null || !EternalSnowState.isOverworldLike(level)) {
            if (activeCount > 0) {
                resetCache();
            }
            return;
        }

        ServerSnowSettings settings = EternalSnowState.getEffectiveSettings();
        if (!settings.enabled || !ClientSnowConfig.visibleSnowfall) {
            if (activeCount > 0) {
                resetCache();
            }
            return;
        }

        WeatherEventType ev = EternalSnowState.getActiveWeatherEvent();
        int targetActive = computeTargetActiveFlakes(ev, EternalSnowState.snowIntensity);

        float radius = Mth.clamp(ClientSnowConfig.snowfallRadius, 8, 40);
        if (ClientSnowConfig.autoWeatherParticles && ev == WeatherEventType.BLIZZARD) {
            radius = Math.min(radius, 26.0F);
        }
        float despawnRadius = radius + 8.0F;
        double despawnHorizSq = despawnRadius * despawnRadius;

        Vec3 eye = player.getEyePosition();
        double px = eye.x;
        double py = eye.y;
        double pz = eye.z;

        double pvx = Mth.clamp(player.getX() - player.xo, -2.5D, 2.5D);
        double pvy = Mth.clamp(player.getY() - player.yo, -2.5D, 2.5D);
        double pvz = Mth.clamp(player.getZ() - player.zo, -2.5D, 2.5D);

        float windSpeed = EternalSnowState.windSpeed;
        float wx = EternalSnowState.getWindDirX();
        float wz = EternalSnowState.getWindDirZ();

        float fallPerTick = switch (ev) {
            case CLEAR_FROST -> 0.075F;
            case LIGHT_SNOW -> 0.095F;
            case HEAVY_SNOW -> 0.180F;
            case GALE -> 0.240F;
            case BLIZZARD -> 0.370F;
        };
        float windPerTick = switch (ev) {
            case CLEAR_FROST -> windSpeed * 0.08F;
            case LIGHT_SNOW -> windSpeed * 0.18F;
            case HEAVY_SNOW -> windSpeed * 0.38F;
            case GALE -> windSpeed * 0.78F;
            case BLIZZARD -> windSpeed * 1.05F;
        };

        boolean leavesPassThrough = (settings.leavesMode == 2);
        boolean checkMidpoint = (windPerTick > 0.42F || fallPerTick > 0.35F);
        int tickMod = (int) (level.getGameTime() & SWIRL_MASK);
        float flutterAmp = 0.018F + windSpeed * 0.025F;

        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        int prevChunkX = Integer.MIN_VALUE;
        int prevChunkZ = Integer.MIN_VALUE;
        LevelChunk cachedChunk = null;

        // 1. Dense ACTIVE_LIST iteration (ONLY visits living snowflakes — 0 wasted checks!)
        int dIdx = 0;
        while (dIdx < activeCount) {
            int i = ACTIVE_LIST[dIdx];
            double x = POS_X[i];
            double y = POS_Y[i];
            double z = POS_Z[i];

            double dx = x - px;
            double dy = y - py;
            double dz = z - pz;
            short nextAge = (short) (AGE[i] + 1);
            if (nextAge >= MAX_AGE[i] || (dx * dx + dz * dz) > despawnHorizSq || dy < -26.0D || dy > 24.0D) {
                killAtDenseIndex(dIdx);
                continue;
            }
            AGE[i] = nextAge;

            PREV_X[i] = x;
            PREV_Y[i] = y;
            PREV_Z[i] = z;

            float sm = SPEED_MULT[i];
            int swIdx = (tickMod + SWIRL_PHASE[i]) & SWIRL_MASK;
            double stepX = (windPerTick * wx * sm) + SWIRL_SIN[swIdx] * flutterAmp;
            double stepY = -(fallPerTick * sm);
            double stepZ = (windPerTick * wz * sm) + SWIRL_COS[swIdx] * flutterAmp;

            double nx = x + stepX;
            double ny = y + stepY;
            double nz = z + stepZ;

            if (checkMidpoint) {
                int mbx = Mth.floor((x + nx) * 0.5D);
                int mby = Mth.floor((y + ny) * 0.5D);
                int mbz = Mth.floor((z + nz) * 0.5D);
                int mcx = mbx >> 4, mcz = mbz >> 4;
                if (mcx != prevChunkX || mcz != prevChunkZ) {
                    prevChunkX = mcx;
                    prevChunkZ = mcz;
                    cachedChunk = level.getChunkSource().getChunkNow(mcx, mcz);
                }
                if (cachedChunk == null || isBlockedByWorld(level, cachedChunk, mpos.set(mbx, mby, mbz), leavesPassThrough)) {
                    killAtDenseIndex(dIdx);
                    continue;
                }
            }

            int bx = Mth.floor(nx);
            int by = Mth.floor(ny);
            int bz = Mth.floor(nz);
            int cx = bx >> 4, cz = bz >> 4;
            if (cx != prevChunkX || cz != prevChunkZ) {
                prevChunkX = cx;
                prevChunkZ = cz;
                cachedChunk = level.getChunkSource().getChunkNow(cx, cz);
            }

            if (cachedChunk == null || isBlockedByWorld(level, cachedChunk, mpos.set(bx, by, bz), leavesPassThrough)) {
                killAtDenseIndex(dIdx);
                continue;
            }

            POS_X[i] = nx;
            POS_Y[i] = ny;
            POS_Z[i] = nz;
            dIdx++;
        }

        // Д-3: Low Ground Spindrift («Позёмка») skimming across snowdrifts in Gale & Blizzard!
        if ((ev == WeatherEventType.GALE || ev == WeatherEventType.BLIZZARD) && windSpeed > 0.45F) {
            int streaks = (ev == WeatherEventType.BLIZZARD) ? 3 : 1;
            for (int s = 0; s < streaks; s++) {
                double gx = px + (RNG.nextDouble() * 2.0D - 1.0D) * 14.0D;
                double gz = pz + (RNG.nextDouble() * 2.0D - 1.0D) * 14.0D;
                int gbx = Mth.floor(gx);
                int gbz = Mth.floor(gz);
                int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, gbx, gbz);
                if (Math.abs(topY - py) <= 10.0D) {
                    mpos.set(gbx, topY - 1, gbz);
                    BlockState ground = level.getBlockState(mpos);
                    if (ground.is(Blocks.SNOW) || ground.is(Blocks.SNOW_BLOCK) || level.getBlockState(mpos.above()).is(Blocks.SNOW)) {
                        level.addParticle(
                                ParticleTypes.SNOWFLAKE,
                                gx, topY + 0.14D, gz,
                                wx * windSpeed * 0.42D, 0.01D, wz * windSpeed * 0.42D
                        );
                    }
                }
            }
        }

        if (targetActive <= 0 || activeCount >= targetActive) {
            return;
        }

        // 2. Spawn new independent snowflakes in open sky
        int deficit = targetActive - activeCount;
        boolean initialPreWarm = (activeCount < targetActive * 0.30F);
        int spawnBudget = initialPreWarm
                ? Math.min(deficit, 3200)
                : Math.min(deficit, Math.max(90, targetActive / 11));

        double centerSpawnX = px + pvx * 7.5D - wx * (windPerTick * 12.0D);
        double centerSpawnZ = pz + pvz * 7.5D - wz * (windPerTick * 12.0D);
        double centerSpawnY = py + pvy * 4.0D;

        int attempts = spawnBudget * 2;
        int spawned = 0;

        for (int a = 0; a < attempts && spawned < spawnBudget; a++) {
            double sx, sy, sz;
            if (initialPreWarm || (a & 3) == 0) {
                sx = centerSpawnX + (RNG.nextDouble() * 2.0D - 1.0D) * radius;
                sz = centerSpawnZ + (RNG.nextDouble() * 2.0D - 1.0D) * radius;
                sy = centerSpawnY + (RNG.nextDouble() * 36.0D - 18.0D);
            } else {
                sx = centerSpawnX + (RNG.nextDouble() * 2.0D - 1.0D) * radius;
                sz = centerSpawnZ + (RNG.nextDouble() * 2.0D - 1.0D) * radius;
                sy = centerSpawnY + 6.0D + RNG.nextDouble() * 13.0D;
            }

            int bx = Mth.floor(sx);
            int by = Mth.floor(sy);
            int bz = Mth.floor(sz);
            if (by <= level.getMinBuildHeight() || by >= level.getMaxBuildHeight() - 1) {
                continue;
            }

            int cx = bx >> 4, cz = bz >> 4;
            if (cx != prevChunkX || cz != prevChunkZ) {
                prevChunkX = cx;
                prevChunkZ = cz;
                cachedChunk = level.getChunkSource().getChunkNow(cx, cz);
            }
            if (cachedChunk == null) {
                continue;
            }

            int motionTop = cachedChunk.getHeight(Heightmap.Types.MOTION_BLOCKING, bx & 15, bz & 15);
            if (by < motionTop) {
                if (!leavesPassThrough) {
                    continue;
                }
                mpos.set(bx, by, bz);
                if (level.getBrightness(LightLayer.SKY, mpos) < 14) {
                    continue;
                }
                if (isBlockedByWorld(level, cachedChunk, mpos, true)) {
                    continue;
                }
            } else {
                mpos.set(bx, by, bz);
                if (isBlockedByWorld(level, cachedChunk, mpos, leavesPassThrough)) {
                    continue;
                }
            }

            int slot = allocSlot();
            if (slot < 0) {
                break;
            }

            POS_X[slot] = sx;
            POS_Y[slot] = sy;
            POS_Z[slot] = sz;
            PREV_X[slot] = sx;
            PREV_Y[slot] = sy;
            PREV_Z[slot] = sz;
            AGE[slot] = 0;
            MAX_AGE[slot] = (short) (160 + RNG.nextInt(180));
            spawned++;
        }
    }

    private static boolean isBlockedByWorld(
            ClientLevel level,
            LevelChunk chunk,
            BlockPos pos,
            boolean leavesPassThrough
    ) {
        BlockState st = chunk.getBlockState(pos);
        if (st.isAir()) {
            return false;
        }
        if (leavesPassThrough && st.is(BlockTags.LEAVES)) {
            return false;
        }
        // Fast boolean full-cube check resolves 95% of stone/wood/dirt/glass blocks without VoxelShape!
        if (st.isCollisionShapeFullBlock(chunk, pos) || st.is(Blocks.SNOW) || !st.getFluidState().isEmpty()) {
            return true;
        }
        if (st.getBlock() instanceof DoorBlock) {
            return !st.getValue(DoorBlock.OPEN);
        }
        if (st.getBlock() instanceof TrapDoorBlock) {
            return !st.getValue(TrapDoorBlock.OPEN);
        }
        return !st.getCollisionShape(chunk, pos).isEmpty();
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (!ClientSnowConfig.visibleSnowfall || activeCount <= 0) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !EternalSnowState.isOverworldLike(level)) {
            return;
        }

        ServerSnowSettings settings = EternalSnowState.getEffectiveSettings();
        if (!settings.enabled) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        double cx = camPos.x;
        double cy = camPos.y;
        double cz = camPos.z;

        float partialTick = event.getPartialTick();
        float radius = Mth.clamp(ClientSnowConfig.snowfallRadius, 8, 40);
        float maxDistSq = (radius + 6.0F) * (radius + 6.0F);
        float invMaxDistSq = 1.0F / maxDistSq;

        Matrix4f pose = event.getPoseStack().last().pose();
        Matrix4f mvp = new Matrix4f(event.getProjectionMatrix())
                .mul(RenderSystem.getModelViewMatrix())
                .mul(pose);
        float m00 = mvp.m00(), m10 = mvp.m10(), m20 = mvp.m20(), m30 = mvp.m30();
        float m01 = mvp.m01(), m11 = mvp.m11(), m21 = mvp.m21(), m31 = mvp.m31();
        float m03 = mvp.m03(), m13 = mvp.m13(), m23 = mvp.m23(), m33 = mvp.m33();

        double timeSec = (level.getGameTime() + partialTick) / 20.0D;
        int rotAnimTick = (int) (timeSec * (11.0D + EternalSnowState.windSpeed * 15.0D));

        float sizeMult = ClientSnowConfig.snowflakeSizePercent / 100.0F;
        float opacityMult = Mth.clamp(ClientSnowConfig.snowflakeOpacityPercent / 100.0F, 0.20F, 1.0F);
        boolean solidDepth = opacityMult >= 0.85F;

        float skyStar = level.getStarBrightness(partialTick);
        float skyBright = Mth.clamp(1.0F - skyStar * 0.36F, 0.62F, 1.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(solidDepth);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, VOXEL_SNOW_TEX);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        int drawn = 0;

        // Dense loop over 0 .. activeCount - 1 ONLY!
        for (int dIdx = 0; dIdx < activeCount; dIdx++) {
            int i = ACTIVE_LIST[dIdx];

            float rx = (float) (Mth.lerp(partialTick, PREV_X[i], POS_X[i]) - cx);
            float ry = (float) (Mth.lerp(partialTick, PREV_Y[i], POS_Y[i]) - cy);
            float rz = (float) (Mth.lerp(partialTick, PREV_Z[i], POS_Z[i]) - cz);

            float horizSq = rx * rx + rz * rz;
            if (horizSq > maxDistSq) {
                continue;
            }

            float wClip = m03 * rx + m13 * ry + m23 * rz + m33;
            if (wClip < 0.14F) {
                continue;
            }
            float clipLim = wClip * 1.08F + 0.32F;
            float xClip = m00 * rx + m10 * ry + m20 * rz + m30;
            if (xClip < -clipLim || xClip > clipLim) {
                continue;
            }
            float yClip = m01 * rx + m11 * ry + m21 * rz + m31;
            if (yClip < -clipLim || yClip > clipLim) {
                continue;
            }

            float birthFade = AGE[i] < 4 ? (AGE[i] + partialTick) * 0.25F : 1.0F;
            float distFade = Mth.clamp((1.0F - horizSq * invMaxDistSq) * 4.0F, 0.0F, 1.0F);
            int alpha = Mth.clamp((int) (opacityMult * birthFade * distFade * 255.0F), 0, 255);
            if (alpha < 20) {
                continue;
            }

            int rotIdx = (ROT_OFFSET[i] + rotAnimTick * ROT_RATE[i]) & ROT_MASK;
            float ux = BASIS_UX[rotIdx], uy = BASIS_UY[rotIdx], uz = BASIS_UZ[rotIdx];
            float vx = BASIS_VX[rotIdx], vy = BASIS_VY[rotIdx], vz = BASIS_VZ[rotIdx];
            float wxAxis = BASIS_WX[rotIdx], wyAxis = BASIS_WY[rotIdx], wzAxis = BASIS_WZ[rotIdx];

            float hx = HX[i] * sizeMult;
            float hy = HY[i] * sizeMult;
            float hz = HZ[i] * sizeMult;

            float e0x = ux * hx, e0y = uy * hx, e0z = uz * hx;
            float e1x = vx * hy, e1y = vy * hy, e1z = vz * hy;
            float e2x = wxAxis * hz, e2y = wyAxis * hz, e2z = wzAxis * hz;

            float u0 = UV_U0[i];
            float u1 = u0 + 0.5F;

            if (rx * ux + ry * uy + rz * uz < 0.0F) {
                emitShadedQuad(buf, pose, rx + e0x, ry + e0y, rz + e0z, e1x, e1y, e1z, e2x, e2y, e2z, ux, uy, uz, skyBright, alpha, u0, u1);
            } else {
                emitShadedQuad(buf, pose, rx - e0x, ry - e0y, rz - e0z, e2x, e2y, e2z, e1x, e1y, e1z, -ux, -uy, -uz, skyBright, alpha, u0, u1);
            }

            if (rx * vx + ry * vy + rz * vz < 0.0F) {
                emitShadedQuad(buf, pose, rx + e1x, ry + e1y, rz + e1z, e2x, e2y, e2z, e0x, e0y, e0z, vx, vy, vz, skyBright, alpha, u0, u1);
            } else {
                emitShadedQuad(buf, pose, rx - e1x, ry - e1y, rz - e1z, e0x, e0y, e0z, e2x, e2y, e2z, -vx, -vy, -vz, skyBright, alpha, u0, u1);
            }

            if (rx * wxAxis + ry * wyAxis + rz * wzAxis < 0.0F) {
                emitShadedQuad(buf, pose, rx + e2x, ry + e2y, rz + e2z, e0x, e0y, e0z, e1x, e1y, e1z, wxAxis, wyAxis, wzAxis, skyBright, alpha, u0, u1);
            } else {
                emitShadedQuad(buf, pose, rx - e2x, ry - e2y, rz - e2z, e1x, e1y, e1z, e0x, e0y, e0z, -wxAxis, -wyAxis, -wzAxis, skyBright, alpha, u0, u1);
            }

            drawn++;
        }

        if (drawn > 0) {
            BufferUploader.drawWithShader(buf.end());
        } else {
            buf.end().release();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void emitShadedQuad(
            BufferBuilder buf,
            Matrix4f pose,
            float fcX, float fcY, float fcZ,
            float aX, float aY, float aZ,
            float bX, float bY, float bZ,
            float nx, float ny, float nz,
            float skyBright,
            int alpha,
            float uv0,
            float uv1
    ) {
        float nyWeight = ny >= 0.0F ? (0.75F + 0.25F * ny * ny) : (0.75F - 0.25F * ny * ny);
        float dirShade = Mth.clamp(nyWeight * (1.0F - 0.16F * nx * nx - 0.06F * nz * nz), 0.48F, 1.0F);
        float lit = skyBright * dirShade;
        float shadowTint = 1.0F - dirShade;

        int r = Mth.clamp((int) ((lit - shadowTint * 0.09F) * 255.0F), 90, 255);
        int g = Mth.clamp((int) ((lit - shadowTint * 0.03F) * 255.0F), 110, 255);
        int b = Mth.clamp((int) ((lit + shadowTint * 0.05F) * 255.0F), 135, 255);

        buf.vertex(pose, fcX - aX - bX, fcY - aY - bY, fcZ - aZ - bZ).uv(uv0, uv1).color(r, g, b, alpha).endVertex();
        buf.vertex(pose, fcX + aX - bX, fcY + aY - bY, fcZ + aZ - bZ).uv(uv1, uv1).color(r, g, b, alpha).endVertex();
        buf.vertex(pose, fcX + aX + bX, fcY + aY + bY, fcZ + aZ + bZ).uv(uv1, uv0).color(r, g, b, alpha).endVertex();
        buf.vertex(pose, fcX - aX + bX, fcY - aY + bY, fcZ - aZ + bZ).uv(uv0, uv0).color(r, g, b, alpha).endVertex();
    }
}
