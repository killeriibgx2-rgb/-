package com.eternalsnow.client;

import com.eternalsnow.EternalSnowMod;
import com.eternalsnow.block.FireplaceBlock;
import com.eternalsnow.block.FireplaceBlockEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Native zero-dependency Blockbench Bedrock .geo.json + .animation.json renderer for the 3D Fireplace!
 * Exact user rules:
 * - No fuel: hide 'fire' bone completely.
 * - Has fuel, unlit: show 'fire' (logs), hide 'fire_on' (flames).
 * - Has fuel & lit: show 'fire' + 'fire_on' (fullbright) and loop animation.fireplace.burn!
 */
public final class FireplaceRenderer implements BlockEntityRenderer<FireplaceBlockEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(EternalSnowMod.MODID, "textures/block/fireplace_128.png");
    private static final ResourceLocation GEO_LOC = new ResourceLocation(EternalSnowMod.MODID, "geo/fireplace.geo.json");
    private static final ResourceLocation ANIM_LOC = new ResourceLocation(EternalSnowMod.MODID, "animations/fireplace.animation.json");

    private static List<GeoBone> rootBones = null;
    private static Map<String, BoneAnim> burnAnims = null;
    private static float animLengthSec = 2.0F;

    public FireplaceRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    private static void ensureLoaded() {
        if (rootBones != null) {
            return;
        }
        rootBones = new ArrayList<>();
        burnAnims = new HashMap<>();

        try {
            var rm = Minecraft.getInstance().getResourceManager();
            try (var reader = new InputStreamReader(rm.getResourceOrThrow(GEO_LOC).open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject geo = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                JsonObject desc = geo.getAsJsonObject("description");
                float texW = desc.get("texture_width").getAsFloat();
                float texH = desc.get("texture_height").getAsFloat();

                Map<String, GeoBone> byName = new HashMap<>();
                Map<String, String> parentLinks = new HashMap<>();

                for (JsonElement el : geo.getAsJsonArray("bones")) {
                    JsonObject bObj = el.getAsJsonObject();
                    String name = bObj.get("name").getAsString();
                    float[] pivot = readVec3(bObj.get("pivot"), 0, 0, 0);
                    float[] rot = readVec3(bObj.get("rotation"), 0, 0, 0);

                    List<GeoCube> cubes = new ArrayList<>();
                    if (bObj.has("cubes")) {
                        for (JsonElement cEl : bObj.getAsJsonArray("cubes")) {
                            cubes.add(parseCube(cEl.getAsJsonObject(), texW, texH));
                        }
                    }

                    GeoBone bone = new GeoBone(name, pivot[0], pivot[1], pivot[2], rot[0], rot[1], rot[2], cubes);
                    byName.put(name, bone);
                    if (bObj.has("parent")) {
                        parentLinks.put(name, bObj.get("parent").getAsString());
                    }
                }

                for (GeoBone bone : byName.values()) {
                    String parentName = parentLinks.get(bone.name);
                    if (parentName != null && byName.containsKey(parentName)) {
                        byName.get(parentName).children.add(bone);
                    } else {
                        rootBones.add(bone);
                    }
                }
            }

            try (var reader = new InputStreamReader(rm.getResourceOrThrow(ANIM_LOC).open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject anims = root.getAsJsonObject("animations");
                if (anims != null && !anims.entrySet().isEmpty()) {
                    JsonObject burn = anims.entrySet().iterator().next().getValue().getAsJsonObject();
                    if (burn.has("animation_length")) {
                        animLengthSec = Math.max(0.1F, burn.get("animation_length").getAsFloat());
                    }
                    if (burn.has("bones")) {
                        for (var entry : burn.getAsJsonObject("bones").entrySet()) {
                            JsonObject ch = entry.getValue().getAsJsonObject();
                            burnAnims.put(entry.getKey(), new BoneAnim(
                                    parseTrack(ch.get("rotation"), 0, 0, 0),
                                    parseTrack(ch.get("position"), 0, 0, 0),
                                    parseTrack(ch.get("scale"), 1, 1, 1)
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            EternalSnowMod.LOGGER.error("[EternalSnow] Failed loading 3D Fireplace geo/animation JSON", e);
        }
    }

    @Override
    public void render(
            FireplaceBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        ensureLoaded();
        if (rootBones == null || rootBones.isEmpty()) {
            return;
        }

        Direction facing = be.getBlockState().hasProperty(FireplaceBlock.FACING)
                ? be.getBlockState().getValue(FireplaceBlock.FACING)
                : Direction.NORTH;

        boolean hasFuel = be.hasFuel();
        boolean isLit = hasFuel && be.isLit() && be.getDamperPercent() > 0;
        float draftRatio = Mth.clamp(be.getDamperPercent() / 100.0F, 0.0F, 1.0F);
        float flameHeightScale = Mth.lerp(draftRatio, 0.48F, 1.12F);
        float animSpeedMult = Mth.lerp(draftRatio, 0.42F, 1.38F);

        float animTime = 0.0F;
        if (isLit && be.getLevel() != null) {
            float phase = ((be.getBlockPos().asLong() & 31L) * 0.07F);
            animTime = ((((be.getLevel().getGameTime() + partialTick) / 20.0F) * animSpeedMult) + phase) % animLengthSec;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        float rotY = switch (facing) {
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        for (GeoBone root : rootBones) {
            renderBoneRecursive(root, vc, poseStack, packedLight, packedOverlay, hasFuel, isLit, animTime, flameHeightScale, false);
        }

        poseStack.popPose();
    }

    private static void renderBoneRecursive(
            GeoBone bone,
            VertexConsumer vc,
            PoseStack poseStack,
            int packedLight,
            int packedOverlay,
            boolean hasFuel,
            boolean isLit,
            float animTime,
            float flameHeightScale,
            boolean insideFireOnSubtree
    ) {
        // Exact user visibility rules:
        if ("fire".equals(bone.name) && !hasFuel) {
            return;
        }
        if ("fire_on".equals(bone.name) && !isLit) {
            return;
        }

        boolean glowing = insideFireOnSubtree || "fire_on".equals(bone.name);
        int lightToUse = glowing ? LightTexture.FULL_BRIGHT : packedLight;

        poseStack.pushPose();

        float rx = bone.rotX;
        float ry = bone.rotY;
        float rz = bone.rotZ;
        float px = 0.0F, py = 0.0F, pz = 0.0F;
        float sx = 1.0F, sy = 1.0F, sz = 1.0F;

        if (isLit && burnAnims != null) {
            BoneAnim anim = burnAnims.get(bone.name);
            if (anim != null) {
                if (anim.rot != null) {
                    rx += anim.rot.sample(animTime, 0);
                    ry += anim.rot.sample(animTime, 1);
                    rz += anim.rot.sample(animTime, 2);
                }
                if (anim.pos != null) {
                    px = anim.pos.sample(animTime, 0);
                    py = anim.pos.sample(animTime, 1);
                    pz = anim.pos.sample(animTime, 2);
                }
                if (anim.scale != null) {
                    sx = anim.scale.sample(animTime, 0);
                    sy = anim.scale.sample(animTime, 1);
                    sz = anim.scale.sample(animTime, 2);
                }
            }
        }

        if ("fire_on".equals(bone.name) && isLit) {
            sy *= flameHeightScale;
        }

        if (px != 0.0F || py != 0.0F || pz != 0.0F) {
            poseStack.translate(-px / 16.0F, py / 16.0F, pz / 16.0F);
        }

        boolean hasTransform = (rx != 0.0F || ry != 0.0F || rz != 0.0F || sx != 1.0F || sy != 1.0F || sz != 1.0F);
        if (hasTransform) {
            poseStack.translate(bone.pivX / 16.0F, bone.pivY / 16.0F, bone.pivZ / 16.0F);
            applyBedrockRotation(poseStack, rx, ry, rz);
            if (sx != 1.0F || sy != 1.0F || sz != 1.0F) {
                poseStack.scale(sx, sy, sz);
            }
            poseStack.translate(-bone.pivX / 16.0F, -bone.pivY / 16.0F, -bone.pivZ / 16.0F);
        }

        for (GeoCube cube : bone.cubes) {
            cube.render(vc, poseStack, lightToUse, packedOverlay);
        }

        for (GeoBone child : bone.children) {
            renderBoneRecursive(child, vc, poseStack, packedLight, packedOverlay, hasFuel, isLit, animTime, flameHeightScale, glowing);
        }

        poseStack.popPose();
    }

    private static void applyBedrockRotation(PoseStack poseStack, float rxDeg, float ryDeg, float rzDeg) {
        if (rzDeg != 0.0F) poseStack.mulPose(Axis.ZP.rotationDegrees(rzDeg));
        if (ryDeg != 0.0F) poseStack.mulPose(Axis.YP.rotationDegrees(-ryDeg));
        if (rxDeg != 0.0F) poseStack.mulPose(Axis.XP.rotationDegrees(-rxDeg));
    }

    @Override
    public boolean shouldRenderOffScreen(FireplaceBlockEntity be) {
        return true;
    }

    private static float[] readVec3(JsonElement el, float dx, float dy, float dz) {
        if (el == null || !el.isJsonArray()) {
            return new float[]{dx, dy, dz};
        }
        JsonArray arr = el.getAsJsonArray();
        return new float[]{
                arr.get(0).getAsFloat(),
                arr.get(1).getAsFloat(),
                arr.get(2).getAsFloat()
        };
    }

    private static KeyTrack parseTrack(JsonElement el, float defX, float defY, float defZ) {
        if (el == null || !el.isJsonObject()) {
            return null;
        }
        JsonObject obj = el.getAsJsonObject();
        int n = obj.entrySet().size();
        if (n == 0) {
            return null;
        }
        float[] times = new float[n];
        float[][] vals = new float[n][3];
        int i = 0;
        for (var e : obj.entrySet()) {
            times[i] = Float.parseFloat(e.getKey());
            vals[i] = readVec3(e.getValue(), defX, defY, defZ);
            i++;
        }
        return new KeyTrack(times, vals);
    }

    private static GeoCube parseCube(JsonObject cObj, float texW, float texH) {
        float[] o = readVec3(cObj.get("origin"), 0, 0, 0);
        float[] s = readVec3(cObj.get("size"), 0, 0, 0);
        float[] piv = readVec3(cObj.get("pivot"), 0, 0, 0);
        float[] rot = readVec3(cObj.get("rotation"), 0, 0, 0);
        boolean hasRot = cObj.has("rotation");

        JsonObject uv = cObj.getAsJsonObject("uv");
        float[] n = readFaceUv(uv.getAsJsonObject("north"), texW, texH);
        float[] e = readFaceUv(uv.getAsJsonObject("east"), texW, texH);
        float[] so = readFaceUv(uv.getAsJsonObject("south"), texW, texH);
        float[] w = readFaceUv(uv.getAsJsonObject("west"), texW, texH);
        float[] u = readFaceUv(uv.getAsJsonObject("up"), texW, texH);
        float[] d = readFaceUv(uv.getAsJsonObject("down"), texW, texH);

        return new GeoCube(
                o[0] / 16.0F, o[1] / 16.0F, o[2] / 16.0F,
                (o[0] + s[0]) / 16.0F, (o[1] + s[1]) / 16.0F, (o[2] + s[2]) / 16.0F,
                hasRot, piv[0] / 16.0F, piv[1] / 16.0F, piv[2] / 16.0F, rot[0], rot[1], rot[2],
                n[0], n[1], n[2], n[3],
                e[0], e[1], e[2], e[3],
                so[0], so[1], so[2], so[3],
                w[0], w[1], w[2], w[3],
                u[0], u[1], u[2], u[3],
                d[0], d[1], d[2], d[3]
        );
    }

    private static float[] readFaceUv(JsonObject fObj, float texW, float texH) {
        if (fObj == null) {
            return new float[]{0, 0, 0, 0};
        }
        JsonArray uv = fObj.getAsJsonArray("uv");
        JsonArray sz = fObj.getAsJsonArray("uv_size");
        float u0 = uv.get(0).getAsFloat() / texW;
        float v0 = uv.get(1).getAsFloat() / texH;
        float u1 = (uv.get(0).getAsFloat() + sz.get(0).getAsFloat()) / texW;
        float v1 = (uv.get(1).getAsFloat() + sz.get(1).getAsFloat()) / texH;
        return new float[]{u0, v0, u1, v1};
    }

    private static final class GeoBone {
        final String name;
        final float pivX, pivY, pivZ;
        final float rotX, rotY, rotZ;
        final List<GeoCube> cubes;
        final List<GeoBone> children = new ArrayList<>();

        GeoBone(String name, float pivX, float pivY, float pivZ, float rotX, float rotY, float rotZ, List<GeoCube> cubes) {
            this.name = name;
            this.pivX = pivX;
            this.pivY = pivY;
            this.pivZ = pivZ;
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
            this.cubes = cubes;
        }
    }

    private record BoneAnim(KeyTrack rot, KeyTrack pos, KeyTrack scale) {
    }

    private record KeyTrack(float[] times, float[][] vals) {
        float sample(float t, int axis) {
            int n = times.length;
            if (n == 1 || t <= times[0]) {
                return vals[0][axis];
            }
            if (t >= times[n - 1]) {
                return vals[n - 1][axis];
            }
            for (int i = 0; i < n - 1; i++) {
                if (t >= times[i] && t <= times[i + 1]) {
                    float span = times[i + 1] - times[i];
                    float f = span > 1.0e-5F ? (t - times[i]) / span : 0.0F;
                    return Mth.lerp(f, vals[i][axis], vals[i + 1][axis]);
                }
            }
            return vals[n - 1][axis];
        }
    }

    private record GeoCube(
            float x0, float y0, float z0, float x1, float y1, float z1,
            boolean hasRot, float pivX, float pivY, float pivZ, float rx, float ry, float rz,
            float nu0, float nv0, float nu1, float nv1,
            float eu0, float ev0, float eu1, float ev1,
            float su0, float sv0, float su1, float sv1,
            float wu0, float wv0, float wu1, float wv1,
            float uu0, float uv0, float uu1, float uv1,
            float du0, float dv0, float du1, float dv1
    ) {
        void render(VertexConsumer vc, PoseStack poseStack, int light, int overlay) {
            if (this.hasRot) {
                poseStack.pushPose();
                poseStack.translate(pivX, pivY, pivZ);
                applyBedrockRotation(poseStack, rx, ry, rz);
                poseStack.translate(-pivX, -pivY, -pivZ);
                emitFaces(vc, poseStack.last(), light, overlay);
                poseStack.popPose();
            } else {
                emitFaces(vc, poseStack.last(), light, overlay);
            }
        }

        private void emitFaces(VertexConsumer vc, PoseStack.Pose pose, int light, int overlay) {
            Matrix4f m = pose.pose();
            Matrix3f n = pose.normal();
            // North (-Z)
            quad(vc, m, n, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, nu0, nv0, nu1, nv1, 0, 0, -1, light, overlay);
            // South (+Z)
            quad(vc, m, n, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, su0, sv0, su1, sv1, 0, 0, 1, light, overlay);
            // East (+X)
            quad(vc, m, n, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1, eu0, ev0, eu1, ev1, 1, 0, 0, light, overlay);
            // West (-X)
            quad(vc, m, n, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0, wu0, wv0, wu1, wv1, -1, 0, 0, light, overlay);
            // Up (+Y)
            quad(vc, m, n, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, uu0, uv0, uu1, uv1, 0, 1, 0, light, overlay);
            // Down (-Y)
            quad(vc, m, n, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, du0, dv0, du1, dv1, 0, -1, 0, light, overlay);
        }

        private static void quad(
                VertexConsumer vc, Matrix4f m, Matrix3f n,
                float ax, float ay, float az,
                float bx, float by, float bz,
                float cx, float cy, float cz,
                float dx, float dy, float dz,
                float u0, float v0, float u1, float v1,
                float nx, float ny, float nz,
                int light, int overlay
        ) {
            vc.vertex(m, ax, ay, az).color(255, 255, 255, 255).uv(u0, v0).overlayCoords(overlay).uv2(light).normal(n, nx, ny, nz).endVertex();
            vc.vertex(m, bx, by, bz).color(255, 255, 255, 255).uv(u1, v0).overlayCoords(overlay).uv2(light).normal(n, nx, ny, nz).endVertex();
            vc.vertex(m, cx, cy, cz).color(255, 255, 255, 255).uv(u1, v1).overlayCoords(overlay).uv2(light).normal(n, nx, ny, nz).endVertex();
            vc.vertex(m, dx, dy, dz).color(255, 255, 255, 255).uv(u0, v1).overlayCoords(overlay).uv2(light).normal(n, nx, ny, nz).endVertex();
        }
    }
}
