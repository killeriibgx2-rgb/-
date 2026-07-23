package net.wizard.elemental_awakening.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wizard.elemental_awakening.ElementalAwakening;
import net.wizard.elemental_awakening.item.EarthStaffItem;
import net.wizard.elemental_awakening.mana.PlayerManaProvider;
import net.wizard.elemental_awakening.networking.ModMessages;
import net.wizard.elemental_awakening.networking.ManaSyncDataPacket;
import net.wizard.elemental_awakening.networking.OpenElementMenuPacket;

@Mod.EventBusSubscriber(modid = ElementalAwakening.MOD_ID)
public class ModEvents {

    // Привязываем Капабилити маны и стихий к каждому заспавненному игроку
    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            // ИСПРАВЛЕНО: Достаем объект через event.getObject() и у него проверяем капабилити
            if (!event.getObject().getCapability(PlayerManaProvider.PLAYER_MANA).isPresent()) {
                event.addCapability(new ResourceLocation(ElementalAwakening.MOD_ID, "properties"), new PlayerManaProvider());
            }
        }
    }

    // Каждый тик сервера восстанавливаем ману игроку и шлем пакет синхронизации на клиент
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            event.player.getCapability(PlayerManaProvider.PLAYER_MANA).ifPresent(mana -> {
                // Реген маны
                mana.regenMana();
                // Отправляем текущее значение маны на клиент, чтобы полоска двигалась в HUD
                if (event.player instanceof ServerPlayer serverPlayer) {
                    ModMessages.sendToPlayer(new ManaSyncDataPacket(mana.getMana()), serverPlayer);
                }
            });
        }
    }

    // Проверяем игрока при первом входе в мир. Если стихия == 0, заставляем выбрать!
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerManaProvider.PLAYER_MANA).ifPresent(mana -> {
                if (mana.getElement() == 0) {
                    // Отправляем пакет на клиент, чтобы принудительно открылось наше Главное Меню выбора
                    ModMessages.sendToPlayer(new OpenElementMenuPacket(), player);
                }
            });
        }
    }

    // Серверный тик мира: крутит таймеры стен, чтобы они росли и вовремя исчезали
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide()) {
            synchronized (EarthStaffItem.ACTIVE_WALLS) {
                // Бежим по списку активных стен с конца в начало для безопасного удаления
                for (int i = EarthStaffItem.ACTIVE_WALLS.size() - 1; i >= 0; i--) {
                    EarthStaffItem.ActiveWall wall = EarthStaffItem.ACTIVE_WALLS.get(i);

                    // Вызываем родной метод тика стены
                    if (wall.tick()) {
                        EarthStaffItem.ACTIVE_WALLS.remove(i);
                    }
                }
            }
        }
    }
}
