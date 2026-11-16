package com.eternalsnow.block;

import com.eternalsnow.network.ClientboundOpenChimneyScreenPacket;
import com.eternalsnow.network.PacketHandler;
import com.eternalsnow.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Brick Chimney block (chimney_bricks.png sides, chimney_top.png flue top).
 * - Connects vertically on top of the 3D Fireplace 'connect' socket up to open sky.
 * - Right-clicking ANY block of the chimney stack opens the Chimney Damper Screen (0%..100% draft slider)!
 */
public final class ChimneyBlock extends Block {
    public ChimneyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        ItemStack held = player.getItemInHand(hand);

        // Allow seamless vertical stacking when clicking UP/DOWN with another Chimney block in hand!
        if (ModRegistry.CHIMNEY_ITEM.isPresent()
                && held.is(ModRegistry.CHIMNEY_ITEM.get())
                && (hit.getDirection() == Direction.UP || hit.getDirection() == Direction.DOWN)) {
            BlockPos placeTarget = pos.relative(hit.getDirection());
            if (level.getBlockState(placeTarget).canBeReplaced()) {
                return InteractionResult.PASS;
            }
        }

        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            BlockPos masterPos = FireplaceBlockEntity.findConnectedFireplaceMaster(level, pos);
            if (masterPos == null || !(level.getBlockEntity(masterPos) instanceof FireplaceBlockEntity be)) {
                sp.displayClientMessage(Component.translatable("eternalsnow.msg.chimney.not_connected"), true);
                return InteractionResult.CONSUME;
            }
            Direction facing = be.getBlockState().getValue(FireplaceBlock.FACING);
            boolean skyValid = (FireplaceBlockEntity.findValidChimneyFlueExit(level, masterPos, facing) != null);
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.55F, 0.9F);
            PacketHandler.sendToPlayer(sp, new ClientboundOpenChimneyScreenPacket(
                    masterPos,
                    be.getDamperPercent(),
                    be.getFuelMilli(),
                    be.isLit(),
                    skyValid
            ));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("eternalsnow.tooltip.chimney.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("eternalsnow.tooltip.chimney.damper_hint").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("eternalsnow.tooltip.chimney.craft").withStyle(ChatFormatting.DARK_GRAY));
    }
}
