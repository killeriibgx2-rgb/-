package com.eternalsnow.client;

import com.eternalsnow.EternalSnowMod;
import com.eternalsnow.config.ClientSnowConfig;
import com.eternalsnow.state.EternalSnowState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import org.joml.Matrix4f;

/**
 * v2.6.6 — Footprints in fresh snow.
 *
 * Purely a client-side decal ring: a fixed circular buffer of N marks (no allocation, no
 * collections, no per-frame scans of the world). Walking entities push a mark into the ring, and
 * the renderer draws whatever is still inside the small radius and not yet faded out.
 *
 * Cheapness rules followed:
 *  - the tick handler only looks at entities within 16 blocks that are ON snow;
 *  - a mark is a single quad; at most MAX_MARKS quads can ever exist;
 *  - fade is computed from game-time, no timers/tasks, no particles.
 */
public final class SnowFootprintRenderer {

    private static final ResourceLocation FOOTPRINT_TEX =
            new ResourceLocation(EternalSnowMod.MODID, "textures/environment/footprint.png");

    private static final int MAX_MARKS = 320;
    private static final float MARK_LIFE_TICKS = 20.0F * 150.0F; // ~2.5 min of snowfall
    private static final float VISIBLE_RADIUS = 30.0F;

    private static final double[] X = new double[MAX_MARKS];
    private static final double[] Y = new double[MAX_MARKS];
    private static final double[] Z = new double[MAX_MARKS];
    private static final float[] YAW = new float[MAX_MARKS];
    private static final float[] BORN = new float[MAX_MARKS];
    private static final byte[] KIND = new byte[MAX_MARKS]; // 0 = player, 1 = small mob, 2 = big mob
    private static int head = 0;

    private SnowFootprintRenderer() {
    }

    public static void reset() {
        head = 0;
        for (int i = 0; i < MAX_MARKS; i++) {
            BORN[i] = -1.0e9F;
        }
    }

    /** v2.6.6: mark the ring "empty" on first use so stale zeros never render at world origin. */
    static {
        reset();
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!ClientSnowConfig.footprints) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        if (!EternalSnowState.getEffectiveSettings().enabled || !EternalSnowState.isOverworldLike(level)) {
            return;
        }

        Player self = mc.player;
        // Only entities close to the camera can ever be seen, so no world-wide entity scan.
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                self.getBoundingBox().inflate(VISIBLE_RADIUS * 0.6D))) {
            if (entity.isSpectator() || entity.isInvisible() || !entity.onGround()) {
                continue;
            }
            if (entity.tickCount % 4 != 0) {
                continue; // a mark every 4 ticks = a natural stride
            }
            // Feet are INSIDE the snow block for layers 1..7 (collision is lower
            // than the visible surface). Check that cell before the block below it.
            BlockPos below = entity.blockPosition();
            BlockState under = level.getBlockState(below);
            if (!under.is(Blocks.SNOW) && !under.is(Blocks.SNOW_BLOCK)) {
                below = below.below();
                under = level.getBlockState(below);
            }
            if (!under.is(Blocks.SNOW) && under.getBlock() != Blocks.SNOW_BLOCK) {
                continue;
            }
            var motion = entity.getDeltaMovement();
            float speed = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            if (speed < 0.045F) {
                continue;
            }
            push(entity.getX(), below.getY() + under.getShape(level, below).max(Direction.Axis.Y) + 0.002D, entity.getZ(),
                    (float) (Mth.atan2(motion.z, motion.x) * (180.0F / Math.PI)) - 90.0F,
                    level.getGameTime() + (float) (entity.tickCount % 4) / 4.0F,
                    entity instanceof AbstractClientPlayer ? (byte) 0 : (entity.getBbWidth() > 0.85F ? (byte) 2 : (byte) 1));
        }
    }

    private static void push(double x, double y, double z, float yaw, float born, byte kind) {
        // Do not stack marks on an identical spot (standing still, hoppers, etc.)
        int prev = (head - 1 + MAX_MARKS) % MAX_MARKS;
        if (BORN[prev] > born - 3.0F
                && Math.abs(X[prev] - x) < 0.22D && Math.abs(Z[prev] - z) < 0.22D) {
            return;
        }
        X[head] = x;
        Y[head] = y;
        Z[head] = z;
        YAW[head] = yaw;
        BORN[head] = born;
        KIND[head] = kind;
        head = (head + 1) % MAX_MARKS;
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (!ClientSnowConfig.footprints) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        float now = level.getGameTime() + event.getPartialTick();
        Camera camera = event.getCamera();
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;
        Matrix4f pose = event.getPoseStack().last().pose();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, FOOTPRINT_TEX);
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        int drawn = 0;
        float r2 = VISIBLE_RADIUS * VISIBLE_RADIUS;
        for (int i = 0; i < MAX_MARKS; i++) {
            float age = now - BORN[i];
            if (age < 0.0F || age > MARK_LIFE_TICKS) {
                continue;
            }
            double dx = X[i] - camX, dy = Y[i] - camY, dz = Z[i] - camZ;
            if (dx * dx + dy * dy + dz * dz > r2) {
                continue;
            }
            // snow fills the print in: fade plus a slight shrink over time
            float life = 1.0F - age / MARK_LIFE_TICKS;
            int alpha = Mth.clamp((int) (150.0F * life * life), 0, 150);
            if (alpha < 6) {
                continue;
            }
            float size = switch (KIND[i]) {
                case 0 -> 0.30F;
                case 1 -> 0.22F;
                default -> 0.46F;
            } * (0.72F + 0.28F * life);

            float yaw = (float) Math.toRadians(YAW[i]);
            float cos = Mth.cos(yaw), sin = Mth.sin(yaw);
            float ax = cos * size, az = sin * size;
            float bx = -sin * size, bz = cos * size;
            float px = (float) dx, py = (float) dy, pz = (float) dz;

            buf.vertex(pose, px - ax - bx, py, pz - az - bz).uv(0.0F, 1.0F).color(255, 255, 255, alpha).endVertex();
            buf.vertex(pose, px + ax - bx, py, pz + az - bz).uv(1.0F, 1.0F).color(255, 255, 255, alpha).endVertex();
            buf.vertex(pose, px + ax + bx, py, pz + az + bz).uv(1.0F, 0.0F).color(255, 255, 255, alpha).endVertex();
            buf.vertex(pose, px - ax + bx, py, pz - az + bz).uv(0.0F, 0.0F).color(255, 255, 255, alpha).endVertex();
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
}
