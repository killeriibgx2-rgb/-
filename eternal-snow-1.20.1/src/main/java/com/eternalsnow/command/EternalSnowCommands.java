package com.eternalsnow.command;

import com.eternalsnow.config.ServerSnowSettings;
import com.eternalsnow.network.ClientboundWeatherSyncPacket;
import com.eternalsnow.network.PacketHandler;
import com.eternalsnow.network.ServerboundUpdateSettingsPacket;
import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.thermal.ThermalEngine;
import com.eternalsnow.weather.WeatherEventManager;
import com.eternalsnow.weather.WeatherEventType;
import com.eternalsnow.world.SnowAccumulationManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class EternalSnowCommands {
    private EternalSnowCommands() {
    }

    public static boolean canPlayerEditServerSettings(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        MinecraftServer server = player.getServer();
        if (server != null && !server.isDedicatedServer() && server.isSingleplayerOwner(player.getGameProfile())) {
            return true;
        }
        return player.hasPermissions(2);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eternalsnow")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> sendStatus(ctx.getSource()))
                .then(Commands.literal("status").executes(ctx -> sendStatus(ctx.getSource())))
                .then(Commands.literal("temp").executes(ctx -> sendThermometer(ctx.getSource())))
                .then(Commands.literal("on").executes(ctx -> setEnabled(ctx.getSource(), true)))
                .then(Commands.literal("off").executes(ctx -> setEnabled(ctx.getSource(), false)))
                .then(Commands.literal("weather")
                        .then(Commands.literal("clear_frost").executes(ctx -> forceWeather(ctx.getSource(), WeatherEventType.CLEAR_FROST)))
                        .then(Commands.literal("light_snow").executes(ctx -> forceWeather(ctx.getSource(), WeatherEventType.LIGHT_SNOW)))
                        .then(Commands.literal("heavy_snow").executes(ctx -> forceWeather(ctx.getSource(), WeatherEventType.HEAVY_SNOW)))
                        .then(Commands.literal("gale").executes(ctx -> forceWeather(ctx.getSource(), WeatherEventType.GALE)))
                        .then(Commands.literal("blizzard").executes(ctx -> forceWeather(ctx.getSource(), WeatherEventType.BLIZZARD))))
                .then(Commands.literal("height")
                        .then(Commands.literal("infinite").executes(ctx -> {
                            ServerSnowSettings copy = EternalSnowState.getServerSettings().copy();
                            copy.infiniteHeight = true;
                            ServerboundUpdateSettingsPacket.applySettingsOnServer(ctx.getSource().getServer(), copy);
                            ctx.getSource().sendSuccess(() -> Component.literal("§b[EternalSnow]§r Высота снега: §b∞ Бесконечно"), true);
                            return 1;
                        }))
                        .then(Commands.argument("blocks", DoubleArgumentType.doubleArg(0.1D, 15.0D))
                                .executes(ctx -> {
                                    double blocks = DoubleArgumentType.getDouble(ctx, "blocks");
                                    ServerSnowSettings copy = EternalSnowState.getServerSettings().copy();
                                    copy.infiniteHeight = false;
                                    copy.averagePillarHeight = blocks;
                                    ServerboundUpdateSettingsPacket.applySettingsOnServer(ctx.getSource().getServer(), copy);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            String.format(Locale.ROOT, "§b[EternalSnow]§r Средняя высота сугроба: §e%.1f бл.", blocks)), true);
                                    return 1;
                                })))
                .then(Commands.literal("speed")
                        .then(Commands.argument("min_speed", IntegerArgumentType.integer(0, 100))
                                .then(Commands.argument("max_speed", IntegerArgumentType.integer(0, 150))
                                        .executes(ctx -> {
                                            int minSp = IntegerArgumentType.getInteger(ctx, "min_speed");
                                            int maxSp = Math.max(minSp, IntegerArgumentType.getInteger(ctx, "max_speed"));
                                            ServerSnowSettings copy = EternalSnowState.getServerSettings().copy();
                                            copy.minAccumulationSpeed = minSp;
                                            copy.maxAccumulationSpeed = maxSp;
                                            ServerboundUpdateSettingsPacket.applySettingsOnServer(ctx.getSource().getServer(), copy);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "§b[EternalSnow]§r Диапазон скорости осадков: §e" + minSp + "x .. " + maxSp + "x"), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("clear")
                        .executes(ctx -> clearCmd(ctx.getSource(), 24))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(4, 64))
                                .executes(ctx -> clearCmd(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
        );
    }

    private static int clearCmd(CommandSourceStack source, int r) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int cleared = SnowAccumulationManager.clearSnowAroundPlayer(player, r);
        source.sendSuccess(() -> Component.literal("§b[EternalSnow]§r Расчищено снежных блоков: §a" + cleared), true);
        return cleared;
    }

    private static int forceWeather(CommandSourceStack source, WeatherEventType type) {
        WeatherEventManager.forceEvent(source.getServer(), type, 9600);
        PacketHandler.broadcastToAll(new ClientboundWeatherSyncPacket(
                EternalSnowState.weatherEventId,
                EternalSnowState.windYawDeg,
                EternalSnowState.windSpeed,
                EternalSnowState.snowIntensity,
                EternalSnowState.worldAmbientTempC,
                EternalSnowState.eventTicksRemaining
        ));
        source.sendSuccess(
                () -> Component.literal("§b[EternalSnow]§r Погода изменена на: §e")
                        .append(Component.translatable(type.getTranslationKey())),
                true
        );
        return 1;
    }

    private static int sendThermometer(CommandSourceStack source) {
        float worldC = EternalSnowState.worldAmbientTempC;
        WeatherEventType ev = EternalSnowState.getActiveWeatherEvent();
        String localPart = "";
        if (source.getEntity() instanceof ServerPlayer sp && sp.level() instanceof ServerLevel sl) {
            float localC = ThermalEngine.getLocalTemperatureC(sl, sp.blockPosition().above());
            localPart = String.format(Locale.ROOT, " | В вашей точке: §e%+.1f°C§r", localC);
        }
        final String finalLocal = localPart;
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                        "§b[EternalSnow Термометр]§r Мир: §b%+.1f°C§r%s | Ветер: §f%d%% (%.0f°)§r | Событие: ",
                        worldC, finalLocal, Math.round(EternalSnowState.windSpeed * 100.0F), EternalSnowState.windYawDeg))
                .append(Component.translatable(ev.getTranslationKey())), false);
        return 1;
    }

    private static int sendStatus(CommandSourceStack source) {
        ServerSnowSettings s = EternalSnowState.getServerSettings();
        String heightLabel = s.infiniteHeight
                ? "∞ (Бесконечно)"
                : String.format(Locale.ROOT, "%.1f бл.", s.averagePillarHeight);
        WeatherEventType ev = EternalSnowState.getActiveWeatherEvent();
        float curPrecip = s.computeCurrentPrecipitationSpeed(ev, EternalSnowState.snowIntensity);
        source.sendSuccess(
                () -> Component.literal(String.format(Locale.ROOT,
                                "§b[EternalSnow v2.2]§r Активен: %s | Высота: %s | Осадки: %.1fx (%d..%dx) | Мир: %+.1f°C | Погода: ",
                                s.enabled, heightLabel, curPrecip, s.minAccumulationSpeed, s.maxAccumulationSpeed, EternalSnowState.worldAmbientTempC))
                        .append(Component.translatable(ev.getTranslationKey())),
                false
        );
        return 1;
    }

    private static int setEnabled(CommandSourceStack source, boolean enabled) {
        ServerSnowSettings copy = EternalSnowState.getServerSettings().copy();
        copy.enabled = enabled;
        ServerboundUpdateSettingsPacket.applySettingsOnServer(source.getServer(), copy);
        source.sendSuccess(
                () -> Component.literal("§b[EternalSnow]§r Вечная зима: " + (enabled ? "§aВКЛ" : "§cВЫКЛ")),
                true
        );
        return 1;
    }
}
