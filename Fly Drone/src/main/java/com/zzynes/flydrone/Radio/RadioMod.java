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

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RadioMod {

    private static final int AUDIO_CHUNK_SIZE = 24000;
    private static final int MAX_AUDIO_CHUNKS = 64;
    private static final Random TRANSFER_RANDOM = new Random();

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FlyDroneMod.MOD_ID);

    public static final RegistryObject<Item> RADIO = ITEMS.register("radio",
            () -> new RadioItem(new Item.Properties().stacksTo(1)));

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FlyDroneMod.MOD_ID, "radio"),
            () -> "1",
            "1"::equals,
            "1"::equals
    );

    public static void init(IEventBus modBus) {
        ITEMS.register(modBus);

        CHANNEL.messageBuilder(SetFreqPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetFreqPacket::encode)
                .decoder(SetFreqPacket::decode)
                .consumerMainThread(SetFreqPacket::handle)
                .add();

        CHANNEL.messageBuilder(BodySyncPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BodySyncPacket::encode)
                .decoder(BodySyncPacket::decode)
                .consumerMainThread(BodySyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(VoicePacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(VoicePacket::encode)
                .decoder(VoicePacket::decode)
                .consumerMainThread(VoicePacket::handle)
                .add();

        CHANNEL.messageBuilder(VoicePlayPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(VoicePlayPacket::encode)
                .decoder(VoicePlayPacket::decode)
                .consumerMainThread(VoicePlayPacket::handle)
                .add();

        CHANNEL.messageBuilder(RadioSignalPacket.class, 4, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RadioSignalPacket::encode)
                .decoder(RadioSignalPacket::decode)
                .consumerMainThread(RadioSignalPacket::handle)
                .add();

        CHANNEL.messageBuilder(RadioReceiveOverlayPacket.class, 5, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RadioReceiveOverlayPacket::encode)
                .decoder(RadioReceiveOverlayPacket::decode)
                .consumerMainThread(RadioReceiveOverlayPacket::handle)
                .add();

        CHANNEL.messageBuilder(VoiceChunkPacket.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder(VoiceChunkPacket::encode)
                .decoder(VoiceChunkPacket::decode)
                .consumerMainThread(VoiceChunkPacket::handle)
                .add();

        CHANNEL.messageBuilder(VoicePlayChunkPacket.class, 7, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(VoicePlayChunkPacket::encode)
                .decoder(VoicePlayChunkPacket::decode)
                .consumerMainThread(VoicePlayChunkPacket::handle)
                .add();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RadioClient.init(modBus));

        MinecraftForge.EVENT_BUS.register(new RadioEvents());
    }

    public static ItemStack findRadioOnBody(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == RADIO.get() && stack != main && stack != off) {
                return stack;
            }
        }

        return null;
    }

    public static ItemStack findRadioForReceive(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() == RADIO.get()) {
            return main;
        }

        ItemStack off = player.getOffhandItem();
        if (off.getItem() == RADIO.get()) {
            return off;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == RADIO.get()) {
                return stack;
            }
        }

        return null;
    }

    public static ItemStack findRadioInHands(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() == RADIO.get()) {
            return main;
        }

        ItemStack off = player.getOffhandItem();
        if (off.getItem() == RADIO.get()) {
            return off;
        }

        return null;
    }

    public static void startVoiceRecording(Player player, int frequency) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            RadioAudioClient.startRecording();
            RadioClient.beginTransmitOverlay(frequency);
        });
    }

    public static void stopVoiceRecordingAndSend(Player player, int frequency) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            byte[] audio = RadioAudioClient.stopRecording();
            RadioClient.endTransmitOverlay();

            if (audio.length == 0) {
                CHANNEL.sendToServer(new RadioSignalPacket());
                return;
            }

            int transferId = TRANSFER_RANDOM.nextInt();
            int totalChunks = (audio.length + AUDIO_CHUNK_SIZE - 1) / AUDIO_CHUNK_SIZE;

            if (totalChunks <= 0 || totalChunks > MAX_AUDIO_CHUNKS) {
                CHANNEL.sendToServer(new RadioSignalPacket());
                return;
            }

            for (int i = 0; i < totalChunks; i++) {
                int start = i * AUDIO_CHUNK_SIZE;
                int end = Math.min(audio.length, start + AUDIO_CHUNK_SIZE);
                byte[] chunk = Arrays.copyOfRange(audio, start, end);
                CHANNEL.sendToServer(new VoiceChunkPacket(frequency, transferId, i, totalChunks, chunk));
            }
        });
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
            player.startUsingItem(hand);

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
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> consumer.accept(RadioClient.RADIO_EXTENSIONS));
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
                if (player == null) {
                    return;
                }

                if (p.freq < 100 || p.freq > 999) {
                    return;
                }

                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() == RADIO.get()) {
                    stack.getOrCreateTag().putInt("Frequency", p.freq);
                    return;
                }

                stack = player.getOffhandItem();
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
            });

            ctx.get().setPacketHandled(true);
        }
    }

    public static class VoiceChunkPacket {
        private final int frequency;
        private final int transferId;
        private final int chunkIndex;
        private final int totalChunks;
        private final byte[] data;

        public VoiceChunkPacket(int frequency, int transferId, int chunkIndex, int totalChunks, byte[] data) {
            this.frequency = frequency;
            this.transferId = transferId;
            this.chunkIndex = chunkIndex;
            this.totalChunks = totalChunks;
            this.data = data;
        }

        public static void encode(VoiceChunkPacket p, FriendlyByteBuf buf) {
            buf.writeVarInt(p.frequency);
            buf.writeInt(p.transferId);
            buf.writeVarInt(p.chunkIndex);
            buf.writeVarInt(p.totalChunks);
            buf.writeByteArray(p.data);
        }

        public static VoiceChunkPacket decode(FriendlyByteBuf buf) {
            return new VoiceChunkPacket(
                    buf.readVarInt(),
                    buf.readInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readByteArray()
            );
        }

        public static void handle(VoiceChunkPacket p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                if (p.data.length > AUDIO_CHUNK_SIZE) {
                    return;
                }

                if (p.totalChunks <= 0 || p.totalChunks > MAX_AUDIO_CHUNKS) {
                    return;
                }

                if (p.chunkIndex < 0 || p.chunkIndex >= p.totalChunks) {
                    return;
                }

                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) {
                    return;
                }

                ItemStack senderRadio = findRadioInHands(sender);
                if (senderRadio == null) {
                    return;
                }

                int senderFrequency = RadioItem.getFrequency(senderRadio);
                if (senderFrequency != p.frequency) {
                    return;
                }

                MinecraftServer server = sender.getServer();
                if (server == null) {
                    return;
                }

                for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                    if (target == sender) {
                        continue;
                    }

                    ItemStack targetRadio = findRadioForReceive(target);
                    if (targetRadio != null && RadioItem.getFrequency(targetRadio) == senderFrequency) {
                        CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new VoicePlayChunkPacket(
                                sender.getUUID(),
                                senderFrequency,
                                p.transferId,
                                p.chunkIndex,
                                p.totalChunks,
                                p.data
                        ));
                    }
                }
            });

            ctx.get().setPacketHandled(true);
        }
    }

    public static class VoicePlayChunkPacket {
        private static final Map<String, AudioAssembly> ASSEMBLIES = new HashMap<>();

        private final UUID senderId;
        private final int frequency;
        private final int transferId;
        private final int chunkIndex;
        private final int totalChunks;
        private final byte[] data;

        public VoicePlayChunkPacket(UUID senderId, int frequency, int transferId, int chunkIndex, int totalChunks, byte[] data) {
            this.senderId = senderId;
            this.frequency = frequency;
            this.transferId = transferId;
            this.chunkIndex = chunkIndex;
            this.totalChunks = totalChunks;
            this.data = data;
        }

        public static void encode(VoicePlayChunkPacket p, FriendlyByteBuf buf) {
            buf.writeUUID(p.senderId);
            buf.writeVarInt(p.frequency);
            buf.writeInt(p.transferId);
            buf.writeVarInt(p.chunkIndex);
            buf.writeVarInt(p.totalChunks);
            buf.writeByteArray(p.data);
        }

        public static VoicePlayChunkPacket decode(FriendlyByteBuf buf) {
            return new VoicePlayChunkPacket(
                    buf.readUUID(),
                    buf.readVarInt(),
                    buf.readInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readByteArray()
            );
        }

        public static void handle(VoicePlayChunkPacket p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                if (p.data.length > AUDIO_CHUNK_SIZE) {
                    return;
                }

                if (p.totalChunks <= 0 || p.totalChunks > MAX_AUDIO_CHUNKS) {
                    return;
                }

                if (p.chunkIndex < 0 || p.chunkIndex >= p.totalChunks) {
                    return;
                }

                if (ctx.get().getDirection().getReceptionSide().isClient()) {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                        String key = p.senderId + ":" + p.transferId;
                        AudioAssembly assembly = ASSEMBLIES.get(key);

                        if (assembly == null || assembly.totalChunks != p.totalChunks) {
                            assembly = new AudioAssembly(p.totalChunks);
                            ASSEMBLIES.put(key, assembly);
                        }

                        if (!assembly.received[p.chunkIndex]) {
                            assembly.received[p.chunkIndex] = true;
                            assembly.parts[p.chunkIndex] = p.data;
                            assembly.receivedCount++;
                        }

                        if (assembly.receivedCount >= assembly.totalChunks) {
                            ByteArrayOutputStream out = new ByteArrayOutputStream();

                            for (byte[] part : assembly.parts) {
                                if (part != null) {
                                    out.write(part, 0, part.length);
                                }
                            }

                            byte[] audio = out.toByteArray();
                            ASSEMBLIES.remove(key);
                            long durationMillis = Math.max(250L, audio.length * 1000L / 32000L);
                            RadioClient.showReceiveOverlay(p.frequency, durationMillis);
                            RadioAudioClient.play(audio);
                        }
                    });
                }
            });

            ctx.get().setPacketHandled(true);
        }

        private static class AudioAssembly {
            private final int totalChunks;
            private final byte[][] parts;
            private final boolean[] received;
            private int receivedCount;

            private AudioAssembly(int totalChunks) {
                this.totalChunks = totalChunks;
                this.parts = new byte[totalChunks][];
                this.received = new boolean[totalChunks];
                this.receivedCount = 0;
            }
        }
    }

    public static class RadioSignalPacket {
        public RadioSignalPacket() {
        }

        public static void encode(RadioSignalPacket p, FriendlyByteBuf buf) {
        }

        public static RadioSignalPacket decode(FriendlyByteBuf buf) {
            return new RadioSignalPacket();
        }

        public static void handle(RadioSignalPacket p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) {
                    return;
                }

                ItemStack senderRadio = findRadioInHands(sender);
                if (senderRadio == null) {
                    return;
                }

                MinecraftServer server = sender.getServer();
                if (server == null) {
                    return;
                }

                int frequency = RadioItem.getFrequency(senderRadio);

                for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                    if (target == sender) {
                        continue;
                    }

                    ItemStack targetRadio = findRadioForReceive(target);
                    if (targetRadio != null && RadioItem.getFrequency(targetRadio) == frequency) {
                        CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new RadioReceiveOverlayPacket(frequency));
                    }
                }
            });

            ctx.get().setPacketHandled(true);
        }
    }

    public static class RadioReceiveOverlayPacket {
        private final int frequency;

        public RadioReceiveOverlayPacket(int frequency) {
            this.frequency = frequency;
        }

        public static void encode(RadioReceiveOverlayPacket p, FriendlyByteBuf buf) {
            buf.writeVarInt(p.frequency);
        }

        public static RadioReceiveOverlayPacket decode(FriendlyByteBuf buf) {
            return new RadioReceiveOverlayPacket(buf.readVarInt());
        }

        public static void handle(RadioReceiveOverlayPacket p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                if (ctx.get().getDirection().getReceptionSide().isClient()) {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RadioClient.showReceiveOverlay(p.frequency));
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
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            if (++tickCounter % 20 != 0) {
                return;
            }

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return;
            }

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                boolean has = findRadioOnBody(player) != null;
                Boolean prev = STATE.get(player.getUUID());

                if (prev == null || prev != has) {
                    STATE.put(player.getUUID(), has);
                    CHANNEL.send(PacketDistributor.ALL.noArg(), new BodySyncPacket(player.getUUID(), has));
                }
            }
        }

        @SubscribeEvent
        public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            STATE.remove(event.getEntity().getUUID());
        }
    }
}