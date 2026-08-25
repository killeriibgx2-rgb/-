package com.zzynes.flydrone.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FpvDroneEntity extends PathfinderMob implements GeoEntity {

    private AnimatableInstanceCache cache;

    private static final EntityDataAccessor<Float> THROTTLE = SynchedEntityData.defineId(FpvDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> BATTERY_TICKS = SynchedEntityData.defineId(FpvDroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MOTOR_TEMP = SynchedEntityData.defineId(FpvDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BATTERY_TEMP = SynchedEntityData.defineId(FpvDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_FLYING = SynchedEntityData.defineId(FpvDroneEntity.class, EntityDataSerializers.BOOLEAN);

    private float pitchRate = 0f;
    private float rollRate = 0f;
    private float yawRate = 0f;
    private final Quaternionf orientation = new Quaternionf();

    private static final float MAX_THROTTLE = 1.0f;
    private static final float THROTTLE_RAMP_SPEED = 0.05f;
    private static final float ANGULAR_DAMPING = 0.92f;
    private static final float GRAVITY_FORCE = 0.04f;
    private static final float LIFT_MULTIPLIER = 0.08f;
    private static final float MAX_ANGULAR_RATE = 3.0f;

    public FpvDroneEntity(EntityType<? extends FpvDroneEntity> type, Level level) {
        super(type, level);
        this.cache = GeckoLibUtil.createInstanceCache(this);
        this.setNoGravity(true);
        this.orientation.identity();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(THROTTLE, 0.0f);
        this.entityData.define(BATTERY_TICKS, 4800);
        this.entityData.define(MOTOR_TEMP, 32.0f);
        this.entityData.define(BATTERY_TEMP, 32.0f);
        this.entityData.define(IS_FLYING, false);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) return;

        float throttle = this.entityData.get(THROTTLE);

        pitchRate *= ANGULAR_DAMPING;
        rollRate *= ANGULAR_DAMPING;
        yawRate *= ANGULAR_DAMPING;

        pitchRate = Mth.clamp(pitchRate, -MAX_ANGULAR_RATE, MAX_ANGULAR_RATE);
        rollRate = Mth.clamp(rollRate, -MAX_ANGULAR_RATE, MAX_ANGULAR_RATE);
        yawRate = Mth.clamp(yawRate, -MAX_ANGULAR_RATE, MAX_ANGULAR_RATE);

        Quaternionf deltaRotation = new Quaternionf()
                .rotateX(pitchRate * 0.01745f)
                .rotateZ(rollRate * 0.01745f)
                .rotateY(yawRate * 0.01745f);
        orientation.mul(deltaRotation);
        orientation.normalize();

        float x = 0f, y = 1f, z = 0f;
        float qx = orientation.x, qy = orientation.y, qz = orientation.z, qw = orientation.w;
        float ix = qw * x + qy * z - qz * y;
        float iy = qw * y + qz * x - qx * z;
        float iz = qw * z + qx * y - qy * x;
        float iw = -qx * x - qy * y - qz * z;
        float rx = ix * qw + iw * -qx + iy * -qz - iz * -qy;
        float ry = iy * qw + iw * -qy + iz * -qx - ix * -qz;
        float rz = iz * qw + iw * -qz + ix * -qy - iy * -qx;

        float lift = throttle * LIFT_MULTIPLIER * ry;

        double motionX = this.getDeltaMovement().x;
        double motionY = this.getDeltaMovement().y;
        double motionZ = this.getDeltaMovement().z;

        motionY += (double)(lift - GRAVITY_FORCE);
        motionX += (double)(throttle * 0.02f * (-rx));
        motionZ += (double)(throttle * 0.02f * rz);

        this.setDeltaMovement(motionX, motionY, motionZ);

        if (throttle > 0.01f) {
            int ticks = this.entityData.get(BATTERY_TICKS);
            ticks -= Math.max(1, (int)(throttle * 3));
            if (ticks < 0) ticks = 0;
            this.entityData.set(BATTERY_TICKS, ticks);

            float motorTemp = this.entityData.get(MOTOR_TEMP);
            motorTemp += throttle * 0.1f;
            motorTemp = Math.min(motorTemp, 120.0f);
            this.entityData.set(MOTOR_TEMP, motorTemp);

            float battTemp = this.entityData.get(BATTERY_TEMP);
            battTemp += throttle * 0.05f;
            battTemp = Math.min(battTemp, 80.0f);
            this.entityData.set(BATTERY_TEMP, battTemp);
        } else {
            float motorTemp = this.entityData.get(MOTOR_TEMP);
            motorTemp = Math.max(32.0f, motorTemp - 0.05f);
            this.entityData.set(MOTOR_TEMP, motorTemp);

            float battTemp = this.entityData.get(BATTERY_TEMP);
            battTemp = Math.max(32.0f, battTemp - 0.02f);
            this.entityData.set(BATTERY_TEMP, battTemp);
        }

        this.entityData.set(IS_FLYING, throttle > 0.01f && this.entityData.get(BATTERY_TICKS) > 0);
    }

    public void setThrottle(float value) {
        float current = this.entityData.get(THROTTLE);
        float target = Mth.clamp(value, 0f, MAX_THROTTLE);
        float newThrottle = Mth.lerp(THROTTLE_RAMP_SPEED, current, target);
        this.entityData.set(THROTTLE, newThrottle);
    }

    public void addPitchRate(float rate) { this.pitchRate += rate; }
    public void addRollRate(float rate) { this.rollRate += rate; }
    public void addYawRate(float rate) { this.yawRate += rate; }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(BATTERY_TICKS, tag.getInt("BatteryTicks"));
        this.entityData.set(MOTOR_TEMP, tag.getFloat("MotorTemp"));
        this.entityData.set(BATTERY_TEMP, tag.getFloat("BatteryTemp"));
        this.pitchRate = tag.getFloat("PitchRate");
        this.rollRate = tag.getFloat("RollRate");
        this.yawRate = tag.getFloat("YawRate");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BatteryTicks", this.entityData.get(BATTERY_TICKS));
        tag.putFloat("MotorTemp", this.entityData.get(MOTOR_TEMP));
        tag.putFloat("BatteryTemp", this.entityData.get(BATTERY_TEMP));
        tag.putFloat("PitchRate", this.pitchRate);
        tag.putFloat("RollRate", this.rollRate);
        tag.putFloat("YawRate", this.yawRate);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public float getThrottle() { return this.entityData.get(THROTTLE); }
    public int getBatteryTicks() { return this.entityData.get(BATTERY_TICKS); }
    public float getMotorTemp() { return this.entityData.get(MOTOR_TEMP); }
    public float getBatteryTemp() { return this.entityData.get(BATTERY_TEMP); }
    public boolean isFlying() { return this.entityData.get(IS_FLYING); }
    public Quaternionf getOrientation() { return this.orientation; }
}