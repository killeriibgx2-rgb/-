package com.eternalsnow.item;

import com.eternalsnow.block.FireplaceBlock;
import com.eternalsnow.block.FireplaceBlockEntity;
import com.eternalsnow.block.FireplacePartBlock;
import com.eternalsnow.world.CampfireFuelManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Day-1 Wooden Fire-Starter (Самодельное деревянное огниво):
 * - Hold Right-Click (ПКМ) for 10.0 seconds (200 ticks) with a live HUD friction bar.
 * - Upon completing 10.0 seconds, rolls an exact 35% (0.35F) chance to spark fire!
 */
public final class WoodenFirestarterItem extends Item {
    public static final int USE_DURATION_TICKS = 200; // Exact 10.0 seconds at 20 TPS
    public static final float SPARK_SUCCESS_CHANCE = 0.35F; // Exact 35% chance

    public WoodenFirestarterItem(Properties properties) {
        super(properties);
    }

    public static boolean isInstantIgniter(ItemStack stack) {
        return stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE);
    }

    public static boolean isAnyIgniter(ItemStack stack) {
        return isInstantIgniter(stack) || stack.getItem() instanceof WoodenFirestarterItem;
    }

    public static boolean tryConsumeInstantIgniter(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        if (stack.is(Items.FLINT_AND_STEEL)) {
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            }
            return true;
        }
        if (stack.is(Items.FIRE_CHARGE)) {
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return true;
        }
        return false;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        // Return PASS so Item#use(...) starts the 10-second continuous hold cleanly!
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(5.0D, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult bhr)) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos hitPos = bhr.getBlockPos();
        BlockState hitState = level.getBlockState(hitPos);

        // 1. Validate 3D Fireplace target BEFORE starting the 10-second rub
        BlockPos fpMaster = resolveFireplaceMaster(hitPos, hitState);
        if (fpMaster != null) {
            BlockState ms = level.getBlockState(fpMaster);
            if (!(ms.getBlock() instanceof FireplaceBlock) || ms.getValue(FireplaceBlock.LIT)) {
                return InteractionResultHolder.pass(stack);
            }
            if (level.getBlockEntity(fpMaster) instanceof FireplaceBlockEntity be) {
                if (!be.hasFuel()) {
                    if (!level.isClientSide) {
                        player.displayClientMessage(Component.translatable("eternalsnow.msg.fireplace_no_fuel"), true);
                    }
                    return InteractionResultHolder.fail(stack);
                }
                Direction facing = ms.getValue(FireplaceBlock.FACING);
                if (!level.isClientSide && FireplaceBlockEntity.findValidChimneyFlueExit(level, fpMaster, facing) == null) {
                    player.displayClientMessage(Component.translatable("eternalsnow.msg.fireplace_no_chimney"), true);
                    return InteractionResultHolder.fail(stack);
                }
                if (be.getDamperPercent() <= 0) {
                    if (!level.isClientSide) {
                        player.displayClientMessage(Component.translatable("eternalsnow.msg.fireplace.damper_closed"), true);
                    }
                    return InteractionResultHolder.fail(stack);
                }
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        // 2. Validate Stick Campfire target BEFORE starting the 10-second rub
        if (hitState.getBlock() instanceof CampfireBlock) {
            if (hitState.getValue(CampfireBlock.LIT) || hitState.getValue(CampfireBlock.WATERLOGGED)) {
                return InteractionResultHolder.pass(stack);
            }
            if (!level.isClientSide && level instanceof ServerLevel sl) {
                if (CampfireFuelManager.getFuelTicks(sl, hitPos, hitState) <= 0) {
                    player.displayClientMessage(Component.translatable("eternalsnow.msg.campfire_no_sticks"), true);
                    return InteractionResultHolder.fail(stack);
                }
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        // 3. Validate ordinary ground block for BaseFireBlock
        BlockPos firePos = hitPos.relative(bhr.getDirection());
        if (BaseFireBlock.canBePlacedAt(level, firePos, player.getDirection())) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }
        HitResult hit = player.pick(5.5D, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult bhr)) {
            player.stopUsingItem();
            return;
        }

        int elapsed = USE_DURATION_TICKS - remainingUseDuration;
        BlockPos pos = bhr.getBlockPos();

        // Friction audio & rising smoke/embers every 10 ticks while rubbing sticks!
        if (elapsed > 0 && elapsed % 10 == 0) {
            float progress = elapsed / (float) USE_DURATION_TICKS;
            if (!level.isClientSide) {
                level.playSound(null, pos, SoundEvents.WOOD_STEP, SoundSource.PLAYERS,
                        0.45F + progress * 0.35F, 1.15F + progress * 0.55F);
                if (level instanceof ServerLevel sl) {
                    double px = pos.getX() + 0.5D;
                    double py = pos.getY() + 0.45D;
                    double pz = pos.getZ() + 0.5D;
                    sl.sendParticles(ParticleTypes.SMOKE, px, py, pz, 2 + (int) (progress * 4), 0.14D, 0.1D, 0.14D, 0.01D);
                    if (progress >= 0.65F) {
                        sl.sendParticles(ParticleTypes.FLAME, px, py, pz, 1, 0.1D, 0.08D, 0.1D, 0.01D);
                    }
                }
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (level.isClientSide || !(level instanceof ServerLevel sl) || !(livingEntity instanceof ServerPlayer player)) {
            return stack;
        }

        InteractionHand hand = player.getUsedItemHand();
        HitResult hit = player.pick(5.5D, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult bhr)) {
            return stack;
        }

        BlockPos hitPos = bhr.getBlockPos();
        BlockState hitState = sl.getBlockState(hitPos);

        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }

        // Roll exact 35% spark success chance after holding 10 full seconds!
        boolean sparked = sl.random.nextFloat() < SPARK_SUCCESS_CHANCE;
        if (!sparked) {
            sl.playSound(null, hitPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.65F, 1.35F);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                    hitPos.getX() + 0.5D, hitPos.getY() + 0.45D, hitPos.getZ() + 0.5D,
                    7, 0.18D, 0.12D, 0.18D, 0.02D);
            player.displayClientMessage(Component.translatable("eternalsnow.msg.firestarter_miss"), true);
            return stack;
        }

        // 35% Spark SUCCEEDED -> Ignite Fireplace, Campfire, or Ground Fire!
        BlockPos fpMaster = resolveFireplaceMaster(hitPos, hitState);
        if (fpMaster != null && sl.getBlockEntity(fpMaster) instanceof FireplaceBlockEntity be) {
            BlockState ms = sl.getBlockState(fpMaster);
            if (ms.getBlock() instanceof FireplaceBlock && !ms.getValue(FireplaceBlock.LIT)) {
                Direction facing = ms.getValue(FireplaceBlock.FACING);
                if (!be.hasFuel()) {
                    player.displayClientMessage(Component.translatable("eternalsnow.msg.fireplace_no_fuel"), true);
                    return stack;
                }
                if (FireplaceBlockEntity.findValidChimneyFlueExit(sl, fpMaster, facing) == null) {
                    player.displayClientMessage(Component.translatable("eternalsnow.msg.fireplace_no_chimney"), true);
                    return stack;
                }
                if (be.getDamperPercent() <= 0) {
                    player.displayClientMessage(Component.translatable("eternalsnow.msg.fireplace.damper_closed"), true);
                    return stack;
                }
                be.setLitState(true);
                sl.playSound(null, fpMaster, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 0.95F);
                sl.playSound(null, fpMaster, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.85F, 1.05F);
                sl.sendParticles(ParticleTypes.FLAME,
                        fpMaster.getX() + 0.5D, fpMaster.getY() + 0.45D, fpMaster.getZ() + 0.5D,
                        14, 0.25D, 0.18D, 0.25D, 0.03D);
            }
            return stack;
        }

        if (hitState.getBlock() instanceof CampfireBlock) {
            CampfireFuelManager.tryIgniteFromFirestarterSuccess(sl, hitPos, hitState, player);
            return stack;
        }

        BlockPos firePos = hitPos.relative(bhr.getDirection());
        if (BaseFireBlock.canBePlacedAt(sl, firePos, player.getDirection())) {
            sl.playSound(null, firePos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 0.95F);
            sl.setBlock(firePos, BaseFireBlock.getState(sl, firePos), 11);
        }

        return stack;
    }

    @Nullable
    private static BlockPos resolveFireplaceMaster(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof FireplaceBlock) {
            return pos;
        }
        if (state.getBlock() instanceof FireplacePartBlock) {
            return FireplacePartBlock.getMasterPos(pos, state);
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("eternalsnow.tooltip.firestarter.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("eternalsnow.tooltip.firestarter.craft").withStyle(ChatFormatting.DARK_GRAY));
    }
}
