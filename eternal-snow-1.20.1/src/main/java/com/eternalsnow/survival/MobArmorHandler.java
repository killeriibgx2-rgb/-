package com.eternalsnow.survival;

import com.eternalsnow.state.EternalSnowState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * v2.6.6 — "все могут быть в броне": armour is no longer a player-only privilege.
 *
 * Right-click a mob with any armour piece and it puts it on (zombies, skeletons, villagers, pigs —
 * whoever vanilla allows to wear that slot). Shift-right-click with an empty hand takes it back off.
 * The equipped wool/jacket then feeds the same insulation maths as for the player, so a dressed
 * zombie really does freeze five times slower.
 */
public final class MobArmorHandler {

    private MobArmorHandler() {
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Mob mob) || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        if (!EternalSnowState.getEffectiveSettings().enabled) {
            return;
        }
        ItemStack held = event.getItemStack();

        if (player.isShiftKeyDown() && held.isEmpty()) {
            // strip armour: top-most non-empty slot first
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack worn = mob.getItemBySlot(slot);
                if (!worn.isEmpty()) {
                    if (!event.getLevel().isClientSide) {
                        mob.setItemSlot(slot, ItemStack.EMPTY);
                        if (!player.getInventory().add(worn)) {
                            player.drop(worn, false);
                        }
                        mob.playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 0.7F, 1.0F);
                    }
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return;
                }
            }
            return;
        }

        if (!(held.getItem() instanceof ArmorItem armor) || held.isEmpty()) {
            return;
        }
        EquipmentSlot slot = armor.getEquipmentSlot();
        if (!mob.getItemBySlot(slot).isEmpty()) {
            return; // slot busy — vanilla/other mods may handle it
        }
        if (event.getLevel().isClientSide) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        ItemStack toWear = held.copy();
        toWear.setCount(1);
        mob.setItemSlot(slot, toWear);
        mob.setPersistenceRequired();
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        mob.playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 0.8F, 1.0F);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
