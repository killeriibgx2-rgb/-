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
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * v2.6.6 — Windows rime up from the edges.
 *
 * Performance contract (the client must never be overloaded):
 *  - only a small cube around the camera is walked (radius 10, and only cells whose centre is in
 *    front of the camera-ish range), once per FRAME with a hard cap on quads per frame;
 *  - no allocation inside the loop, no chunk load requests, no lighting queries, no per-glass
 *    state cache — just a block-state identity check;
 *  - one blended, depth-tested quad per exposed glass face, drawn with the frost texture whose
 *    alpha already fades to zero in the middle of the pane, so no shader is needed;
 *  - renders nothing at all when the pane is not cold (no snow event, warm air, or the block is
 *    warm-adjacent).
 */
public final class FrostedWindowRenderer {

    private static final ResourceLocation FROST_TEX =
            new ResourceLocation(EternalSnowMod.MODID, "textures/environment/window_frost.png");

    private static final int RADIUS = 10;
    private static final int MAX_QUADS = 220;
    private static final Direction[] FACES = Direction.values();

    private FrostedWindowRenderer() {
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (!ClientSnowConfig.frostedWindows) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        if (!EternalSnowState.getEffectiveSettings().enabled || !EternalSnowState.isOverworldLike(level)) {
            return;
        }
        // Frost only forms while it is actually cold outside.
        float outsideC = EternalSnowState.clientLocalTempC;
        if (outsideC > 2.0F && EternalSnowState.snowIntensity < 0.05F) {
            return;
        }

        Camera camera = event.getCamera();
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;
        BlockPos camPos = camera.getBlockPosition();

        int drawn = 0;
        Matrix4f pose = event.getPoseStack().last().pose();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, FROST_TEX);
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS && drawn < MAX_QUADS; dx++) {
            for (int dy = -RADIUS + 2; dy <= RADIUS - 2 && drawn < MAX_QUADS; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS && drawn < MAX_QUADS; dz++) {
                    pos.set(camPos.getX() + dx, camPos.getY() + dy, camPos.getZ() + dz);
                    // chunk lookup only for loaded chunks (getBlockState never loads)
                    BlockState state = level.getBlockState(pos);
                    if (!isGlass(state)) {
                        continue;
                    }
                    float cx = pos.getX() + 0.5F - (float) camX;
                    float cy = pos.getY() + 0.5F - (float) camY;
                    float cz = pos.getZ() + 0.5F - (float) camZ;
                    if (cx * cx + cy * cy + cz * cz > RADIUS * RADIUS) {
                        continue;
                    }
                    drawn += emitFaces(buf, pose, level, pos, cx, cy, cz);
                }
            }
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

    private static boolean isGlass(BlockState state) {
        return state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)
                || state.is(Blocks.TINTED_GLASS) || state.is(Blocks.WHITE_STAINED_GLASS)
                || state.is(Blocks.BLACK_STAINED_GLASS) || state.is(Blocks.BLUE_STAINED_GLASS)
                || state.is(Blocks.BROWN_STAINED_GLASS) || state.is(Blocks.CYAN_STAINED_GLASS)
                || state.is(Blocks.GRAY_STAINED_GLASS) || state.is(Blocks.GREEN_STAINED_GLASS)
                || state.is(Blocks.LIGHT_BLUE_STAINED_GLASS) || state.is(Blocks.LIGHT_GRAY_STAINED_GLASS)
                || state.is(Blocks.LIME_STAINED_GLASS) || state.is(Blocks.MAGENTA_STAINED_GLASS)
                || state.is(Blocks.ORANGE_STAINED_GLASS) || state.is(Blocks.PINK_STAINED_GLASS)
                || state.is(Blocks.PURPLE_STAINED_GLASS) || state.is(Blocks.RED_STAINED_GLASS)
                || state.is(Blocks.YELLOW_STAINED_GLASS);
    }

    /** Draws the frost quad on every face that touches air (inside faces are the visible ones). */
    private static int emitFaces(BufferBuilder buf, Matrix4f pose, ClientLevel level,
                                 BlockPos.MutableBlockPos pos, float cx, float cy, float cz) {
        int quads = 0;
        BlockPos.MutableBlockPos n = new BlockPos.MutableBlockPos();
        for (Direction dir : FACES) {
            n.set(pos.getX() + dir.getStepX(), pos.getY() + dir.getStepY(), pos.getZ() + dir.getStepZ());
            if (!level.getBlockState(n).isAir()) {
                continue;
            }
            float ox = dir.getStepX() * 0.5F;
            float oy = dir.getStepY() * 0.5F;
            float oz = dir.getStepZ() * 0.5F;
            float x0 = cx + ox, y0 = cy + oy, z0 = cz + oz;
            // push 0.002 towards the air side to avoid z-fighting with the glass itself
            x0 += dir.getStepX() * 0.002F;
            y0 += dir.getStepY() * 0.002F;
            z0 += dir.getStepZ() * 0.002F;

            float ax, ay, az, bx, by, bz;
            if (dir.getAxis() == Direction.Axis.Y) {
                ax = 0.5F; ay = 0; az = 0;
                bx = 0; by = 0; bz = 0.5F;
            } else if (dir.getAxis() == Direction.Axis.X) {
                ax = 0; ay = 0.5F; az = 0;
                bx = 0; by = 0; bz = 0.5F;
            } else {
                ax = 0.5F; ay = 0; az = 0;
                bx = 0; by = 0.5F; bz = 0;
            }
            // Frost fades the further the camera is; near panes are the ones that matter.
            float dist = Mth.sqrt(cx * cx + cy * cy + cz * cz);
            int alpha = Mth.clamp((int) (200.0F * Mth.clamp(1.35F - dist / (RADIUS * 1.15F), 0.15F, 1.0F)), 0, 210);
            if (alpha < 8) {
                continue;
            }
            int r = 232, g = 240, b = 255;
            buf.vertex(pose, x0 - ax - bx, y0 - ay - by, z0 - az - bz).uv(0.0F, 0.0F).color(r, g, b, alpha).endVertex();
            buf.vertex(pose, x0 + ax - bx, y0 + ay - by, z0 + az - bz).uv(1.0F, 0.0F).color(r, g, b, alpha).endVertex();
            buf.vertex(pose, x0 + ax + bx, y0 + ay + by, z0 + az + bz).uv(1.0F, 1.0F).color(r, g, b, alpha).endVertex();
            buf.vertex(pose, x0 - ax + bx, y0 - ay + by, z0 - az + bz).uv(0.0F, 1.0F).color(r, g, b, alpha).endVertex();
            quads++;
        }
        return quads;
    }
}
