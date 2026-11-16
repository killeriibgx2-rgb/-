package com.eternalsnow.survival;

import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.item.CopperThermosItem;
import com.eternalsnow.item.HeatScannerItem;
import com.eternalsnow.item.HotDrinkCupItem;
import com.eternalsnow.network.ClientboundHeatGridPacket;
import com.eternalsnow.network.ClientboundPlayerSurvivalPacket;
import com.eternalsnow.network.PacketHandler;
import com.eternalsnow.registry.ModRegistry;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.thermal.ThermalEngine;
import com.eternalsnow.weather.WeatherEventType;
import com.eternalsnow.world.CampfireFuelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public final class PlayerSurvivalManager {
    private static final String NBT_ROOT = "EternalSnowSurvival";
    private static final String TAG_BODY_TEMP = "BodyTempState";
    private static final String TAG_THIRST = "ThirstLevel";
    private static final String TAG_THIRST_SAT = "ThirstSat";
    private static final String TAG_THIRST_EXH = "ThirstExh";

    private static final UUID COLD_SLOW_MODIFIER_UUID = UUID.fromString("92b6c4a8-51e3-4c9a-9f14-7183c0d12301");

    private PlayerSurvivalManager() {
    }

    /**
     * v2.6.1: survival state lives UNDER Forge's PlayerPersisted tag and is written back after
     * every mutation. Before this, getCompound() returned a detached copy, so body temperature and
     * thirst were silently reset on world re-entry / respawn (and on dedicated servers).
     */
    private static CompoundTag getOrCreateTag(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag persisted = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(NBT_ROOT, CompoundTag.TAG_COMPOUND)) {
            CompoundTag tag = new CompoundTag();
            tag.putFloat(TAG_BODY_TEMP, 0.0F);
            tag.putInt(TAG_THIRST, 20);
            tag.putFloat(TAG_THIRST_SAT, 5.0F);
            tag.putFloat(TAG_THIRST_EXH, 0.0F);
            persisted.put(NBT_ROOT, tag);
            persistent.put(Player.PERSISTED_NBT_TAG, persisted);
            return tag;
        }
        return persisted.getCompound(NBT_ROOT);
    }

    private static void storeTag(ServerPlayer player, CompoundTag tag) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag persisted = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(NBT_ROOT, tag.copy());
        persistent.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static void resetOnRespawn(ServerPlayer newPlayer) {
        CompoundTag tag = getOrCreateTag(newPlayer);
        tag.putFloat(TAG_BODY_TEMP, 0.0F);
        tag.putInt(TAG_THIRST, 20);
        tag.putFloat(TAG_THIRST_SAT, 5.0F);
        tag.putFloat(TAG_THIRST_EXH, 0.0F);
        storeTag(newPlayer, tag);
        updateSmoothColdSlowdown(newPlayer, 0.0F);
    }

    public static void addThirstDirect(ServerPlayer player, int addThirst, float addSat) {
        CompoundTag tag = getOrCreateTag(player);
        int thirst = Mth.clamp(tag.getInt(TAG_THIRST) + addThirst, 0, 20);
        float sat = Mth.clamp(tag.getFloat(TAG_THIRST_SAT) + addSat, 0.0F, 10.0F);
        tag.putInt(TAG_THIRST, thirst);
        tag.putFloat(TAG_THIRST_SAT, sat);
        storeTag(player, tag);
    }

    private static void updateSmoothColdSlowdown(ServerPlayer player, float slowFraction) {
        AttributeInstance moveAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveAttr == null) {
            return;
        }
        double targetAmount = -Mth.clamp(slowFraction, 0.0F, 0.42F);
        AttributeModifier existing = moveAttr.getModifier(COLD_SLOW_MODIFIER_UUID);
        if (targetAmount >= -0.01D) {
            if (existing != null) {
                moveAttr.removeModifier(COLD_SLOW_MODIFIER_UUID);
            }
            return;
        }
        if (existing != null && Math.abs(existing.getAmount() - targetAmount) < 0.025D) {
            return;
        }
        if (existing != null) {
            moveAttr.removeModifier(COLD_SLOW_MODIFIER_UUID);
        }
        moveAttr.addTransientModifier(new AttributeModifier(
                COLD_SLOW_MODIFIER_UUID,
                "EternalSnow hypothermia chill",
                targetAmount,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));
    }

    /**
     * А-3: Checks if player has a solid leeward windbreak (cliff/wall/tent) blocking the oncoming wind vector!
     */
    private static boolean hasWindbreakAgainstWind(ServerLevel level, BlockPos samplePos) {
        float wx = EternalSnowState.getWindDirX();
        float wz = EternalSnowState.getWindDirZ();
        for (int step = 1; step <= 2; step++) {
            BlockPos upwind = BlockPos.containing(
                    samplePos.getX() + 0.5D - wx * step,
                    samplePos.getY() + 0.2D,
                    samplePos.getZ() + 0.5D - wz * step
            );
            BlockState st = level.getBlockState(upwind);
            if (!st.getCollisionShape(level, upwind).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static void tickPlayer(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        ServerSnowSettings settings = EternalSnowState.getServerSettings();
        BlockPos samplePos = player.blockPosition().above();
        float localAirTempC = ThermalEngine.getLocalTemperatureC(level, samplePos);
        float deltaHeat = ThermalEngine.getDeltaHeatAt(level, samplePos);

        CompoundTag tag = getOrCreateTag(player);
        float bodyState = tag.getFloat(TAG_BODY_TEMP);
        int thirst = tag.contains(TAG_THIRST) ? tag.getInt(TAG_THIRST) : 20;
        float thirstSat = tag.getFloat(TAG_THIRST_SAT);
        float thirstExh = tag.getFloat(TAG_THIRST_EXH);

        boolean creativeOrSpec = player.isCreative() || player.isSpectator();

        // 1. Player Temperature, Wetness (А-1), Windchill (А-3) & Cozy Hearth Simulation (every 10 ticks)
        if (player.tickCount % 10 == 0) {
            if (!player.isInPowderSnow && player.getTicksFrozen() > 0) {
                player.setTicksFrozen(0);
            }

            if (!settings.enabled || !settings.enablePlayerTemperature || creativeOrSpec) {
                bodyState = Mth.approach(bodyState, 0.0F, 6.0F);
                updateSmoothColdSlowdown(player, 0.0F);
            } else {
                float rateMult = settings.playerTempRateMultiplier;
                boolean inWater = player.isInWaterOrBubble() && !(player.getVehicle() instanceof Boat);

                // А-1: Falling into freezing water soaks clothes (up to 40s); standing near fire dries in ~6s!
                boolean isWet = false;
                if (ModRegistry.WET_EFFECT.isPresent()) {
                    if (inWater && EternalSnowState.isOverworldLike(level) && localAirTempC < 5.0F) {
                        player.addEffect(new MobEffectInstance(ModRegistry.WET_EFFECT.get(), 800, 0, false, true, true));
                        isWet = true;
                    } else if (player.hasEffect(ModRegistry.WET_EFFECT.get())) {
                        isWet = true;
                        if (deltaHeat >= 9.0F || localAirTempC >= 14.0F) {
                            MobEffectInstance cur = player.getEffect(ModRegistry.WET_EFFECT.get());
                            player.removeEffect(ModRegistry.WET_EFFECT.get());
                            if (cur != null && cur.getDuration() > 120) {
                                player.addEffect(new MobEffectInstance(ModRegistry.WET_EFFECT.get(), cur.getDuration() - 120, 0, false, true, true));
                            } else {
                                isWet = false;
                            }
                            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0D, player.getZ(), 2, 0.2D, 0.25D, 0.2D, 0.01D);
                        }
                    }
                }

                boolean hasColdImmunity = ModRegistry.COLD_IMMUNITY.isPresent()
                        && player.hasEffect(ModRegistry.COLD_IMMUNITY.get());
                boolean hasWarmRecovery = ModRegistry.WARM_RECOVERY.isPresent()
                        && player.hasEffect(ModRegistry.WARM_RECOVERY.get());
                boolean hasCozyHearth = ModRegistry.COZY_HEARTH.isPresent()
                        && player.hasEffect(ModRegistry.COZY_HEARTH.get());

                if (inWater && bodyState > 5.0F) {
                    bodyState = Math.max(0.0F, bodyState - 14.0F);
                }

                if (hasWarmRecovery && bodyState < 0.0F) {
                    bodyState = Math.min(0.0F, bodyState + 1.65F);
                }

                if (localAirTempC < 6.0F) {
                    if (hasColdImmunity) {
                        if (!hasWarmRecovery && bodyState < 0.0F) {
                            bodyState = Math.min(0.0F, bodyState + 0.35F);
                        }
                    } else {
                        float coldSeverity = (6.0F - localAirTempC) / 24.0F;
                        if (inWater) {
                            coldSeverity *= 2.4F;
                        } else if (isWet) {
                            coldSeverity *= 1.55F;
                        }

                        // А-3: Windchill when exposed vs protected behind a windbreak wall/cliff!
                        if (level.canSeeSky(samplePos)) {
                            if (!hasWindbreakAgainstWind(level, samplePos)) {
                                WeatherEventType ev = EternalSnowState.getActiveWeatherEvent();
                                float stormBonus = (ev == WeatherEventType.BLIZZARD || ev == WeatherEventType.GALE) ? 0.65F : 0.42F;
                                coldSeverity *= (1.0F + EternalSnowState.windSpeed * stormBonus);
                            }
                        }

                        float armorDrainMult = ArmorInsulationHelper.getColdDrainMultiplier(player, settings.armorInsulationMultiplier);
                        if (isWet) {
                            // Wet clothes lose ~55% of their insulating loft until dried by a fire!
                            armorDrainMult = Mth.lerp(0.55F, armorDrainMult, 1.0F);
                        }
                        if (hasCozyHearth) {
                            // 1-minute Cozy Hearth from 3D Fireplace buffers body warmth outdoors!
                            armorDrainMult *= 0.45F;
                        }

                        float effectiveDrain = coldSeverity * 1.45F * rateMult * armorDrainMult;
                        float targetMin = Mth.clamp(-coldSeverity * 95.0F, -100.0F, 0.0F);
                        if (bodyState > targetMin) {
                            bodyState = Math.max(targetMin, bodyState - effectiveDrain);
                        } else {
                            bodyState = Mth.approach(bodyState, targetMin, 0.6F);
                        }
                    }
                } else if (localAirTempC > 34.0F && !inWater) {
                    float heatSeverity = (localAirTempC - 34.0F) / 36.0F;
                    float targetMax = Mth.clamp(heatSeverity * 105.0F, 0.0F, 100.0F);
                    if (bodyState < targetMax) {
                        bodyState = Math.min(targetMax, bodyState + Math.max(0.7F, heatSeverity * 2.1F) * rateMult);
                    } else {
                        bodyState = Mth.approach(bodyState, targetMax, 1.0F);
                    }
                } else {
                    float recoverySpeed = (localAirTempC >= 15.0F && bodyState < 0.0F) ? 3.2F : 1.8F;
                    bodyState = Mth.approach(bodyState, 0.0F, recoverySpeed);
                }

                bodyState = Mth.clamp(bodyState, -100.0F, 100.0F);

                if (!hasColdImmunity && bodyState <= -42.0F) {
                    float chillProgress = Mth.clamp((-bodyState - 42.0F) / 58.0F, 0.0F, 1.0F);
                    updateSmoothColdSlowdown(player, chillProgress * 0.36F);

                    if (bodyState <= -60.0F) {
                        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0, true, false, true));
                    }
                    if (bodyState <= -65.0F && player.tickCount % 90 == 0 && ModRegistry.FROST_SHIVER.isPresent()) {
                        level.playSound(null, player.blockPosition(), ModRegistry.FROST_SHIVER.get(), SoundSource.PLAYERS, 0.7F, 0.95F + level.random.nextFloat() * 0.15F);
                    }
                    if (bodyState <= -85.0F && player.tickCount % 50 == 0) {
                        player.hurt(level.damageSources().freeze(), 1.0F);
                    }
                } else {
                    updateSmoothColdSlowdown(player, 0.0F);
                }

                if (bodyState >= 60.0F) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false, true));
                    if (bodyState >= 75.0F) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false, true));
                    }
                    if (bodyState >= 88.0F && player.tickCount % 50 == 0) {
                        player.hurt(level.damageSources().onFire(), 1.0F);
                    }
                }
            }
            tag.putFloat(TAG_BODY_TEMP, bodyState);
            storeTag(player, tag);
        }

        // 2. Player Thirst Simulation
        if (player.tickCount % 10 == 0) {
            if (!settings.enabled || !settings.enableThirst || creativeOrSpec) {
                thirst = 20;
                thirstSat = 5.0F;
                thirstExh = 0.0F;
            } else {
                float drain = 0.032F * settings.thirstDrainMultiplier;
                if (player.isSprinting()) {
                    drain += 0.065F * settings.thirstDrainMultiplier;
                }
                if (bodyState >= 30.0F) {
                    float sweatFactor = 1.0F + ((bodyState - 25.0F) / 25.0F);
                    drain *= sweatFactor;
                }

                thirstExh += drain;
                while (thirstExh >= 4.0F) {
                    thirstExh -= 4.0F;
                    if (thirstSat > 0.0F) {
                        thirstSat = Math.max(0.0F, thirstSat - 1.0F);
                    } else if (thirst > 0) {
                        thirst--;
                    }
                }

                if (thirst <= 4) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false, true));
                    if (thirst <= 2) {
                        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0, true, false, true));
                    }
                }
                if (thirst <= 0 && player.tickCount % 60 == 0) {
                    player.hurt(level.damageSources().dryOut(), 1.0F);
                }
            }

            tag.putInt(TAG_THIRST, thirst);
            tag.putFloat(TAG_THIRST_SAT, thirstSat);
            tag.putFloat(TAG_THIRST_EXH, thirstExh);
            storeTag(player, tag);
        }

        // 3. Sync survival + live looked-at Campfire telemetry to client every 10 ticks
        if (player.tickCount % 10 == 0) {
            long lookedCampPos = Long.MIN_VALUE;
            int lookedCampTicks = 0;
            boolean lookedCampWind = false;

            // v2.6.0: dynamic fuel readout only point-blank (2 blocks), not from across the room
            HitResult hit = player.pick(2.0D, 1.0F, false);
            if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult bhr) {
                BlockPos hp = bhr.getBlockPos();
                BlockState hs = level.getBlockState(hp);
                if (hs.getBlock() instanceof CampfireBlock) {
                    lookedCampPos = hp.asLong();
                    lookedCampTicks = CampfireFuelManager.getFuelTicks(level, hp, hs);
                    lookedCampWind = CampfireFuelManager.isExposedToStormGale(level, hp);
                }
            }

            float bodyTempC = 36.6F + (bodyState / 100.0F) * 4.5F;
            PacketHandler.sendToPlayer(player, new ClientboundPlayerSurvivalPacket(
                    localAirTempC,
                    bodyTempC,
                    bodyState,
                    thirst,
                    thirstSat,
                    lookedCampPos,
                    lookedCampTicks,
                    lookedCampWind
            ));

            if (HeatScannerItem.isHoldingScanner(player)) {
                ItemStack scannerStack = HeatScannerItem.getHeldScanner(player);
                int mode = HeatScannerItem.getMode(scannerStack);
                // Air modes refresh once a second; wall/thermal modes (heavier capture) every 2 s.
                int period = mode >= HeatScannerItem.MODE_WALLS ? 40 : 20;
                if (player.tickCount % period == 0) {
                    PacketHandler.sendToPlayer(player,
                            ClientboundHeatGridPacket.captureFor(level, player, mode));
                }
            }
        }
    }

    /**
     * Г-2: Checks if consumed item is warm cooked food (baked potato, cooked meats/fish, pumpkin pie).
     */
    private static boolean isHotCookedSolidFood(ItemStack stack) {
        return stack.is(Items.BAKED_POTATO)
                || stack.is(Items.COOKED_BEEF)
                || stack.is(Items.COOKED_PORKCHOP)
                || stack.is(Items.COOKED_MUTTON)
                || stack.is(Items.COOKED_CHICKEN)
                || stack.is(Items.COOKED_RABBIT)
                || stack.is(Items.COOKED_COD)
                || stack.is(Items.COOKED_SALMON)
                || stack.is(Items.PUMPKIN_PIE);
    }

    public static void onItemConsumed(ServerPlayer player, ItemStack consumedStack) {
        ServerSnowSettings settings = EternalSnowState.getServerSettings();
        if (!settings.enabled || consumedStack.isEmpty()) {
            return;
        }
        // CopperThermosItem handles its own per-sip thirst & warmth inside finishUsingItem
        if (consumedStack.getItem() instanceof CopperThermosItem) {
            return;
        }

        CompoundTag tag = getOrCreateTag(player);
        float bodyState = tag.getFloat(TAG_BODY_TEMP);
        boolean dirty = false;

        // Г-2: Warm cooked foods gently warm the player from the inside!
        if (settings.enablePlayerTemperature && isHotCookedSolidFood(consumedStack)) {
            if (bodyState < 0.0F) {
                bodyState = Math.min(0.0F, bodyState + 12.0F);
                tag.putFloat(TAG_BODY_TEMP, bodyState);
                dirty = true;
            }
            if (ModRegistry.WARM_RECOVERY.isPresent()) {
                player.addEffect(new MobEffectInstance(ModRegistry.WARM_RECOVERY.get(), 140, 0, false, false, true));
            }
        }

        if (!settings.enableThirst) {
            return;
        }

        int addThirst = 0;
        float addSat = 0.0F;
        float tempShift = 0.0F;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(consumedStack.getItem());
        if (itemId != null && settings.customDrinks != null && !settings.customDrinks.isBlank()) {
            String keyStr = itemId.toString();
            String[] entries = settings.customDrinks.split("[,;\\n]");
            for (String raw : entries) {
                String trimmed = raw.trim();
                int eq = trimmed.indexOf('=');
                if (eq > 0) {
                    String k = trimmed.substring(0, eq).trim();
                    if (k.equalsIgnoreCase(keyStr)) {
                        try {
                            addThirst = Mth.clamp(Integer.parseInt(trimmed.substring(eq + 1).trim()), 1, 20);
                            addSat = addThirst * 0.6F;
                        } catch (NumberFormatException ignored) {
                        }
                        break;
                    }
                }
            }
        }

        if (addThirst == 0) {
            if (consumedStack.getItem() instanceof HotDrinkCupItem) {
                addThirst = 7;
                addSat = 5.5F;
            } else if (consumedStack.getItem() instanceof PotionItem) {
                addThirst = 6;
                addSat = 4.5F;
                tempShift = -16.0F;
            } else if (consumedStack.is(Items.MILK_BUCKET)) {
                addThirst = 8;
                addSat = 5.5F;
                tempShift = -10.0F;
            } else if (consumedStack.getItem() instanceof BowlFoodItem
                    || consumedStack.getItem() instanceof SuspiciousStewItem
                    || consumedStack.is(Items.MUSHROOM_STEW)
                    || consumedStack.is(Items.RABBIT_STEW)
                    || consumedStack.is(Items.BEETROOT_SOUP)) {
                addThirst = 5;
                addSat = 4.0F;
                tempShift = +18.0F;
                if (ModRegistry.WARM_RECOVERY.isPresent()) {
                    player.addEffect(new MobEffectInstance(ModRegistry.WARM_RECOVERY.get(), 180, 0, false, false, true));
                }
            } else if (consumedStack.is(Items.HONEY_BOTTLE)) {
                addThirst = 4;
                addSat = 3.0F;
                tempShift = +8.0F;
            } else if (consumedStack.is(Items.MELON_SLICE)) {
                addThirst = 2;
                addSat = 1.5F;
            } else if (consumedStack.is(Items.SWEET_BERRIES) || consumedStack.is(Items.GLOW_BERRIES)) {
                addThirst = 1;
                addSat = 0.8F;
            }
        }

        if (addThirst <= 0 && !dirty) {
            return;
        }

        int thirst = Mth.clamp(tag.getInt(TAG_THIRST) + addThirst, 0, 20);
        float sat = Mth.clamp(tag.getFloat(TAG_THIRST_SAT) + addSat, 0.0F, 10.0F);
        tag.putInt(TAG_THIRST, thirst);
        tag.putFloat(TAG_THIRST_SAT, sat);

        if (tempShift < 0.0F && bodyState > 10.0F) {
            bodyState = Math.max(0.0F, bodyState + tempShift);
            tag.putFloat(TAG_BODY_TEMP, bodyState);
        } else if (tempShift > 0.0F && bodyState < 0.0F) {
            bodyState = Math.min(0.0F, bodyState + tempShift);
            tag.putFloat(TAG_BODY_TEMP, bodyState);
        }
        storeTag(player, tag);
    }
}
