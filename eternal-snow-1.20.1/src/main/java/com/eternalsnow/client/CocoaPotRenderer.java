package com.eternalsnow.client;

import com.eternalsnow.EternalSnowMod;
import com.eternalsnow.block.CocoaPotBlock;
import com.eternalsnow.block.CocoaPotBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Exact 3D renderer for the user's Blockbench/GeckoLib model "pot - Converted.geo.json" (64x64 pot.png).
 * Bone "bone" = pot body & bail handle; child bone "full" = inner drink liquid (visible ONLY when cooking or filled!).
 */
public final class CocoaPotRenderer implements BlockEntityRenderer<CocoaPotBlockEntity> {
    private static final ResourceLocation POT_TEXTURE = new ResourceLocation(EternalSnowMod.MODID, "textures/block/pot.png");

    private static final GeoCube[] POT_BONE_CUBES = new GeoCube[]{
            perFace(-6, 0, -6, 12, 1, 12,
                    0, 24, 12, 1, 0, 25, 12, 1, 0, 26, 12, 1, 0, 27, 12, 1, 12, 12, -12, -12, 12, 24, -12, -12),
            perFace(-5, 1, 6, 10, 7, 1,
                    12, 0, 10, 7, 11, 32, 1, 7, 12, 7, 10, 7, 12, 32, 1, 7, 10, 31, -10, -1, 20, 31, -10, -1),
            perFace(-0.5F, 8, 6.35F, 1, 7, 0.25F,
                    33, 32, 1, 7, 34, 5, 1, 7, 34, 12, 1, 7, 34, 19, 1, 7, 36, 11, -1, -1, 36, 12, -1, -1),
            perFace(-0.5F, 14, -6.35F, 1, 1, 12.70023F,
                    35, 12, 1, 1, 0, 28, 13, 1, 35, 13, 1, 1, 13, 28, 13, 1, 27, 41, -1, -13, 28, 41, -1, -13),
            perFace(-5, 0, 6, 10, 1, 1,
                    0, 32, 10, 1, 34, 34, 1, 1, 32, 0, 10, 1, 35, 5, 1, 1, 42, 2, -10, -1, 42, 3, -10, -1),
            perFace(-6, 1, 5, 1, 7, 1,
                    18, 32, 1, 7, 19, 32, 1, 7, 28, 32, 1, 7, 29, 32, 1, 7, 33, 25, -1, -1, 33, 26, -1, -1),
            perFace(-6, 1, -6, 1, 7, 1,
                    30, 32, 1, 7, 31, 32, 1, 7, 32, 32, 1, 7, 0, 33, 1, 7, 33, 27, -1, -1, 33, 28, -1, -1),
            perFace(5, 1, -6, 1, 7, 1,
                    1, 33, 1, 7, 2, 33, 1, 7, 3, 33, 1, 7, 4, 33, 1, 7, 34, 27, -1, -1, 34, 28, -1, -1),
            perFace(5, 1, 5, 1, 7, 1,
                    5, 33, 1, 7, 33, 5, 1, 7, 6, 33, 1, 7, 7, 33, 1, 7, 35, 27, -1, -1, 35, 28, -1, -1),
            perFace(-5, 1, -7, 10, 7, 1,
                    12, 14, 10, 7, 13, 32, 1, 7, 12, 21, 10, 7, 14, 32, 1, 7, 38, 31, -10, -1, 10, 32, -10, -1),
            perFace(-5, 0, -7, 10, 1, 1,
                    28, 28, 10, 1, 32, 22, 1, 1, 0, 29, 10, 1, 32, 23, 1, 1, 20, 30, -10, -1, 38, 30, -10, -1),
            perFace(-0.5F, 8, -6.6F, 1, 7, 0.25F,
                    8, 33, 1, 7, 9, 33, 1, 7, 33, 12, 1, 7, 33, 19, 1, 7, 36, 9, -1, -1, 36, 10, -1, -1),
            perFace(6, 1, -5, 1, 7, 10,
                    15, 32, 1, 7, 22, 0, 10, 7, 32, 15, 1, 7, 22, 7, 10, 7, 21, 39, -1, -10, 22, 39, -1, -10),
            perFace(6, 0, -5, 1, 1, 10,
                    35, 6, 1, 1, 32, 3, 10, 1, 35, 7, 1, 1, 32, 4, 10, 1, 33, 15, -1, -10, 11, 42, -1, -10),
            perFace(-7, 1, -5, 1, 7, 10,
                    16, 32, 1, 7, 22, 14, 10, 7, 17, 32, 1, 7, 22, 21, 10, 7, 23, 39, -1, -10, 24, 39, -1, -10),
            perFace(-7, 0, -5, 1, 1, 10,
                    34, 32, 1, 1, 10, 31, 10, 1, 34, 33, 1, 1, 28, 31, 10, 1, 25, 39, -1, -10, 26, 39, -1, -10)
    };

