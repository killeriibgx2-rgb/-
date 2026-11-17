package com.eternalsnow.food;

import com.eternalsnow.state.EternalSnowState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Item-only restrictions: never cancel block interactions such as storage or shelves. */
@Mod.EventBusSubscriber(modid = "eternalsnow")
public final class FrozenItemRules {
    private FrozenItemRules() {}

    public static boolean enabled(Level level) {
        var settings = level.isClientSide ? EternalSnowState.getClientSyncedSettings()
                : EternalSnowState.getServerSettings();
        return settings.enabled && settings.enableFoodFreezing;
    }

    @SubscribeEvent
    public static void useInAir(PlayerInteractEvent.RightClickItem event) {
        if (enabled(event.getLevel()) && FoodProfiles.canFreeze(event.getItemStack())
                && FoodState.isFrozen(event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }
}
