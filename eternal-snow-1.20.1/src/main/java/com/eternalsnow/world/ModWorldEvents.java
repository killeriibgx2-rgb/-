package com.eternalsnow.world;

import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.entity.FestiveCopperGolemEntity;
import com.eternalsnow.entity.StickSelectionTracker;
import com.eternalsnow.item.SnowShovelItem;
import com.eternalsnow.item.WoodenFirestarterItem;
import com.eternalsnow.network.ClientboundOpenGolemScreenPacket;
import com.eternalsnow.network.PacketHandler;
import com.eternalsnow.registry.ModRegistry;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.survival.PlayerSurvivalManager;
import com.eternalsnow.thermal.ThermalEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.SaplingGrowTreeEvent;
import net.minecraftforge.eventbus.api.Event;

public final class ModWorldEvents {

    private ModWorldEvents() {
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState placed = event.getPlacedBlock();

        // 1. Update chunk-indexed ThermalEngine
        ThermalEngine.onBlockPlaced(serverLevel, pos);

        // 2. Campfire Overhaul (Г-3): Newly placed campfires start UNLIT with 0/30 sticks!
        if (placed.getBlock() instanceof CampfireBlock) {
            CampfireFuelManager.onCampfirePlaced(serverLevel, pos, placed);
            return;
        }

        // 3. Festive Copper Golem creation (Copper Block atop 2 Snow Blocks)
        if (isCopperHeadBlock(placed)) {
            BlockPos body1 = pos.below();
            BlockPos body2 = pos.below(2);
            if (serverLevel.getBlockState(body1).is(Blocks.SNOW_BLOCK)
                    && serverLevel.getBlockState(body2).is(Blocks.SNOW_BLOCK)) {
                serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                serverLevel.setBlock(body1, Blocks.AIR.defaultBlockState(), 3);
                serverLevel.setBlock(body2, Blocks.AIR.defaultBlockState(), 3);

                FestiveCopperGolemEntity golem = ModRegistry.FESTIVE_COPPER_GOLEM.get().create(serverLevel);
                if (golem != null) {
                    golem.moveTo(body2.getX() + 0.5D, body2.getY(), body2.getZ() + 0.5D, 0.0F, 0.0F);
                    golem.setAnchorPos(body2);
                    serverLevel.addFreshEntity(golem);

                    serverLevel.playSound(null, body2, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 1.0F, 1.1F);
                    serverLevel.playSound(null, body2, SoundEvents.SNOW_GOLEM_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
                    serverLevel.sendParticles(ParticleTypes.WAX_ON, body2.getX() + 0.5D, body2.getY() + 1.1D, body2.getZ() + 0.5D, 28, 0.4D, 0.6D, 0.4D, 0.04D);
                    serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, body2.getX() + 0.5D, body2.getY() + 1.4D, body2.getZ() + 0.5D, 14, 0.35D, 0.4D, 0.35D, 0.02D);

                    if (event.getEntity() instanceof ServerPlayer sp) {
                        sp.displayClientMessage(Component.translatable("eternalsnow.msg.golem_created"), true);
                    }
                }
            }
        }
    }

    private static boolean isCopperHeadBlock(BlockState state) {
        return state.is(Blocks.COPPER_BLOCK)
                || state.is(Blocks.WAXED_COPPER_BLOCK)
                || state.is(Blocks.CUT_COPPER)
                || state.is(Blocks.WAXED_CUT_COPPER)
                || state.is(Blocks.EXPOSED_COPPER)
                || state.is(Blocks.WAXED_EXPOSED_COPPER);
    }

    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState brokenState = event.getState();

        // Clean up Campfire fuel entry if campfire broken
        if (brokenState.getBlock() instanceof CampfireBlock) {
            CampfireFuelManager.onCampfireBroken(serverLevel, pos);
        }