    private static final GeoCube[] FULL_BONE_CUBES = new GeoCube[]{
            boxUv(-5, 5, 5, 10, 1, 1, 45, 40),
            perFace(-5, 5, -5, 10, 1, 10,
                    45, 24, 12, 1, 45, 25, 12, 1, 45, 26, 12, 1, 45, 27, 12, 1, 57, 12, -12, -12, 57, 24, -12, -12),
            boxUv(-6, 5, -5, 1, 1, 10, 0, 48),
            boxUv(-5, 5, -6, 10, 1, 1, 9, 48),
            boxUv(5, 5, -5, 1, 1, 10, 22, 48)
    };

    public CocoaPotRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(
            CocoaPotBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Direction facing = be.getBlockState().hasProperty(CocoaPotBlock.FACING)
                ? be.getBlockState().getValue(CocoaPotBlock.FACING)
                : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        float rotY = switch (facing) {
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(POT_TEXTURE));

        // 1. Always render main pot bone ("bone")
        PoseStack.Pose mainPose = poseStack.last();
        for (GeoCube cube : POT_BONE_CUBES) {
            cube.render(vc, mainPose, packedLight, packedOverlay, 255, 255, 255);
        }

        // 2. Render child bone ("full") ONLY when something is cooking or cooked inside the pot!
        if (be.shouldShowFullBone()) {
            poseStack.pushPose();
            // Subtle liquid level shift for 1, 2, or 3 servings while keeping "full" clearly visible!
            int servings = be.getServings();
            if (servings == 2) {
                poseStack.translate(0.0D, -0.055D, 0.0D);
            } else if (servings == 1) {
                poseStack.translate(0.0D, -0.11D, 0.0D);
            }

            boolean isCaramel = be.getDrinkType() == CocoaPotBlockEntity.DRINK_CARAMEL;
            // pot.png "full" surface is rich dark cocoa brown; brighten & shift toward golden caramel for Caramel Drink!
            int r = 255;
            int g = isCaramel ? 228 : 255;
            int b = isCaramel ? 155 : 255;

            PoseStack.Pose fullPose = poseStack.last();
            for (GeoCube cube : FULL_BONE_CUBES) {
                cube.render(vc, fullPose, packedLight, packedOverlay, r, g, b);
            }
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static GeoCube perFace(
            float ox, float oy, float oz, float sx, float sy, float sz,
            float nu, float nv, float ndu, float ndv,
            float eu, float ev, float edu, float edv,
            float su, float sv, float sdu, float sdv,
            float wu, float wv, float wdu, float wdv,
            float uu, float uv, float udu, float udv,
            float du, float dv, float ddu, float ddv
    ) {
        return new GeoCube(
                ox / 16.0F, oy / 16.0F, oz / 16.0F,
                (ox + sx) / 16.0F, (oy + sy) / 16.0F, (oz + sz) / 16.0F,
                nu / 64.0F, nv / 64.0F, (nu + ndu) / 64.0F, (nv + ndv) / 64.0F,
                eu / 64.0F, ev / 64.0F, (eu + edu) / 64.0F, (ev + edv) / 64.0F,
                su / 64.0F, sv / 64.0F, (su + sdu) / 64.0F, (sv + sdv) / 64.0F,
                wu / 64.0F, wv / 64.0F, (wu + wdu) / 64.0F, (wv + wdv) / 64.0F,
                uu / 64.0F, uv / 64.0F, (uu + udu) / 64.0F, (uv + udv) / 64.0F,
                du / 64.0F, dv / 64.0F, (du + ddu) / 64.0F, (dv + ddv) / 64.0F
        );
    }

    private static GeoCube boxUv(float ox, float oy, float oz, int sx, int sy, int sz, int u, int v) {
        return perFace(
                ox, oy, oz, sx, sy, sz,
                u + sz, v + sz, sx, sy,
                u, v + sz, sz, sy,
                u + sz + sx + sz, v + sz, sx, sy,
                u + sz + sx, v + sz, sz, sy,
                u + sz + sx, v + sz, -sx, -sz,
                u + sz + sx + sx, v, -sx, sz
        );
    }

    private record GeoCube(
            float x0, float y0, float z0, float x1, float y1, float z1,
            float nu0, float nv0, float nu1, float nv1,
            float eu0, float ev0, float eu1, float ev1,
            float su0, float sv0, float su1, float sv1,
            float wu0, float wv0, float wu1, float wv1,
            float uu0, float uv0, float uu1, float uv1,
            float du0, float dv0, float du1, float dv1
    ) {
        void render(VertexConsumer vc, PoseStack.Pose pose, int light, int overlay, int r, int g, int b) {
            Matrix4f m = pose.pose();
            Matrix3f n = pose.normal();

            // North (-Z)
            quad(vc, m, n, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, nu0, nv0, nu1, nv1, 0, 0, -1, light, overlay, r, g, b);
            // South (+Z)
            quad(vc, m, n, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, su0, sv0, su1, sv1, 0, 0, 1, light, overlay, r, g, b);
            // East (+X)
            quad(vc, m, n, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1, eu0, ev0, eu1, ev1, 1, 0, 0, light, overlay, r, g, b);
            // West (-X)
            quad(vc, m, n, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0, wu0, wv0, wu1, wv1, -1, 0, 0, light, overlay, r, g, b);
            // Up (+Y)
            quad(vc, m, n, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, uu0, uv0, uu1, uv1, 0, 1, 0, light, overlay, r, g, b);
            // Down (-Y)
            quad(vc, m, n, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, du0, dv0, du1, dv1, 0, -1, 0, light, overlay, r, g, b);
        }

        private static void quad(
                VertexConsumer vc, Matrix4f m, Matrix3f n,
                float ax, float ay, float az,
                float bx, float by, float bz,
                float cx, float cy, float cz,
                float dx, float dy, float dz,
                float u0, float v0, float u1, float v1,
                float nx, float ny, float nz,
                int light, int overlay, int r, int g, int b
        ) {
            vc.vertex(m, ax, ay, az).color(r, g, b, 255).uv(u0, v0).overlayCoords(overlay).uv2(light).normal(n, nx, ny, nz).endVertex();
            vc.vertex(m, bx, by, bz).color(r, g, b, 255).uv(u1, v0).overlayCoords(overlay).uv2(light).normal(n, nx, ny, nz).endVertex();
            vc.vertex(m, cx, cy, cz).color(r, g, b, 255).uv(u1, v1).overlayCoords(overlay).uv2(light).normal(n, nx, ny, nz).endVertex();
            vc.vertex(m, dx, dy, dz).color(r, g, b, 255).uv(u0, v1).overlayCoords(overlay).uv2(light).normal(n, nx, ny, nz).endVertex();
        }
    }
}
