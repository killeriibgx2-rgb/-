package com.zzynes.flydrone.Radio;

import com.zzynes.flydrone.DRONE.FlyDroneMod;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RadioMod {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FlyDroneMod.MOD_ID);

    public static final RegistryObject<Item> RADIO = ITEMS.register("radio",
            () -> new RadioItem(new Item.Properties().stacksTo(1)));

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FlyDroneMod.MOD_ID, "radio"), () -> "1", "1"::equals, "1"::equals);

    public static void init(IEventBus modBus) {
        ITEMS.register(modBus);

        CHANNEL.messageBuilder(SetFreqPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetFreqPacket::encode).decoder(SetFreqPacket::decode)
                .consumerMainThread(SetFreqPacket::handle).add();

        CHANNEL.messageBuilder(BodySyncPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BodySyncPacket::encode).decoder(BodySyncPacket::decode)
                .consumerMainThread(BodySyncPacket::handle).add();

        CHANNEL.messageBuilder(VoicePacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(VoicePacket::encode).decoder(VoicePacket::decode)
                .consumerMainThread(VoicePacket::handle).add();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            RadioClient.init(modBus);
        });

        MinecraftForge.EVENT_BUS.register(new RadioEvents());
    }

    public static ItemStack findRadioOnBody(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == RADIO.get() && stack != main && stack != off) return stack;
        }
        return null;
    }

    public static void startVoiceRecording(Player player, int frequency) {
    }

    public static void stopVoiceRecordingAndSend(Player player, int frequency) {
    }

    public static class RadioItem extends Item implements GeoItem {
        private static final Random RANDOM = new Random();
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        public RadioItem(Properties properties) {
            super(properties);
        }

        public static int getFrequency(ItemStack stack) {
            CompoundTag tag = stack.getOrCreateTag();
            if (!tag.contains("Frequency")) {
                tag.putInt("Frequency", 100 + RANDOM.nextInt(900));
            }
            return tag.getInt("Frequency");
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal(ChatFormatting.GREEN + "Частота: " + getFrequency(stack)));
        }

        @Override
        public int getUseDuration(ItemStack stack) {
            return 72000;
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            return UseAnim.NONE;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide()) {
                startVoiceRecording(player, getFrequency(stack));
            }
            return InteractionResultHolder.consume(stack);
        }

        @Override
        public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
            if (level.isClientSide() && entity instanceof Player player) {
                stopVoiceRecordingAndSend(player, getFrequency(stack));
            }
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        @Override
        public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                consumer.accept(RadioClient.RADIO_EXTENSIONS);
            });
        }
    }

    public static class SetFreqPacket {
        private final int freq;

        public SetFreqPacket(int freq) {
            this.freq = freq;
        }

        public static void encode(SetFreqPacket p, FriendlyByteBuf buf) {
            buf.writeVarInt(p.freq);
        }

        public static SetFreqPacket decode(FriendlyByteBuf buf) {
            return new SetFreqPacket(buf.readVarInt());
        }

        public static void handle(SetFreqPacket p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;
                if (p.freq < 100 || p.freq > 999) return;
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() == RADIO.get()) {
                    stack.getOrCreateTag().putInt("Frequency", p.freq);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class BodySyncPacket {
        private final UUID playerId;
        private final boolean hasRadio;

        public BodySyncPacket(UUID playerId, boolean hasRadio) {
            this.playerId = playerId;
            this.hasRadio = hasRadio;
        }

        public static void encode(BodySyncPacket p, FriendlyByteBuf buf) {
            buf.writeUUID(p.playerId);
            buf.writeBoolean(p.hasRadio);
        }

        public static BodySyncPacket decode(FriendlyByteBuf buf) {
            return new BodySyncPacket(buf.readUUID(), buf.readBoolean());
        }

        public static void handle(BodySyncPacket p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                if (ctx.get().getDirection().getReceptionSide().isClient()) {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RadioClient.updateRadioState(p.playerId, p.hasRadio));
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class VoicePacket {
        private final int frequency;
        private final byte[] audioData;

        public VoicePacket(int frequency, byte[] audioData) {
            this.frequency = frequency;
            this.audioData = audioData;
        }

        public static void encode(VoicePacket p, FriendlyByteBuf buf) {
            buf.writeVarInt(p.frequency);
            buf.writeByteArray(p.audioData);
        }

        public static VoicePacket decode(FriendlyByteBuf buf) {
            return new VoicePacket(buf.readVarInt(), buf.readByteArray());
        }

        public static void handle(VoicePacket p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;
                MinecraftServer server = sender.getServer();
                if (server == null) return;
                for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                    if (target == sender) continue;
                    ItemStack targetRadio = target.getMainHandItem();
                    if (targetRadio.getItem() != RADIO.get()) targetRadio = target.getOffhandItem();
                    if (targetRadio.getItem() != RADIO.get()) {
                        targetRadio = findRadioOnBody(target);
                    }
                    if (targetRadio != null && RadioItem.getFrequency(targetRadio) == p.frequency) {
                        CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new VoicePlayPacket(p.audioData));
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class VoicePlayPacket {
        private final byte[] audioData;

        public VoicePlayPacket(byte[] audioData) {
            this.audioData = audioData;
        }

        public static void encode(VoicePlayPacket p, FriendlyByteBuf buf) {
            buf.writeByteArray(p.audioData);
        }

        public static VoicePlayPacket decode(FriendlyByteBuf buf) {
            return new VoicePlayPacket(buf.readByteArray());
        }

        public static void handle(VoicePlayPacket p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                if (ctx.get().getDirection().getReceptionSide().isClient()) {
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class RadioEvents {
        private static final Map<UUID, Boolean> STATE = new HashMap<>();
        private static int tickCounter = 0;

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (++tickCounter % 20 != 0) return;
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                boolean has = findRadioOnBody(player) != null;
                Boolean prev = STATE.get(player.getUUID());
                if (prev == null || prev != has) {
                    STATE.put(player.getUUID(), has);
                    CHANNEL.send(PacketDistributor.ALL.noArg(),
                            new BodySyncPacket(player.getUUID(), has));
                }
            }
        }

        @SubscribeEvent
        public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            STATE.remove(event.getEntity().getUUID());
        }
    }
}