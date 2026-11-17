package com.eternalsnow.client;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * v2.6.6 — drops the memoised villager outfits whenever resource packs are (re)loaded, so a texture
 * pack that ships its own villager clothes wins immediately instead of after a game restart.
 *
 * <p>Nothing is parsed: this is purely a "resources changed" hook, hence the {@code Void} payload.
 */
public final class VillagerWardrobeReloadListener extends SimplePreparableReloadListener<Void> {

    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void unused, ResourceManager resourceManager, ProfilerFiller profiler) {
        VillagerWardrobe.invalidate();
    }
}
