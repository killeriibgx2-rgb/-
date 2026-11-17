package com.eternalsnow;

import com.eternalsnow.client.ClientModInit;
import com.eternalsnow.command.EternalSnowCommands;
import com.eternalsnow.entity.FestiveCopperGolemEntity;
import com.eternalsnow.network.ClientboundSyncSettingsPacket;
import com.eternalsnow.network.ClientboundWeatherSyncPacket;
import com.eternalsnow.network.PacketHandler;
import com.eternalsnow.registry.ModRegistry;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.survival.MobArmorHandler;
import com.eternalsnow.survival.MobSurvivalManager;
import com.eternalsnow.survival.PlayerSurvivalManager;
import com.eternalsnow.thermal.ThermalEngine;
import com.eternalsnow.weather.WeatherEventManager;
import com.eternalsnow.world.CampfireFuelManager;
import com.eternalsnow.world.EternalSnowSavedData;
import com.eternalsnow.world.ModWorldEvents;
import com.eternalsnow.world.SnowAccumulationManager;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.eternalsnow.world.RecipeUnlockHelper;
import com.eternalsnow.client.EternalSnowClientSetup;
import org.slf4j.Logger;

@Mod(EternalSnowMod.MODID)
public class EternalSnowMod {
    public static final String MODID = "eternalsnow";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String STARTER_PILL_GIVEN_TAG = "EternalSnowStarterPillGiven";

    public EternalSnowMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.register(modBus);
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onEntityAttributes);

        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerChangedDimension);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onLevelTick);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(this::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(MobArmorHandler::onEntityInteract);

        MinecraftForge.EVENT_BUS.addListener(ModWorldEvents::onBlockPlaced);
        MinecraftForge.EVENT_BUS.addListener(ModWorldEvents::onBlockBroken);
        MinecraftForge.EVENT_BUS.addListener(ModWorldEvents::onLeftClickBlock);
        MinecraftForge.EVENT_BUS.addListener(ModWorldEvents::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(ModWorldEvents::onCropGrowPre);
        MinecraftForge.EVENT_BUS.addListener(ModWorldEvents::onSaplingGrow);
        MinecraftForge.EVENT_BUS.addListener(ModWorldEvents::onItemUseFinish);
        MinecraftForge.EVENT_BUS.addListener(ModWorldEvents::onPlayerClone);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> EternalSnowClientSetup::init);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientModInit::init);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
    }

    private void onEntityAttributes(final EntityAttributeCreationEvent event) {
        event.put(ModRegistry.FESTIVE_COPPER_GOLEM.get(), FestiveCopperGolemEntity.createAttributes().build());
    }

    private void onRegisterCommands(final RegisterCommandsEvent event) {
        EternalSnowCommands.register(event.getDispatcher());
    }

    private void onServerStarted(final ServerStartedEvent event) {
        EternalSnowState.tagBiomesFromRegistry(event.getServer().registryAccess());
        EternalSnowSavedData data = EternalSnowSavedData.get(event.getServer());
        EternalSnowState.setServerSettings(data.getSettings());
        EternalSnowState.setServerRunning(true);
        LOGGER.info("[EternalSnow] Loaded world settings (v2.6.6): enabled={}", data.getSettings().enabled);
    }

    private void onServerStopped(final ServerStoppedEvent event) {
        EternalSnowState.setServerRunning(false);
        ThermalEngine.clearAll();
        WeatherEventManager.resetClimate();
    }

    private void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SnowAccumulationManager.forgetPlayer(player.getUUID());
        }
    }

    private void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            grantStarterPillOnce(player);
            RecipeUnlockHelper.unlockAll(player);
            EternalSnowSavedData data = EternalSnowSavedData.get(player.server);
            boolean canManage = EternalSnowState.canPlayerManageServer(player);
            PacketHandler.sendToPlayer(player, new ClientboundSyncSettingsPacket(data.getSettings(), canManage));
            sendCurrentWeatherTo(player);
        }
    }

    private static void grantStarterPillOnce(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.getBoolean(STARTER_PILL_GIVEN_TAG)) {
            persisted.putBoolean(STARTER_PILL_GIVEN_TAG, true);
            root.put(Player.PERSISTED_NBT_TAG, persisted);

            ItemStack pill = new ItemStack(ModRegistry.WARMING_PILL.get(), 1);
            if (!player.getInventory().add(pill)) {
                player.drop(pill, false);
            }
        }
    }

    private static void sendCurrentWeatherTo(ServerPlayer player) {
        PacketHandler.sendToPlayer(player, new ClientboundWeatherSyncPacket(
                EternalSnowState.weatherEventId,
                EternalSnowState.windYawDeg,
                EternalSnowState.windSpeed,
                EternalSnowState.snowIntensity,
                EternalSnowState.worldAmbientTempC,
                EternalSnowState.eventTicksRemaining
        ));
    }

    private void onPlayerChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendCurrentWeatherTo(player);
        }
    }

    private void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            SnowAccumulationManager.resetTickBudget();
        } else if (event.phase == TickEvent.Phase.END) {
            WeatherEventManager.tickServerWeather(event.getServer());
        }
    }

    private void onLevelTick(final TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            ThermalEngine.tickLevelThermal(serverLevel);
            SnowAccumulationManager.tickLevelSnow(serverLevel);
            CampfireFuelManager.tickCampfires(serverLevel);
        }
    }

    /** v2.6.6: every mob (not just players) now feels the cold — five times slower. */
    private void onLivingTick(final LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        MobSurvivalManager.tickMob(mob);
    }

    /**
     * v2.6.6: nothing burns while snow is falling on it. Fire damage and fire ignition are both
     * dropped for mobs that stand in snow / under an active snowfall.
     */
    private void onLivingHurt(final LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // Also covers the "snow walks into a mob" case: a burning mob loses its fire on contact
        // with snow even when no fire damage is being dealt right now.
        if (MobSurvivalManager.shouldExtinguishFire(serverLevel, entity) && entity.isOnFire()) {
            entity.clearFire();
        }
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                && MobSurvivalManager.shouldExtinguishFire(serverLevel, entity)) {
            entity.clearFire();
            event.setCanceled(true);
        }
    }

    private void onPlayerTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer sp) {
            PlayerSurvivalManager.tickPlayer(sp);
        }
    }
}
