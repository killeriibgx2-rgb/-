package com.zzynes.fly_drone;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FlyDroneMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getCrafting().is(ModItems.COPPER_WIRE.get())) {
            CraftingContainer container = (CraftingContainer) event.getInventory();
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (stack.is(ModItems.STAL_MOLOT.get())) {
                    int currentDamage = stack.getDamageValue();
                    stack.setDamageValue(currentDamage + 5);
                    if (stack.getDamageValue() >= stack.getMaxDamage()) {
                        container.setItem(i, ItemStack.EMPTY);
                    }
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerUpdate(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.getCamera() instanceof FPVDroneEntity) {
                player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onMobTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof ServerPlayer player) {
            if (player.getCamera() instanceof FPVDroneEntity) {
                event.setCanceled(true);
            }
        }
    }

    // ИСПРАВЛЕНО: Безопасное событие поворота камеры теперь живёт здесь и работает на стороне клиента!
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.getCameraEntity() instanceof FPVDroneEntity drone) {
                event.setRoll(drone.customRoll);
            }
        }
    }
}
