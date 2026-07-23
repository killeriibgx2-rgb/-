package net.wizard.elemental_awakening.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wizard.elemental_awakening.ElementalAwakening;
import net.wizard.elemental_awakening.item.EarthStaffItem;
import net.wizard.elemental_awakening.mana.PlayerManaProvider;
import net.wizard.elemental_awakening.networking.ManaSyncDataPacket;
import net.wizard.elemental_awakening.networking.ModMessages;

@Mod.EventBusSubscriber(modid = ElementalAwakening.MOD_ID)
public class ModEvents {

    // 1. Привязываем нашу ману к игроку, когда он появляется в мире
    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerManaProvider.PLAYER_MANA).isPresent()) {
                event.addCapability(new ResourceLocation(ElementalAwakening.MOD_ID, "properties"), new PlayerManaProvider());
            }
        }
    }

    // 2. Каждый тик игрока запускаем регенерацию маны И СИНХРОНИЗИРУЕМ её с экраном!
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Синхронизацию и реген делаем только на СЕРВЕРЕ для безопасности
            if (!event.player.level().isClientSide && event.player instanceof ServerPlayer serverPlayer) {
                event.player.getCapability(PlayerManaProvider.PLAYER_MANA).ifPresent(mana -> {
                    // Регенерируем ману (по 0.2 за тик)
                    mana.regenMana();

                    // МГНОВЕННО отправляем новое число маны по сети на экран игрока
                    ModMessages.sendToPlayer(new ManaSyncDataPacket(mana.getMana()), serverPlayer);
                });
            }
        }
    }

    // 3. Каждый тик МИРА крутит таймеры стены. Частицы и звуки работают на 100%!
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (!event.level.isClientSide()) {
                synchronized (EarthStaffItem.ACTIVE_WALLS) {
                    for (int i = EarthStaffItem.ACTIVE_WALLS.size() - 1; i >= 0; i--) {
                        EarthStaffItem.ActiveWall wall = EarthStaffItem.ACTIVE_WALLS.get(i);

                        wall.tick();

                        if (wall.isFinished()) {
                            EarthStaffItem.ACTIVE_WALLS.remove(i);
                        }
                    }
                }
            }
        }
    }
}
