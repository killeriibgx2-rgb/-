package com.eternalsnow.client;

import com.eternalsnow.EternalSnowMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v2.6.6 — winter wardrobe lookup for {@code VillagerProfessionLayer}.
 *
 * <p>Lives outside the mixin on purpose: a static field initialiser written in a mixin class is not
 * merged into the target class, so the map would arrive as {@code null} and NPE on the first
 * villager. Here it is an ordinary class and the initialiser is guaranteed to run.
 *
 * <p>The layer resolves its texture for every visible villager on every frame, so the answer is
 * memoised per profession and dropped on resource reload (a texture pack must win immediately).
 */
public final class VillagerWardrobe {

    private static final String ROOT = "textures/entity/villager/profession/";

    private static final Map<ResourceLocation, Optional<ResourceLocation>> CACHE = new ConcurrentHashMap<>();

    private VillagerWardrobe() {
    }

    public static void invalidate() {
        CACHE.clear();
    }

    /**
     * @return our winter texture for this profession, or {@code null} when the profession is
     *         vanilla-shaped but has no repaint (modded jobs keep their own clothes).
     */
    public static ResourceLocation resolve(ResourceLocation profession) {
        if (profession == null) {
            return null;
        }
        Optional<ResourceLocation> cached = CACHE.get(profession);
        if (cached == null) {
            ResourceLocation custom = new ResourceLocation(EternalSnowMod.MODID, ROOT + profession.getPath() + ".png");
            ResourceManager rm = Minecraft.getInstance().getResourceManager();
            boolean present = !custom.equals(profession) && rm != null && rm.getResource(custom).isPresent();
            cached = present ? Optional.of(custom) : Optional.empty();
            CACHE.put(profession, cached);
        }
        return cached.orElse(null);
    }
}
