package net.wizard.elemental_awakening.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.wizard.elemental_awakening.mana.PlayerManaProvider;

import java.util.ArrayList;
import java.util.List;

public class EarthStaffItem extends Item {
    // Список для отслеживания всех активных стен в мире, чтобы обновлять их рост и исчезновение
    public static final List<ActiveWall> ACTIVE_WALLS = new ArrayList<>();

    public EarthStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // Shift + ПКМ — открываем меню Земли
        if (level.isClientSide()) {
            if (player.isCrouching()) {
                net.minecraft.client.Minecraft.getInstance().setScreen(new net.wizard.elemental_awakening.client.EarthMenuScreen());
                return InteractionResultHolder.success(itemstack);
            }
        }

        // Обычный ПКМ — призываем огромную стену 5х3 на СЕРВЕРЕ
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            if (!player.isCrouching()) {
                player.getCapability(PlayerManaProvider.PLAYER_MANA).ifPresent(mana -> {
                    if (mana.getMana() >= 10.0F) {
                        mana.consumeMana(10.0F); // Списываем 10 маны

                        Direction direction = player.getDirection();
                        // Спавним строго на 1 блок впереди игрока
                        BlockPos startPos = player.blockPosition().relative(direction, 1);
                        Direction clockwise = direction.getClockWise();

                        // Создаем новую структуру для управления нашей стеной
                        ActiveWall wall = new ActiveWall(serverLevel);

                        // Высчитываем координаты для 15 блоков (5 в ширину, 3 в высоту)
                        // Сдвигаем на 2 блока влево, чтобы центр стены был ровно перед глазами игрока
                        BlockPos centerLeft = startPos.relative(clockwise.getOpposite(), 2);

                        for (int h = 0; h < 3; h++) { // 3 ряда в высоту
                            for (int w = 0; w < 5; w++) { // 5 блоков в ширину
                                BlockPos blockPos = centerLeft.relative(clockwise, w).above(h);
                                wall.addBlockPlaceholder(h, blockPos);
                            }
                        }

                        // Строим первый нижний ряд мгновенно
                        wall.spawnRow(0);

                        // Добавляем стену в глобальный список, чтобы класс событий ModEvents крутил её таймеры
                        synchronized (ACTIVE_WALLS) {
                            ACTIVE_WALLS.add(wall);
                        }

                        player.sendSystemMessage(Component.literal("Магия Земли: Барьер Скаута V уровня возведен!"));
                    } else {
                        player.sendSystemMessage(Component.literal("Недостаточно маны для Барьера Скаута!"));
                    }
                });
                return InteractionResultHolder.success(itemstack);
            }
        }

        return InteractionResultHolder.pass(itemstack);
    }

    // Класс-помощник, который управляет одной конкретной стеной в мире
    public static class ActiveWall {
        private final ServerLevel level;
        private final List<BlockPos> row0 = new ArrayList<>();
        private final List<BlockPos> row1 = new ArrayList<>();
        private final List<BlockPos> row2 = new ArrayList<>();

        // Списки для хранения оригинальных блоков, чтобы вернуть их назад через 10 секунд!
        private final List<BlockPos> savedPositions = new ArrayList<>();
        private final List<BlockState> savedStates = new ArrayList<>();

        private int ticksExisted = 0;
        private boolean isDone = false;

        public ActiveWall(ServerLevel level) {
            this.level = level;
        }

        public void addBlockPlaceholder(int heightRow, BlockPos pos) {
            if (heightRow == 0) row0.add(pos);
            else if (heightRow == 1) row1.add(pos);
            else if (heightRow == 2) row2.add(pos);
        }

        // Обновление тиков стены (вызывается из ModEvents)
        public void tick() {
            ticksExisted++;

            // Через 0.5 сек (10 тиков) выращиваем второй ряд
            if (ticksExisted == 10) {
                spawnRow(1);
            }
            // Через 1.0 сек (20 тиков) выращиваем третий ряд
            if (ticksExisted == 20) {
                spawnRow(2);
            }
            // Через 11.0 сек (220 тиков = 1 секунда роста + 10 секунд стояния) полностью восстанавливаем мир!
            if (ticksExisted >= 220) {
                restoreWorld();
                isDone = true;
            }
        }

        public boolean isFinished() {
            return isDone;
        }

        // Спавн конкретного ряда блоков со звуками, эффектами летящей грязи и ЗАПОМИНАНИЕМ старых блоков
        public void spawnRow(int heightRow) {
            List<BlockPos> targets = heightRow == 0 ? row0 : (heightRow == 1 ? row1 : row2);

            for (BlockPos pos : targets) {
                // Запоминаем текущий оригинальный блок на этом месте перед тем, как перезаписать его грязью!
                BlockState originalState = level.getBlockState(pos);
                savedPositions.add(pos);
                savedStates.add(originalState);

                // Ставим блок земли
                level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());

                // Воспроизводим звук копания/установки земли
                level.playSound(null, pos, SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);

                // Спавним 10 сочных частиц летящей во все стороны грязи
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.1);
            }
        }

        // Волшебное исчезновение стены с полным восстановлением уничтоженных блоков!
        public void restoreWorld() {
            for (int i = 0; i < savedPositions.size(); i++) {
                BlockPos pos = savedPositions.get(i);
                BlockState originalState = savedStates.get(i);

                // Если там всё еще стоит наша грязь — возвращаем оригинальный блок обратно
                if (level.getBlockState(pos).is(Blocks.DIRT)) {
                    level.setBlockAndUpdate(pos, originalState);

                    // Звук рассыпающейся магии
                    level.playSound(null, pos, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.0F, 0.5F);

                    // Эффектные частицы взрыва разрушения земли
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.3, 0.3, 0.3, 0.15);
                }
            }
        }
    }
}
