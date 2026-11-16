package com.eternalsnow.entity;

import com.eternalsnow.item.SnowShovelItem;
import com.eternalsnow.state.EternalSnowState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.Optional;

public class FestiveCopperGolemEntity extends PathfinderMob {

    private static final EntityDataAccessor<Optional<BlockPos>> DATA_ANCHOR_POS =
            SynchedEntityData.defineId(FestiveCopperGolemEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> DATA_CLEAN_RADIUS =
            SynchedEntityData.defineId(FestiveCopperGolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SWING_ANIM =
            SynchedEntityData.defineId(FestiveCopperGolemEntity.class, EntityDataSerializers.INT);

    private int workCooldownTicks = 20;
    @Nullable
    private BlockPos currentTargetSnow = null;

    public FestiveCopperGolemEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.25F);
        this.setPersistenceRequired();
        this.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.ARMOR, 6.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ANCHOR_POS, Optional.empty());
        this.entityData.define(DATA_CLEAN_RADIUS, 10);
        this.entityData.define(DATA_SWING_ANIM, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public Optional<BlockPos> getAnchorPos() {
        return this.entityData.get(DATA_ANCHOR_POS);
    }

    public void setAnchorPos(@Nullable BlockPos pos) {
        this.entityData.set(DATA_ANCHOR_POS, Optional.ofNullable(pos));
        this.currentTargetSnow = null;
    }

    public int getCleaningRadius() {
        return this.entityData.get(DATA_CLEAN_RADIUS);
    }

    public int getWorkRadius() {
        return getCleaningRadius();
    }

    public void setCleaningRadius(int radius) {
        int maxR = EternalSnowState.getEffectiveSettings().golemMaxRadius;
        this.entityData.set(DATA_CLEAN_RADIUS, Mth.clamp(radius, 2, Math.max(4, maxR)));
        this.currentTargetSnow = null;
    }

    public void setWorkRadius(int radius) {
        setCleaningRadius(radius);
    }

    public ItemStack getEquippedShovel() {
        return this.getMainHandItem();
    }

    public void triggerZonePreview() {
        if (this.level() instanceof ServerLevel sl) {
            spawnZonePreviewParticles(sl);
        }
    }

    public int getSwingAnimTicks() {
        return this.entityData.get(DATA_SWING_ANIM);
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.COPPER_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COPPER_BREAK;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);

        // 1. Stick selects this golem for anchor & radius setup
        if (held.is(Items.STICK)) {
            if (!this.level().isClientSide) {
                StickSelectionTracker.selectGolem(player.getUUID(), this.getId());
                this.level().playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.2F);
                player.displayClientMessage(
                        Component.translatable("eternalsnow.golem.stick_selected").withStyle(ChatFormatting.GOLD),
                        true
                );
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // 2. Give or swap SnowShovelItem
        if (held.getItem() instanceof SnowShovelItem) {
            if (!this.level().isClientSide) {
                ItemStack previous = this.getMainHandItem().copy();
                ItemStack toEquip = held.copy();
                toEquip.setCount(1);
                this.setItemSlot(EquipmentSlot.MAINHAND, toEquip);

                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                if (!previous.isEmpty()) {
                    if (!player.getInventory().add(previous)) {
                        this.spawnAtLocation(previous);
                    }
                }
                this.level().playSound(null, this.blockPosition(), SoundEvents.COPPER_PLACE, SoundSource.NEUTRAL, 1.0F, 1.15F);
                player.displayClientMessage(
                        Component.translatable("eternalsnow.msg.golem_shovel_given").withStyle(ChatFormatting.AQUA),
                        true
                );
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // 3. Shift + Empty hand retrieves the equipped shovel
        if (player.isShiftKeyDown() && held.isEmpty() && !this.getMainHandItem().isEmpty()) {
            if (!this.level().isClientSide) {
                ItemStack prev = this.getMainHandItem().copy();
                this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                if (!player.getInventory().add(prev)) {
                    this.spawnAtLocation(prev);
                }
                this.level().playSound(null, this.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.8F, 1.0F);
                player.displayClientMessage(
                        Component.translatable("eternalsnow.golem.shovel_taken").withStyle(ChatFormatting.YELLOW),
                        true
                );
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // 4. Normal Empty-hand click opens Golem radius/status GUI directly
        if (!player.isShiftKeyDown() && held.isEmpty()) {
            if (!this.level().isClientSide && player instanceof ServerPlayer sp) {
                ItemStack shovel = this.getEquippedShovel();
                boolean hasShovel = shovel.getItem() instanceof SnowShovelItem;
                int w = hasShovel ? SnowShovelItem.getSweepWidth(shovel) : 0;
                int l = hasShovel ? SnowShovelItem.getSweepLength(shovel) : 0;
                int h = hasShovel ? SnowShovelItem.getSweepHeight(shovel) : 0;
                    com.eternalsnow.network.PacketHandler.sendToPlayer(sp, new com.eternalsnow.network.ClientboundOpenGolemScreenPacket(
                        this.getId(),
                        this.getAnchorPos().orElse(this.blockPosition()),
                        this.getWorkRadius(),
                        hasShovel,
                        w,
                        l,
                        h
                ));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void tick() {
        super.tick();

        int anim = this.getSwingAnimTicks();
        if (anim > 0 && !this.level().isClientSide) {
            this.entityData.set(DATA_SWING_ANIM, anim - 1);
        }

        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.getAnchorPos().isEmpty()) {
            this.setAnchorPos(this.blockPosition());
        }

        ItemStack shovel = this.getMainHandItem();
        if (!(shovel.getItem() instanceof SnowShovelItem)) {
            return;
        }

        if (this.workCooldownTicks > 0) {
            this.workCooldownTicks--;
        }

        BlockPos center = this.getAnchorPos().orElse(this.blockPosition());
        int radius = this.getCleaningRadius();

        // Validate current target or search for snow in radius
        if (this.currentTargetSnow == null || !SnowShovelItem.isSnow(serverLevel.getBlockState(this.currentTargetSnow))
                || this.currentTargetSnow.distSqr(center) > (double) ((radius + 1) * (radius + 1))) {
            if (this.tickCount % 10 == 0) {
                this.currentTargetSnow = findNearestSnowInRadius(serverLevel, center, radius);
            }
        }

        if (this.currentTargetSnow != null) {
            double distSq = this.distanceToSqr(
                    this.currentTargetSnow.getX() + 0.5D,
                    this.currentTargetSnow.getY(),
                    this.currentTargetSnow.getZ() + 0.5D
            );
            if (distSq > 6.5D) {
                if (this.tickCount % 10 == 0) {
                    this.getNavigation().moveTo(
                            this.currentTargetSnow.getX() + 0.5D,
                            this.currentTargetSnow.getY(),
                            this.currentTargetSnow.getZ() + 0.5D,
                            1.05D
                    );
                }
            } else if (this.workCooldownTicks <= 0) {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(
                        this.currentTargetSnow.getX() + 0.5D,
                        this.currentTargetSnow.getY() + 0.5D,
                        this.currentTargetSnow.getZ() + 0.5D
                );
                Direction dir = Direction.fromYRot(
                        (float) (Mth.atan2(
                                this.currentTargetSnow.getZ() + 0.5D - this.getZ(),
                                this.currentTargetSnow.getX() + 0.5D - this.getX()
                        ) * (180.0D / Math.PI)) - 90.0F
                );
                int cleared = SnowShovelItem.sweepSnow(serverLevel, this.currentTargetSnow, dir, shovel);
                this.swing(InteractionHand.MAIN_HAND);
                this.entityData.set(DATA_SWING_ANIM, 12);
                if (cleared > 0) {
                    serverLevel.playSound(null, this.blockPosition(), SoundEvents.SNOW_BREAK, SoundSource.NEUTRAL, 0.85F, 1.1F);
                }
                this.currentTargetSnow = null;
                this.workCooldownTicks = Math.max(10, EternalSnowState.getServerSettings().golemSwingIntervalTicks);
            }
        } else if (this.tickCount % 40 == 0 && this.blockPosition().distSqr(center) > 9.0D) {
            // Return near assigned post when zone is clean
            this.getNavigation().moveTo(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D, 0.9D);
        }
    }

    @Nullable
    private BlockPos findNearestSnowInRadius(ServerLevel level, BlockPos center, int radius) {
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int rSq = radius * radius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > rSq) {
                    continue;
                }
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) {
                    continue;
                }
                int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                // Check motionY and motionY - 1 (for multi-layer snow & SNOW_BLOCKs)
                for (int dy = 0; dy >= -2; dy--) {
                    cursor.set(x, topY + dy, z);
                    if (Math.abs(cursor.getY() - center.getY()) > 8) {
                        continue;
                    }
                    if (SnowShovelItem.isSnow(level.getBlockState(cursor))) {
                        double d = this.distanceToSqr(x + 0.5D, cursor.getY(), z + 0.5D);
                        if (d < bestDistSq) {
                            bestDistSq = d;
                            best = cursor.immutable();
                        }
                        break;
                    }
                }
            }
        }
        return best;
    }

    public void spawnZonePreviewParticles(ServerLevel level) {
        BlockPos center = this.getAnchorPos().orElse(this.blockPosition());
        int r = this.getCleaningRadius();
        int points = Math.max(24, r * 6);
        for (int i = 0; i < points; i++) {
            double ang = (Math.PI * 2.0D * i) / points;
            double px = center.getX() + 0.5D + Math.cos(ang) * r;
            double pz = center.getZ() + 0.5D + Math.sin(ang) * r;
            int py = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(px), Mth.floor(pz));
            level.sendParticles(ParticleTypes.END_ROD, px, py + 0.25D, pz, 1, 0.0D, 0.03D, 0.0D, 0.01D);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.getAnchorPos().ifPresent(pos -> tag.put("AnchorPos", NbtUtils.writeBlockPos(pos)));
        tag.putInt("CleanRadius", this.getCleaningRadius());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AnchorPos")) {
            this.setAnchorPos(NbtUtils.readBlockPos(tag.getCompound("AnchorPos")));
        }
        if (tag.contains("CleanRadius")) {
            this.setCleaningRadius(tag.getInt("CleanRadius"));
        }
    }
}
