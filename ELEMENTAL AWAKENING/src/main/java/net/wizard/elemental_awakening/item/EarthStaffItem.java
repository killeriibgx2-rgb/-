package net.wizard.elemental_awakening.item;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.wizard.elemental_awakening.client.EarthMenuScreen;
import net.wizard.elemental_awakening.client.FireMenuScreen;
import net.wizard.elemental_awakening.client.WaterMenuScreen;
import net.wizard.elemental_awakening.client.AirMenuScreen;
import net.wizard.elemental_awakening.mana.PlayerManaProvider;
import net.wizard.elemental_awakening.networking.ModMessages;
import net.wizard.elemental_awakening.networking.OpenElementMenuPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

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

        // --- КЛИЕНТСКАЯ СТОРОНА: АВТОМАТИЧЕСКОЕ ОТКРЫТИЕ МЕНЮ ВЫБРАННОЙ СТИХИИ (SHIFT + ПКМ) ---
        if (level.isClientSide) {
            if (player.isCrouching()) {
                player.getCapability(PlayerManaProvider.PLAYER_MANA).ifPresent(mana -> {
                    int element = mana.getElement();
                    // Палочка сама смотрит на ID стихии и открывает нужное меню 512х512
                    if (element == 1) {
                        Minecraft.getInstance().setScreen(new EarthMenuScreen()); // Земля
                    } else if (element == 2) {
                        Minecraft.getInstance().setScreen(new FireMenuScreen());  // Огонь (пустое)
                    } else if (element == 3) {
                        Minecraft.getInstance().setScreen(new WaterMenuScreen()); // Вода (пустое)
                    } else if (element == 4) {
                        Minecraft.getInstance().setScreen(new AirMenuScreen());   // Воздух (пустое)
                    }
                });
                return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
            }
        }

        // --- СЕРВЕРНАЯ СТОРОНА: ПРОВЕРКА ПЕРВОГО ВХОДА И КАСТ МАГИИ ---
        if (!level.isClientSide) {
            // Если стихия еще не выбрана (0), отправляем пакет на открытие Главного Меню выбора
            if (player.isCrouching() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.getCapability(PlayerManaProvider.PLAYER_MANA).ifPresent(mana -> {
                    if (mana.getElement() == 0) {
                        ModMessages.sendToPlayer(new OpenElementMenuPacket(), serverPlayer);
                    }
                });
                return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
            }

            // ОБЫЧНЫЙ ПКМ: СРАБОТАЕТ ТОЛЬКО ДЛЯ МАГИИ ЗЕМЛИ
            player.getCapability(PlayerManaProvider.PLAYER_MANA).ifPresent(mana -> {
                int element = mana.getElement();

                // 🟫 ЕСЛИ ВЫБРАНА ЗЕМЛЯ (1) — СТРОИМ СТЕНУ
                if (element == 1) {
                    if (mana.getMana() >= 10.0F) {
                        mana.consumeMana(10.0F); // Тратим 10 маны

                        Direction direction = player.getDirection();
                        BlockPos startPos = player.blockPosition().relative(direction, 1);

                        // Создаем и запускаем стену перед игроком
                        ActiveWall wall = new ActiveWall(level, startPos, direction);
                        ACTIVE_WALLS.add(wall);
                    }
                }
                // Для Огня (2), Воды (3) и Воздуха (4) здесь пока пусто, палочка ничего не делает
            });
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    // --- ВНУТРЕННИЙ КЛАСС СТЕНЫ ЗЕМЛИ ---
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

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GRAVEL.defaultBlockState()),
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                10, 0.2, 0.2, 0.2, 0.1);
                    }
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

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                15, 0.2, 0.2, 0.2, 0.1);
                    }
                    level.playSound(null, pos, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
        }
    }
}
