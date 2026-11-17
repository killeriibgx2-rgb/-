package com.eternalsnow.mixin;

import com.eternalsnow.client.VillagerWardrobe;
import com.eternalsnow.state.EternalSnowState;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * v2.6.6 — Villagers wear clothes that match their job.
 *
 * The profession overlay is a single texture resolved by VillagerProfessionLayer through
 * {@code getResourceLocation(kind, id)}. Redirecting that call for kind "profession" to our own
 * winter wardrobe repaints the jacket while the vanilla profession hat (a separate metadata-driven
 * pass) stays intact:
 *
 *   unemployed -> plain green jacket     cleric    -> violet robe
 *   armorer    -> slate blue overalls    farmer    -> ochre
 *   butcher    -> oxblood apron          librarian -> pale parchment
 *   ... one palette per job, all with a stitched hem.
 *
 * Client-side, texture swap only: no model rebuild, no extra render layer, no per-tick cost.
 * The lookup and its cache live in {@link VillagerWardrobe} — a mixin must not own static state.
 */
@Mixin(value = net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer.class)
public abstract class VillagerProfessionMixin {

    // require = 0: if a future MC/Forge version renames this private helper, villagers simply keep
    // their vanilla clothes instead of crashing the client.
    @Inject(method = "getResourceLocation", at = @At("HEAD"), cancellable = true, require = 0)
    private void eternalsnow$winterWardrobe(String kind, ResourceLocation id,
                                            CallbackInfoReturnable<ResourceLocation> cir) {
        if (!"profession".equals(kind) || id == null) {
            return;
        }
        if (!EternalSnowState.getEffectiveSettings().enabled) {
            return;
        }
        ResourceLocation custom = VillagerWardrobe.resolve(id);
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}