        ItemStack held = event.getPlayer().getMainHandItem();
        if (held.getItem() instanceof SnowShovelItem) {
            BlockState targetState = serverLevel.getBlockState(pos);
            if (!SnowShovelItem.isSnowTarget(targetState)) {
                event.setCanceled(true);
                return;
            }
            if (event.getPlayer() instanceof ServerPlayer sp) {
                event.setCanceled(true);
                int cleared = SnowShovelItem.clearSnowVolume(serverLevel, pos, sp.getDirection(), held);
                if (cleared > 0) {
                    serverLevel.playSound(null, pos, SoundEvents.SNOW_BREAK, SoundSource.BLOCKS, 0.9F, 1.05F);
                    if (!sp.isCreative()) {
                        held.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(sp.getUsedItemHand()));
                    }
                }
                return;
            }
        }

        ThermalEngine.onBlockBroken(serverLevel, pos);
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack held = event.getItemStack();
        if (held.getItem() instanceof SnowShovelItem) {
            BlockState state = event.getLevel().getBlockState(event.getPos());
            if (!SnowShovelItem.isSnowTarget(state)) {
                event.setCanceled(true);
            }
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState clickedState = level.getBlockState(pos);

        // 1. Stick Golem Anchor Selection (takes priority if player actively clicked a Festive Copper Golem first!)
        if (held.is(Items.STICK)
                && event.getEntity() instanceof ServerPlayer sp
                && level instanceof ServerLevel sl
                && StickSelectionTracker.getSelectedGolemId(sp.getUUID()) != null) {
            Integer golemId = StickSelectionTracker.consumeSelectedGolem(sp);
            if (golemId != null) {
                Entity entity = sl.getEntity(golemId);
                if (entity instanceof FestiveCopperGolemEntity golem && golem.isAlive()) {
                    BlockPos anchor = pos.above();
                    golem.setAnchorPos(anchor);
                    golem.triggerZonePreview();
                    sl.playSound(null, anchor, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.9F, 1.25F);

                    ItemStack shovel = golem.getEquippedShovel();
                    boolean hasSnowShovel = shovel.getItem() instanceof SnowShovelItem;
                    int sw = hasSnowShovel ? SnowShovelItem.getSweepWidth(shovel) : 0;
                    int slen = hasSnowShovel ? SnowShovelItem.getSweepLength(shovel) : 0;
                    int sh = hasSnowShovel ? SnowShovelItem.getSweepHeight(shovel) : 0;

                    PacketHandler.sendToPlayer(sp, new ClientboundOpenGolemScreenPacket(
                            golem.getId(), anchor, golem.getWorkRadius(), hasSnowShovel, sw, slen, sh
                    ));
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return;
                }
            }
        }

        // 2. Stick-Fueled Campfire Interaction (Г-3: load sticks up to 30 = 5:00, instant igniters; WoodenFirestarterItem passes through to 10s hold!)
        if (clickedState.getBlock() instanceof CampfireBlock && !clickedState.getValue(CampfireBlock.WATERLOGGED)) {
            if (held.getItem() instanceof WoodenFirestarterItem) {
                return; // Let WoodenFirestarterItem#use start its 10-second hold!
            }
            if (level.isClientSide) {
                if (held.is(Items.STICK) || (!clickedState.getValue(CampfireBlock.LIT) && WoodenFirestarterItem.isInstantIgniter(held))) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                }
            } else if (level instanceof ServerLevel sl && event.getEntity() instanceof ServerPlayer sp) {
                InteractionResult res = CampfireFuelManager.handleCampfireInteract(
                        sl, pos, clickedState, sp, event.getHand(), held
                );
                if (res != InteractionResult.PASS) {
                    event.setCanceled(true);
                    event.setCancellationResult(res);
                }
            }
        }
    }

    /**
     * Б-2: Crops pause growth in sub-zero cold (< 1.5°C) and grow indoors where room temp >= +2°C!
     */
    public static void onCropGrowPre(BlockEvent.CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        ServerSnowSettings settings = EternalSnowState.getServerSettings();
        if (!settings.enabled || !EternalSnowState.isOverworldLike(serverLevel)) {
            return;
        }
        if (ThermalEngine.getLocalTemperatureC(serverLevel, event.getPos()) < 1.5F) {
            event.setResult(Event.Result.DENY);
        }
    }

    /**
     * Б-2: Saplings pause in deep frost (< 1.5°C) unless warmed by indoor heat or nearby fires!
     */
    public static void onSaplingGrow(SaplingGrowTreeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        ServerSnowSettings settings = EternalSnowState.getServerSettings();
        if (!settings.enabled || !EternalSnowState.isOverworldLike(serverLevel)) {
            return;
        }
        if (ThermalEngine.getLocalTemperatureC(serverLevel, event.getPos()) < 1.5F) {
            event.setResult(Event.Result.DENY);
        }
    }

    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerSurvivalManager.onItemConsumed(sp, event.getItem());
        }
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }
        // v2.6.1: survival state must also survive dimension changes (End return, portals),
        // not just death — otherwise body temp / thirst silently reset when changing worlds.
        CompoundTag oldData = event.getOriginal().getPersistentData();
        if (oldData.contains(Player.PERSISTED_NBT_TAG)) {
            newPlayer.getPersistentData().put(
                    Player.PERSISTED_NBT_TAG,
                    oldData.getCompound(Player.PERSISTED_NBT_TAG).copy()
            );
        }
        if (event.isWasDeath()) {
            PlayerSurvivalManager.resetOnRespawn(newPlayer);
        }
    }
}
