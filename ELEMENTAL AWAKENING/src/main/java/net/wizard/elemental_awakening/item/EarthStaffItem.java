package net.wizard.elemental_awakening.item;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.wizard.elemental_awakening.client.EarthMenuScreen;
import net.wizard.elemental_awakening.client.FireMenuScreen;
import net.wizard.elemental_awakening.client.WaterMenuScreen;
import net.wizard.elemental_awakening.client.AirMenuScreen;
import net.wizard.elemental_awakening.mana.PlayerManaProvider;
import net.wizard.elemental_awakening.networking.ModMessages;
import net.wizard.elemental_awakening.networking.OpenElementMenuPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class EarthStaffItem extends Item {
    public static final List<ActiveWall> ACTIVE_WALLS = new ArrayList<>();

    public EarthStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        // --- КЛИЕНТ: ОТКРЫТИЕ СТИХИЙНЫХ МЕНЮ (SHIFT + ПКМ) ---
        if (level.isClientSide) {
            if (player.isCrouching()) {
                player.getCapability(PlayerManaProvider.PLAYER_MANA).ifPresent(mana -> {
                    int element = mana.getElement();
                    // Палочка сама смотрит на ID стихии и открывает нужное меню 256х256!
                    if (element == 1) Minecraft.getInstance().setScreen(new EarthMenuScreen());
                    else if (element == 2) Minecraft.getInstance().setScreen(new FireMenuScreen());
                    else if (element == 3) Minecraft.getInstance().setScreen(new WaterMenuScreen());
                    else if (element == 4) Minecraft.getInstance().setScreen(new AirMenuScreen());
                });
                return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
            }
        }

        // --- СЕРВЕР: ОБРАБОТКА МЕНЮ ВЫБОРА И КАСТ МАГИИ ---
        if (!level.isClientSide) {
            if (player.isCrouching() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.getCapability(PlayerManaProvider.PLAYER_MANA).ifPresent(mana -> {
                    if (mana.getElement() == 0) {
                        ModMessages.sendToPlayer(new OpenElementMenuPacket(), serverPlayer);
                    }
                });
                return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
            }

            // ОБЫЧНЫЙ ПКМ: АВТОПОДСТРОЙКА ПОД ЗАКЛИНАНИЕ В NBT
            CompoundTag nbt = itemStack.getTag();
            if (nbt != null && nbt.contains("SelectedSpell")) {
                String selectedSpell = nbt.getString("SelectedSpell");

                player.getCapability(PlayerManaProvider.PLAYER_MANA).ifPresent(mana -> {

                    // 🟫 ЗЕМЛЯ — СТЕНА ИЗ ГРЯЗИ
                    if (selectedSpell.equals("earth_stena") && mana.getMana() >= 10.0F) {
                        mana.consumeMana(10.0F);
                        Direction direction = player.getDirection();
                        BlockPos startPos = player.blockPosition().relative(direction, 1);
                        ActiveWall wall = new ActiveWall(level, startPos, direction);
                        ACTIVE_WALLS.add(wall);
                    }

                    // 🟥 ОГОНЬ — ОГНЕННЫЙ ШАР
                    else if (selectedSpell.equals("fire_ball") && mana.getMana() >= 15.0F) {
                        mana.consumeMana(15.0F);
                        Vec3 look = player.getLookAngle();
                        LargeFireball fireball = new LargeFireball(level, player, look.x, look.y, look.z, 1);
                        fireball.setPos(player.getX() + look.x * 1.5, player.getEyeY() + look.y * 0.5, player.getZ() + look.z * 1.5);
                        level.addFreshEntity(fireball);
                        level.playSound(null, player.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
                    }

                    // 🟦 ВОДА — ЛЕЧЕБНЫЙ ЭФФЕКТ
                    else if (selectedSpell.equals("water_heal") && mana.getMana() >= 20.0F) {
                        mana.consumeMana(20.0F);
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                        level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
                    }

                    // ⬜ ВОЗДУХ — РЫВОК ВПЕРЕД (ДЭШ)
                    else if (selectedSpell.equals("air_dash") && mana.getMana() >= 12.0F) {
                        mana.consumeMana(12.0F);
                        Vec3 look = player.getLookAngle();
                        player.setDeltaMovement(look.x * 1.8, look.y * 1.2, look.z * 1.8);
                        player.hurtMarked = true;
                        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0F, 1.5F);
                    }
                });
            }
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    // --- ВНУТРЕННИЙ КЛАСС ЧИСТОЙ СТЕНЫ ЗЕМЛИ ---
    public static class ActiveWall {
        private final Level level;
        private final BlockPos centerPos;
        private final Direction direction;
        private int ticksExisted = 0;
        private final List<BlockPos> savedPositions = new ArrayList<>();
        private final List<BlockState> savedStates = new ArrayList<>();

        public ActiveWall(Level level, BlockPos centerPos, Direction direction) {
            this.level = level;
            this.centerPos = centerPos;
            this.direction = direction;
            spawnRow(0);
        }

        private void spawnRow(int row) {
            Direction widthDir = direction.getClockWise();
            for (int w = -2; w <= 2; w++) {
                BlockPos pos = centerPos.relative(widthDir, w).above(row);
                if (level.getBlockState(pos).isAir() || level.getBlockState(pos).getDestroySpeed(level, pos) >= 0) {
                    savedPositions.add(pos);
                    savedStates.add(level.getBlockState(pos));
                    level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                    // Убраны все sendParticles, оставлен только чистый звук
                    level.playSound(null, pos, SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
        }

        public boolean tick() {
            ticksExisted++;
            if (ticksExisted == 10) spawnRow(1);
            if (ticksExisted == 20) spawnRow(2);
            if (ticksExisted >= 220) {
                restoreWorld();
                return true;
            }
            return false;
        }

        private void restoreWorld() {
            for (int i = 0; i < savedPositions.size(); i++) {
                BlockPos pos = savedPositions.get(i);
                if (level.getBlockState(pos).getBlock() == Blocks.DIRT) {
                    level.setBlockAndUpdate(pos, savedStates.get(i));
                    // Убраны все sendParticles, оставлен только чистый звук
                    level.playSound(null, pos, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
        }
    }
}
