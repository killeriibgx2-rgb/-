package com.eternalsnow.thermal.world;

import com.eternalsnow.thermal.engine.BodyModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Server-side body state, saved with the player and advanced only while they are online. */
public final class PlayerBodySimulator {
    private static final String CORE_KEY = "eternalsnow.bodyCoreCelsius";

    private PlayerBodySimulator() {
    }

    public record BodyState(double coreCelsius, double clothingClo) {
    }

    public static BodyState advance(ServerPlayer player, double ambientCelsius, double modelSeconds) {
        double core = coreCelsius(player);
        double clo = clothingClo(player);

        // Multiplier zero is a true pause; do not run a zero-duration Euler step.
        if (modelSeconds > 0) {
            BodyModel body = new BodyModel();
            body.setCoreCelsius(core);
            core = body.step(ambientCelsius, clo, modelSeconds, BodyModel.RESTING_WATTS);
            setCoreCelsius(player, core);
            applyLethalThreshold(player, core);
        }
        return new BodyState(core, clo);
    }

    public static double coreCelsius(Player player) {
        CompoundTag persisted = persisted(player);
        if (!persisted.contains(CORE_KEY)) return BodyModel.COMFORT_CELSIUS;
        double value = persisted.getDouble(CORE_KEY);
        return Double.isFinite(value) ? value : BodyModel.COMFORT_CELSIUS;
    }

    public static void setCoreCelsius(Player player, double celsius) {
        if (!Double.isFinite(celsius)) throw new IllegalArgumentException("body temperature must be finite");
        persisted(player).putDouble(CORE_KEY, celsius);
    }

    public static void reset(Player player) {
        persisted(player).remove(CORE_KEY);
    }

    /** Sum each worn item’s explicit datapack clo value; unequipped items do nothing. */
    public static double clothingClo(Player player) {
        double sum = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            // One item belongs in one value tag. The order is deliberately high-to-low
            // so accidental overlapping datapack tags cannot double its insulation.
            if (stack.is(ThermalClothingTags.CLO_300)) sum += 3.0;
            else if (stack.is(ThermalClothingTags.CLO_200)) sum += 2.0;
            else if (stack.is(ThermalClothingTags.CLO_100)) sum += 1.0;
            else if (stack.is(ThermalClothingTags.CLO_050)) sum += 0.5;
            else if (stack.is(ThermalClothingTags.CLO_035)) sum += 0.35;
            else if (stack.is(ThermalClothingTags.CLO_025)) sum += 0.25;
            else if (stack.is(ThermalClothingTags.CLO_005)) sum += 0.05;
        }
        return sum;
    }

    private static CompoundTag persisted(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static void applyLethalThreshold(ServerPlayer player, double coreCelsius) {
        if (player.isCreative() || player.isSpectator() || !player.isAlive()) return;
        Level level = player.level();
        if (coreCelsius <= BodyModel.DEATH_CELSIUS) {
            player.hurt(level.damageSources().freeze(), 1000.0F);
        } else if (coreCelsius >= BodyModel.HEAT_DEATH_CELSIUS) {
            player.hurt(level.damageSources().onFire(), 1000.0F);
        }
    }
}
