package com.eternalsnow.item;

import com.eternalsnow.registry.ModRegistry;
import com.eternalsnow.state.EternalSnowState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public class SnowShovelItem extends Item {

    public SnowShovelItem(Properties properties) {
        super(properties);
    }

    public static boolean isSnow(BlockState state) {
        return state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW);
    }

    public static boolean isSnowTarget(BlockState state) {
        return isSnow(state);
    }

    public static int clearSnowVolume(ServerLevel level, BlockPos origin, Direction forward, ItemStack shovelStack) {
        return sweepSnow(level, origin, forward, shovelStack);
    }

    public static int getSweepWidth(ItemStack stack) {
        if (stack.isEmpty() || !ModRegistry.SNOW_WIDTH.isPresent()) {
            return 1;
        }
        return 1 + Math.min(2, EnchantmentHelper.getTagEnchantmentLevel(ModRegistry.SNOW_WIDTH.get(), stack));
    }

    public static int getSweepLength(ItemStack stack) {
        if (stack.isEmpty() || !ModRegistry.SNOW_LENGTH.isPresent()) {
            return 1;
        }
        return 1 + Math.min(2, EnchantmentHelper.getTagEnchantmentLevel(ModRegistry.SNOW_LENGTH.get(), stack));
    }

    public static int getSweepHeight(ItemStack stack) {
        if (stack.isEmpty() || !ModRegistry.SNOW_HEIGHT.isPresent()) {
            return 1;
        }
        return 1 + Math.min(2, EnchantmentHelper.getTagEnchantmentLevel(ModRegistry.SNOW_HEIGHT.get(), stack));
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return Math.max(64, EternalSnowState.getEffectiveSettings().shovelDurability);
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.COPPER_INGOT) || repair.is(Items.IRON_INGOT) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return isSnow(state) ? 28.0F : 0.0F;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return isSnow(state);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return isSnow(state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel && isSnow(state)) {
            Direction facing = entity.getDirection();
            if (facing.getAxis().isVertical()) {
                facing = Direction.NORTH;
            }
            int cleared = sweepSnow(serverLevel, pos, facing, stack);
            if (cleared > 0) {
                stack.hurtAndBreak(1, entity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            }
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState state = level.getBlockState(clicked);
        if (!isSnow(state)) {
            BlockPos above = clicked.above();
            if (isSnow(level.getBlockState(above))) {
                clicked = above;
            } else {
                return InteractionResult.PASS;
            }
        }

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            Direction facing = context.getHorizontalDirection();
            ItemStack stack = context.getItemInHand();
            int cleared = sweepSnow(serverLevel, clicked, facing, stack);
            if (cleared > 0) {
                serverLevel.playSound(null, clicked, SoundEvents.SNOW_BREAK, SoundSource.BLOCKS, 0.85F, 1.05F);
                Player player = context.getPlayer();
                if (player != null) {
                    stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(context.getHand()));
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Sweeps strictly snow blocks in a (width x length x height) box forward and upward without dropping item clutter.
     */
    public static int sweepSnow(ServerLevel level, BlockPos origin, Direction forward, ItemStack shovelStack) {
        if (forward.getAxis().isVertical()) {
            forward = Direction.NORTH;
        }
        Direction right = forward.getClockWise();

        int width = getSweepWidth(shovelStack);
        int length = getSweepLength(shovelStack);
        int height = getSweepHeight(shovelStack);

        int latMin = (width == 3) ? -1 : 0;
        int latMax = (width == 1) ? 0 : 1;

        int cleared = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int f = 0; f < length; f++) {
            for (int w = latMin; w <= latMax; w++) {
                for (int u = 0; u < height; u++) {
                    cursor.set(
                            origin.getX() + forward.getStepX() * f + right.getStepX() * w,
                            origin.getY() + u,
                            origin.getZ() + forward.getStepZ() * f + right.getStepZ() * w
                    );
                    BlockState st = level.getBlockState(cursor);
                    if (isSnow(st)) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        cleared++;
                    }
                }
            }
        }

        if (cleared > 0) {
            level.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    origin.getX() + 0.5D,
                    origin.getY() + 0.4D,
                    origin.getZ() + 0.5D,
                    Math.min(12, cleared * 2),
                    0.35D, 0.25D, 0.35D, 0.04D
            );
        }
        return cleared;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int w = getSweepWidth(stack);
        int l = getSweepLength(stack);
        int h = getSweepHeight(stack);
        tooltip.add(Component.translatable("eternalsnow.shovel.tooltip.size", w, l, h).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("eternalsnow.shovel.tooltip.safe").withStyle(ChatFormatting.GRAY));
    }
}
