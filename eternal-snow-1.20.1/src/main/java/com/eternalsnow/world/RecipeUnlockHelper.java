package com.eternalsnow.world;

import com.eternalsnow.EternalSnowMod;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Makes every recipe of this mod show up in the vanilla recipe book from the first login.
 *
 * <p>v2.6.2 introduced this helper; v2.6.6b fixes why it silently did nothing. Two independent
 * bugs were in the way:</p>
 * <ol>
 *   <li>It awarded the hard-coded criterion {@code "has_recipe"}, but the shipped advancement
 *       files declare {@code "has_the_recipe"} — {@link AdvancementProgress#grantProgress} just
 *       returned {@code false} and no reward was ever handed out. Now the real criterion names are
 *       read straight out of the advancement, so the two data/code sides can never drift apart
 *       again.</li>
 *   <li>Even with the advancement granted, the recipe only reaches the book through
 *       {@code rewards.recipes}. That is now belt-and-braces: every recipe of the mod is also
 *       pushed through {@link ServerPlayer#awardRecipesByKey} directly, so a player whose
 *       advancement file is missing/corrupt still gets a working book.</li>
 * </ol>
 *
 * <p>Both paths are idempotent ({@code ServerRecipeBook.addRecipes} skips ids already known and
 * sends a packet only for the new ones), so re-logging in is cheap.</p>
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

        // 1. Grant the bookkeeping advancements — using THEIR criterion names, not a guess.
        ServerAdvancementManager advancements = server.getAdvancements();
        for (Advancement advancement : advancements.getAllAdvancements()) {
            ResourceLocation id = advancement.getId();
            if (!EternalSnowMod.MODID.equals(id.getNamespace())) {
                continue;
            }
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
            if (progress.isDone()) {
                continue;
            }
            // award() is a no-op for criteria that are already satisfied, so just try each one.
            for (String criterion : advancement.getCriteria().keySet()) {
                player.getAdvancements().award(advancement, criterion);
            }
        }

        // 2. Guarantee the recipes themselves are known (this is what fills the book + the green
        //    "new recipe" badges, and what vanilla uses when you craft something by hand).
        RecipeManager recipes = server.getRecipeManager();
        List<ResourceLocation> ours = new ArrayList<>();
        for (Recipe<?> recipe : recipes.getRecipes()) {
            ResourceLocation id = recipe.getId();
            if (EternalSnowMod.MODID.equals(id.getNamespace()) && !player.getRecipeBook().contains(id)) {
                ours.add(id);
            }
        }
        if (!ours.isEmpty()) {
            player.awardRecipesByKey(ours.toArray(new ResourceLocation[0]));
        }
    }
}
