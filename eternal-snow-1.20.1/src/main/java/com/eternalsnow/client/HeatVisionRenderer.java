package com.eternalsnow.client;

import com.eternalsnow.item.HeatScannerItem;
import com.eternalsnow.network.ClientboundHeatGridPacket;
import com.eternalsnow.state.HeatScanClientData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class HeatVisionRenderer {
    private static final Matrix4f VIEW_PROJ = new Matrix4f();
    private static final Vector4f CLIP = new Vector4f();

    private HeatVisionRenderer() {
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        ItemStack heldScanner = HeatScannerItem.getHeldScanner(player);
        if (heldScanner.isEmpty()) {
            return;
        }

        Vec3 cam = event.getCamera().getPosition();
        double cx = cam.x;
        double cy = cam.y;
        double cz = cam.z;
        Matrix4f pose = event.getPoseStack().last().pose();

        // v2.6.0: project the two world-anchored markers BEFORE any early return.
        projectMarkers(mc, pose, cx, cy, cz);

        int mode = HeatScannerItem.getMode(heldScanner);
        if (mode == HeatScannerItem.MODE_THERMAL) {
            return; // full-screen thermal pass is drawn by the HUD overlay (cheap, no wireframes)
        }

        short[] offsets = HeatScanClientData.packedOffsets;
        short[] temps = HeatScanClientData.deciTemps;
        BlockPos origin = HeatScanClientData.origin;
        if (offsets.length == 0 || offsets.length != temps.length) {
            return;
        }
        // Ignore stale grid if player teleported far away
        if (origin.distSqr(player.blockPosition()) > 64.0D * 64.0D) {
            return;
        }

        boolean warmOnly = mode == HeatScannerItem.MODE_WARM_ONLY;
        boolean relative = mode == HeatScannerItem.MODE_WALLS;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(2.0F);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        int radius = ClientboundHeatGridPacket.RADIUS;
        int drawnBoxes = 0;

        for (int i = 0; i < offsets.length; i++) {
            int packed = offsets[i] & 0xFFFF;
            boolean warmed = (packed & 0x8000) != 0;
            if (warmOnly && !warmed) {
                continue;
            }

            int dx = (packed & 0x1F) - radius;
            int dy = ((packed >> 5) & 0x1F) - radius;
            int dz = ((packed >> 10) & 0x1F) - radius;

            int bx = origin.getX() + dx;
            int by = origin.getY() + dy;
            int bz = origin.getZ() + dz;

            float tempC = temps[i] * 0.1F;
            int colorRgb = relative
                    ? relativeToRgb((tempC - HeatScanClientData.minC)
                            / Math.max(0.001F, HeatScanClientData.maxC - HeatScanClientData.minC))
                    : temperatureToRgb(tempC);
            int r = (colorRgb >> 16) & 0xFF;
            int g = (colorRgb >> 8) & 0xFF;
            int b = colorRgb & 0xFF;

            float distRatio = Mth.sqrt(dx * dx + dy * dy + dz * dz) / (float) radius;
            float fade = Mth.clamp(1.12F - distRatio * 0.78F, 0.18F, 1.0F);

            // Warmed blocks glow vividly; ambient cold air blocks get a crisp translucent outline so ALL air in 10m is visible without blinding!
            int alpha = warmed
                    ? Mth.clamp((int) (215.0F * fade), 85, 225)
                    : Mth.clamp((int) (88.0F * fade), 32, 95);

            // Slight inset (0.03..0.97) so adjacent air cubes never Z-fight and clearly show individual block temperature colors!
            float x0 = (float) (bx + 0.035D - cx);
            float y0 = (float) (by + 0.035D - cy);
            float z0 = (float) (bz + 0.035D - cz);
            float x1 = (float) (bx + 0.965D - cx);
            float y1 = (float) (by + 0.965D - cy);
            float z1 = (float) (bz + 0.965D - cz);

            emitWireBox(buf, pose, x0, y0, z0, x1, y1, z1, r, g, b, alpha);
            drawnBoxes++;
        }

        if (drawnBoxes > 0) {
            BufferUploader.drawWithShader(buf.end());
        } else {
            buf.end().release();
        }

        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void emitWireBox(
            BufferBuilder buf, Matrix4f p,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            int r, int g, int b, int a
    ) {
        // Bottom rectangle
        line(buf, p, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(buf, p, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(buf, p, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(buf, p, x0, y0, z1, x0, y0, z0, r, g, b, a);
        // Top rectangle
        line(buf, p, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(buf, p, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(buf, p, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(buf, p, x0, y1, z1, x0, y1, z0, r, g, b, a);
        // 4 vertical pillars
        line(buf, p, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(buf, p, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(buf, p, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(buf, p, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    private static void line(
            BufferBuilder buf, Matrix4f p,
            float ax, float ay, float az,
            float bx, float by, float bz,
            int r, int g, int b, int a
    ) {
        buf.vertex(p, ax, ay, az).color(r, g, b, a).endVertex();
        buf.vertex(p, bx, by, bz).color(r, g, b, a).endVertex();
    }

    /** v2.6.0: auto-scaled palette — coldest in view = blue, hottest in view = red. */
    public static int relativeToRgb(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        if (t < 0.25F) return lerpRgb(t / 0.25F, 30, 70, 235, 40, 190, 255);
        if (t < 0.50F) return lerpRgb((t - 0.25F) / 0.25F, 40, 190, 255, 70, 230, 150);
        if (t < 0.75F) return lerpRgb((t - 0.50F) / 0.25F, 70, 230, 150, 255, 225, 60);
        return lerpRgb((t - 0.75F) / 0.25F, 255, 225, 60, 255, 45, 40);
    }

    /**
     * Screen-space projection of the hottest/coldest world positions. The markers are stored as
     * BLOCK POSITIONS, so turning the camera keeps them anchored in the world (they simply leave
     * the screen); the server replaces them every 2 seconds with a fresh look-target sample.
     */
    private static void projectMarkers(Minecraft mc, Matrix4f pose, double cx, double cy, double cz) {
        long hot = HeatScanClientData.hotPos;
        long cold = HeatScanClientData.coldPos;
        if (!HeatScanClientData.markersFresh() || (hot == Long.MIN_VALUE && cold == Long.MIN_VALUE)) {
            HeatScanClientData.setMarkerScreen(-1.0F, -1.0F, false, -1.0F, -1.0F, false);
            return;
        }
        VIEW_PROJ.set(RenderSystem.getProjectionMatrix()).mul(pose);
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        float hx = -1.0F, hy = -1.0F, lx = -1.0F, ly = -1.0F;
        boolean hv = false, lv = false;
        if (hot != Long.MIN_VALUE) {
            CLIP.set((float) (BlockPos.getX(hot) + 0.5D - cx),
                    (float) (BlockPos.getY(hot) + 0.5D - cy),
                    (float) (BlockPos.getZ(hot) + 0.5D - cz), 1.0F);
            VIEW_PROJ.transform(CLIP);
            if (CLIP.w > 0.0F && Math.abs(CLIP.x) <= CLIP.w && Math.abs(CLIP.y) <= CLIP.w) {
                hx = (CLIP.x / CLIP.w * 0.5F + 0.5F) * w;
                hy = (1.0F - (CLIP.y / CLIP.w * 0.5F + 0.5F)) * h;
                hv = true;
            }
        }
        if (cold != Long.MIN_VALUE) {
            CLIP.set((float) (BlockPos.getX(cold) + 0.5D - cx),
                    (float) (BlockPos.getY(cold) + 0.5D - cy),
                    (float) (BlockPos.getZ(cold) + 0.5D - cz), 1.0F);
            VIEW_PROJ.transform(CLIP);
            if (CLIP.w > 0.0F && Math.abs(CLIP.x) <= CLIP.w && Math.abs(CLIP.y) <= CLIP.w) {
                lx = (CLIP.x / CLIP.w * 0.5F + 0.5F) * w;
                ly = (1.0F - (CLIP.y / CLIP.w * 0.5F + 0.5F)) * h;
                lv = true;
            }
        }
        HeatScanClientData.setMarkerScreen(hx, hy, hv, lx, ly, lv);
    }

    public static int temperatureToRgb(float tempC) {
        // Smooth 7-stop thermal rainbow:
        // -25C: Deep Blue (42, 95, 255)
        // -8C : Ice Cyan (45, 215, 255)
        // +2C : Turquoise (55, 242, 180)
        // +14C: Cozy Green (95, 245, 80)
        // +24C: Warm Yellow (255, 225, 50)
        // +38C: Hot Orange (255, 128, 25)
        // +58C: Blazing Red (255, 38, 38)
        if (tempC <= -25.0F) {
            return (42 << 16) | (95 << 8) | 255;
        } else if (tempC < -8.0F) {
            return lerpRgb((tempC - (-25.0F)) / 17.0F, 42, 95, 255, 45, 215, 255);
        } else if (tempC < 2.0F) {
            return lerpRgb((tempC - (-8.0F)) / 10.0F, 45, 215, 255, 55, 242, 180);
        } else if (tempC < 14.0F) {
            return lerpRgb((tempC - 2.0F) / 12.0F, 55, 242, 180, 95, 245, 80);
        } else if (tempC < 24.0F) {
            return lerpRgb((tempC - 14.0F) / 10.0F, 95, 245, 80, 255, 225, 50);
        } else if (tempC < 38.0F) {
            return lerpRgb((tempC - 24.0F) / 14.0F, 255, 225, 50, 255, 128, 25);
        } else if (tempC < 58.0F) {
            return lerpRgb((tempC - 38.0F) / 20.0F, 255, 128, 25, 255, 38, 38);
        } else {
            return (255 << 16) | (38 << 8) | 38;
        }
    }

    private static int lerpRgb(float t, int r1, int g1, int b1, int r2, int g2, int b2) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        int r = Math.round(Mth.lerp(t, r1, r2));
        int g = Math.round(Mth.lerp(t, g1, g2));
        int b = Math.round(Mth.lerp(t, b1, b2));
        return (r << 16) | (g << 8) | b;
    }
}
