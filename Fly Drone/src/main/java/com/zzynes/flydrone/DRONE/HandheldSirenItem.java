package com.zzynes.flydrone.DRONE;

import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class HandheldSirenItem extends Item implements GeoItem {
    private static final RawAnimation ACTIVE_ANIMATION = RawAnimation.begin().thenLoop("animation");
    private static boolean animationActive = false;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HandheldSirenItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 1200;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);

        if (level.isClientSide()) {
            animationActive = true;
        } else {
            playSiren(level, player);
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (level.isClientSide()) {
            animationActive = false;
        }

        if (!level.isClientSide() && entity instanceof Player player) {
            stopSiren(level, player);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide()) {
            animationActive = false;
        }

        if (!level.isClientSide() && entity instanceof Player player) {
            stopSiren(level, player);
        }

        return stack;
    }

    private void playSiren(Level level, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.AIR_SIREN.get(),
                SoundSource.PLAYERS,
                9.4F,
                1.0F
        );
    }

    private void stopSiren(Level level, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        ResourceLocation soundId = new ResourceLocation(FlyDroneMod.MOD_ID, "item.air_siren");

        for (ServerPlayer target : serverLevel.players()) {
            if (target.distanceToSqr(player) <= 22500.0D) {
                target.connection.send(new ClientboundStopSoundPacket(soundId, SoundSource.PLAYERS));
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (animationActive) {
                return state.setAndContinue(ACTIVE_ANIMATION);
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> consumer.accept(HandheldSirenRenderer.EXTENSIONS));
    }
}