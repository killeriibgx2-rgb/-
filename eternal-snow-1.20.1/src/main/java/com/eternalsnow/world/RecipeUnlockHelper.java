package com.eternalsnow.world;

import com.eternalsnow.EternalSnowMod;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * v2.6.2: modded recipes never showed up in the vanilla recipe book until you crafted them once,
 * because vanilla only auto-unlocks recipes that have their "has_recipe" advancement. On login
 * every recipe of this mod is granted to the player, so the green book works from the start.
 */
public final class RecipeUnlockHelper {

    private RecipeUnlockHelper() {
    }

    public static void unlockAll(ServerPlayer player) {
        if (player == null) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerAdvancementManager manager = server.getAdvancements();
        for (Advancement advancement : manager.getAllAdvancements()) {
            ResourceLocation id = advancement.getId();
            if (!EternalSnowMod.MODID.equals(id.getNamespace())) {
                continue;
            }
            // Granting is idempotent: repeat logins are cheap, the client just re-reads its book.
            player.getAdvancements().award(advancement, "has_recipe");
        }
    }
}
