package com.zzynes.fly_drone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.Collections;

public class FPVDroneEntity extends LivingEntity {

    public float customPitch = 0.0F;
    public float customYaw = 0.0F;
    public float customRoll = 0.0F;
    public final Quaternionf droneRotation = new Quaternionf();

    public float thrustPower = 0.0F;
    public float sidePower = 0.0F;

    private static final double MAX_SPEED_PER_TICK = 1.0416D;

    // ЛОГИКА АККУМУЛЯТОРА
    public int maxBattery = 4000;
    public int batteryCharge = 0;
    public boolean isMotorsActive = false;
    public boolean hasBattery = false;

    // СИНХРОНИЗАЦИЯ БОЕВОЙ ЧАСТИ (0 - пусто, 1 - C4, 2 - РПГ)
    private static final EntityDataAccessor<Integer> ATTACHED_WEAPON =
            SynchedEntityData.defineId(FPVDroneEntity.class, EntityDataSerializers.INT);

    public FPVDroneEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACHED_WEAPON, 0);
    }

    public int getAttachedWeapon() {
        return this.entityData.get(ATTACHED_WEAPON);
    }

    public void setAttachedWeapon(int weaponType) {
        this.entityData.set(ATTACHED_WEAPON, weaponType);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    public void tick() {
        super.tick();
        updateQuaternion();

        if (this.hasBattery && this.isMotorsActive && this.batteryCharge > 0) {
            int burn = 1 + (int)(this.thrustPower * 4);
            this.batteryCharge = Math.max(0, this.batteryCharge - burn);

            if (this.batteryCharge <= 0) {
                this.isMotorsActive = false;
                this.thrustPower = 0.0F;
                this.sidePower = 0.0F;
            }
        }
        Vec3 motion = this.getDeltaMovement();
        double drag = 0.98D;

        if (this.hasBattery && this.isMotorsActive && this.thrustPower > 0.0F && this.batteryCharge > 0) {
            Vec3 thrustDir = this.getThrustVector();
            Vec3 thrustForce = thrustDir.scale(this.thrustPower * 0.08D);
            motion = motion.add(thrustForce);
        }

        if (this.hasBattery && this.isMotorsActive && this.sidePower != 0.0F && this.batteryCharge > 0) {
            Vector3f sideDir = new Vector3f(1.0F, 0.0F, 0.0F);
            droneRotation.transform(sideDir);
            Vec3 sideForce = new Vec3(sideDir.x(), sideDir.y(), sideDir.z()).scale(this.sidePower * 0.04D);
            motion = motion.add(sideForce);
        }

        if (!this.isNoGravity()) {
            motion = motion.add(0, -0.04D, 0);
        }

        motion = new Vec3(motion.x * drag, motion.y * drag, motion.z * drag);

        double currentSpeed = motion.length();
        if (currentSpeed > MAX_SPEED_PER_TICK) {
            motion = motion.scale(MAX_SPEED_PER_TICK / currentSpeed);
        }

        this.setDeltaMovement(motion);
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());

        if (!this.level().isClientSide && (this.horizontalCollision || this.verticalCollision)) {
            if (this.hasBattery && this.isMotorsActive && currentSpeed > 0.2D) {
                this.explodeDrone();
            }
        }
    }

    public void fireAttachedWeapon() {
        if (!this.level().isClientSide) {
            int weaponType = this.getAttachedWeapon();

            if (weaponType == 2) {
                RPGRocketEntity rocket = new RPGRocketEntity(ModEntities.RPG_ROCKET.get(), this.level(), this, this.getDeltaMovement());
                this.level().addFreshEntity(rocket);
                this.setAttachedWeapon(0);
            }
        }
    }

    // МЕТОД ДЕТОНАЦИИ C4 (Вызывается по ПКМ с пульта)
    public void detonateC4() {
        if (!this.level().isClientSide) {
            int weaponType = this.getAttachedWeapon();

            // Проверяем, что на дроне действительно висит C4 (тип 1)
            if (weaponType == 1) {
                // Создаем мощный объемный взрыв радиусом 5.0 блоков
                this.level().explode(this, this.getX(), this.getY(), this.getZ(), 5.0F, Level.ExplosionInteraction.TNT);
                // Очищаем подвес и уничтожаем дрон
                this.setAttachedWeapon(0);
                this.discard();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && this.hasBattery && this.isMotorsActive) {
            this.explodeDrone();
            return true;
        }
        return super.hurt(source, amount);
    }

    private void explodeDrone() {
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), 2.0F, Level.ExplosionInteraction.TNT);
        this.discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (!player.isShiftKeyDown()) {
            if (!this.level().isClientSide) {
                if (this.getAttachedWeapon() == 0) {
                    if (heldItem.is(ModItems.CI4.get())) {
                        this.setAttachedWeapon(1);
                        if (!player.getAbilities().instabuild) heldItem.shrink(1);
                        player.sendSystemMessage(Component.literal("§cВзрывчатка C4 успешно закреплена под брюхом дрона!"));
                        return InteractionResult.SUCCESS;
                    }
                    if (heldItem.is(ModItems.RPG_SNAR.get())) {
                        this.setAttachedWeapon(2);
                        if (!player.getAbilities().instabuild) heldItem.shrink(1);
                        player.sendSystemMessage(Component.literal("§cКумулятивный снаряд РПГ-74 успешно подвешен на дрон!"));
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (player.isShiftKeyDown()) {
            if (!this.level().isClientSide) {
                if (this.hasBattery) {
                    ItemStack droppedBattery = new ItemStack(ModItems.BATTERY.get());
                    int damageValue = 200 - (this.batteryCharge / 20);
                    droppedBattery.setDamageValue(Math.min(199, Math.max(0, damageValue)));

                    ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), this.getY() + 0.2D, this.getZ(), droppedBattery);
                    this.level().addFreshEntity(itemEntity);

                    this.hasBattery = false;
                    this.batteryCharge = 0;
                    this.isMotorsActive = false;
                    this.thrustPower = 0.0F;

                    player.sendSystemMessage(Component.literal("§6Батарея извлечена из дрона!"));
                    return InteractionResult.SUCCESS;
                }

                if (!this.hasBattery && heldItem.is(ModItems.BATTERY.get())) {
                    this.hasBattery = true;
                    int itemDamage = heldItem.getDamageValue();
                    this.batteryCharge = (200 - itemDamage) * 20;

                    if (!player.getAbilities().instabuild) {
                        heldItem.shrink(1);
                    }

                    player.sendSystemMessage(Component.literal("§aНовая АКБ успешно установлена! Заряд: " + (this.batteryCharge / 40) + "%"));
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.interact(player, hand);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BatteryCharge", this.batteryCharge);
        tag.putBoolean("MotorsActive", this.isMotorsActive);
        tag.putBoolean("HasBattery", this.hasBattery);
        tag.putInt("AttachedWeapon", this.getAttachedWeapon());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BatteryCharge")) this.batteryCharge = tag.getInt("BatteryCharge");
        if (tag.contains("MotorsActive")) this.isMotorsActive = tag.getBoolean("MotorsActive");
        if (tag.contains("HasBattery")) this.hasBattery = tag.getBoolean("HasBattery");
        if (tag.contains("AttachedWeapon")) this.setAttachedWeapon(tag.getInt("AttachedWeapon"));
    }

    private void updateQuaternion() {
        float p = (float) Math.toRadians(customPitch);
        float y = (float) Math.toRadians(customYaw);
        float r = (float) Math.toRadians(customRoll);

        droneRotation.identity();
        droneRotation.rotateY(y);
        droneRotation.rotateX(p);
        droneRotation.rotateZ(r);
    }

    public Vec3 getThrustVector() {
        Vector3f thrust = new Vector3f(0.0F, 1.0F, 0.0F);
        droneRotation.transform(thrust);
        return new Vec3(thrust.x(), thrust.y(), thrust.z());
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getItemBySlot(net.minecraft.world.entity.EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(net.minecraft.world.entity.EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }
}
