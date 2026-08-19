package com.zzynes.fly_drone;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import java.util.List;

public class RPGRocketEntity extends ThrowableProjectile implements ItemSupplier {

    public RPGRocketEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    public RPGRocketEntity(EntityType<? extends ThrowableProjectile> type, Level level, LivingEntity shooter, Vec3 droneMotion) {
        super(type, level);
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getY() - 0.15D, shooter.getZ());
        Vec3 lookDir = shooter.getLookAngle();
        this.setDeltaMovement(lookDir.scale(2.5D).add(droneMotion));
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitresult.getType() != HitResult.Type.MISS) {
            this.onHit(hitresult);
        }

        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        if (!this.isNoGravity()) {
            this.setDeltaMovement(motion.add(0.0D, -0.015D, 0.0D));
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 15.0F);
            this.explodeRocket();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            this.explodeRocket();
        }
    }

    // МЕТОД ДЕТОНАЦИИ С КУМУЛЯТИВНЫМ ПРОБИТИЕМ СТЕНЫ
    private void explodeRocket() {
        // 1. Создаем базовый взрыв на месте контакта
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), 3.5F, Level.ExplosionInteraction.TNT);

        // 2. Рассчитываем вектор направления кумулятивной струи
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() > 0.001D) {
            Vec3 direction = motion.normalize(); // Направление луча совпадает с полетом ракеты
            Vec3 startPos = new Vec3(this.getX(), this.getY(), this.getZ());

            // 3. Запускаем цикл сканирования луча на 7 блоков вперед (шаг 0.5 блока для точности)
            for (double d = 0.0D; d <= 7.0D; d += 0.5D) {
                Vec3 currentRayPos = startPos.add(direction.scale(d));
                BlockPos blockPos = BlockPos.containing(currentRayPos);

                // Уничтожаем блоки на пути струи (кроме бедрока и воздуха)
                if (!this.level().getBlockState(blockPos).isAir() && this.level().getBlockState(blockPos).getBlock() != Blocks.BEDROCK) {
                    this.level().setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
                }

                // Наносим колоссальный урон живым существам, если они попали под струю
                AABB damageArea = new AABB(currentRayPos.x - 0.25D, currentRayPos.y - 0.25D, currentRayPos.z - 0.25D,
                        currentRayPos.x + 0.25D, currentRayPos.y + 0.25D, currentRayPos.z + 0.25D);
                List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, damageArea);

                for (LivingEntity target : targets) {
                    if (target != this.getOwner()) {
                        // Кумулятивная струя прожигает броню и наносит 30 единиц чистого урона
                        target.hurt(this.damageSources().magic(), 30.0F);
                    }
                }
            }
        }

        // Удаляем ракету из мира
        this.discard();
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.RPG_SNAR.get());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
