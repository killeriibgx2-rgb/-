package com.zzynes.flydrone.Radio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class RadioClient {

    public static final net.minecraftforge.client.extensions.common.IClientItemExtensions RADIO_EXTENSIONS =
            new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return RadioItemRenderer.INSTANCE;
                }
            };

    public static void init(IEventBus modBus) {
        modBus.addListener(RadioClient::onRegisterKeys);
        modBus.addListener(RadioClient::onClientSetup);
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
    }

    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(RadioKeyHandler.KEY);
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (EntityRenderer<? extends Player> r :
                    Minecraft.getInstance().getEntityRenderDispatcher().getSkinMap().values()) {
                if (r instanceof PlayerRenderer pr) {
                    pr.addLayer(new RadioChestLayer(pr));
                }
            }
        });
    }

    public static void updateRadioState(UUID playerId, boolean hasRadio) {
        if (hasRadio) RadioChestLayer.PLAYERS_WITH_RADIO.add(playerId);
        else RadioChestLayer.PLAYERS_WITH_RADIO.remove(playerId);
    }

    public static class RadioKeyHandler {
        public static final KeyMapping KEY =
                new KeyMapping("key.fly_drone.radio_menu", GLFW.GLFW_KEY_Z, "key.categories.fly_drone");
    }

    public static class RadioScreen extends Screen {
        private static final ResourceLocation GUI =
                new ResourceLocation("fly_drone", "textures/gui/radio.png");

        private final int startFreq;
        private final StringBuilder input = new StringBuilder();

        public RadioScreen(ItemStack stack) {
            super(Component.literal("РАЦИЯ"));
            this.startFreq = RadioMod.RadioItem.getFrequency(stack);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            if (chr >= '0' && chr <= '9' && input.length() < 3) input.append(chr);
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 259 && !input.isEmpty()) {
                input.deleteCharAt(input.length() - 1);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void onClose() {
            if (!input.isEmpty()) {
                int freq = Integer.parseInt(input.toString());
                if (freq >= 100 && freq <= 999) {
                    RadioMod.CHANNEL.sendToServer(new RadioMod.SetFreqPacket(freq));
                } else {
                    RadioMod.CHANNEL.sendToServer(new RadioMod.SetFreqPacket(startFreq));
                }
            }
            super.onClose();
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            renderBackground(g);
            g.blit(GUI, 0, 0, this.width, this.height, 0, 0, 1920, 1080, 1920, 1080);

            float sx = this.width / 1920f;
            float sy = this.height / 1080f;
            int rx = (int) (807 * sx);
            int ry = (int) (264 * sy);
            int rw = (int) (404 * sx);
            int rh = (int) (180 * sy);

            String text = !input.isEmpty() ? input.toString() : String.valueOf(startFreq);

            PoseStack pose = g.pose();
            pose.pushPose();
            float scale = (rh * 0.55f) / 8f;
            int textWidth = this.font.width(text);
            if (textWidth > 0) scale = Math.min(scale, (rw * 0.9f) / textWidth);
            pose.translate(rx + rw / 2f, ry + rh / 2f, 0);
            pose.scale(scale, scale, 1);
            g.drawCenteredString(this.font, text, 0, -4, 0x55FF55);
            pose.popPose();

            super.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    public static class RadioItemModel extends GeoModel<RadioMod.RadioItem> {
        @Override
        public ResourceLocation getModelResource(RadioMod.RadioItem object) {
            return new ResourceLocation("fly_drone", "geo/radio.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(RadioMod.RadioItem object) {
            return new ResourceLocation("fly_drone", "textures/item/radio_texture.png");
        }

        @Override
        public ResourceLocation getAnimationResource(RadioMod.RadioItem animatable) {
            return new ResourceLocation("fly_drone", "animations/radio.animation.json");
        }
    }

    public static class RadioItemRenderer extends GeoItemRenderer<RadioMod.RadioItem> {
        public static final RadioItemRenderer INSTANCE = new RadioItemRenderer();

        public RadioItemRenderer() {
            super(new RadioItemModel());
        }
    }

    public static class RadioChestLayer
            extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

        public static final Set<UUID> PLAYERS_WITH_RADIO =
                Collections.synchronizedSet(new HashSet<>());

        public static float OFF_X = 0.0f;
        public static float OFF_Y = 0.25f; // Чуть выше центра груди
        public static float OFF_Z = 0.15f; // Вперед от тела
        public static float ROT_Y = 0.0f;
        public static float SCALE = 0.5f; // Уменьшаем модель на теле

        public RadioChestLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                           AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (player.isInvisible()) return;

            ItemStack radio;
            if (player == Minecraft.getInstance().player) {
                radio = RadioMod.findRadioOnBody(player);
            } else {
                if (!PLAYERS_WITH_RADIO.contains(player.getUUID())) return;
                radio = new ItemStack(RadioMod.RADIO.get());
            }
            if (radio == null) return;

            poseStack.pushPose();
            getParentModel().body.translateAndRotate(poseStack);
            poseStack.translate(OFF_X, OFF_Y, OFF_Z);
            poseStack.mulPose(Axis.YP.rotationDegrees(ROT_Y));
            poseStack.scale(SCALE, SCALE, SCALE);

            RadioItemRenderer.INSTANCE.renderByItem(radio, ItemDisplayContext.NONE, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);

            poseStack.popPose();
        }
    }

    public static class ClientEvents {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;
            if (RadioKeyHandler.KEY.consumeClick()) {
                ItemStack stack = mc.player.getMainHandItem();
                if (stack.getItem() == RadioMod.RADIO.get()) {
                    mc.setScreen(new RadioScreen(stack));
                }
            }
        }
    }
}