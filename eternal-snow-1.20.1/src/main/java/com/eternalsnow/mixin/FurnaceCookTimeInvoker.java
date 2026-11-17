package com.eternalsnow.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * v2.7 — accessor for the private {@code getTotalCookTime}, needed by
 * {@link AbstractFurnaceBlockEntityMixin} to add the thaw time to the vanilla cook time.
 *
 * <p>Kept as its own interface mixin because {@code @Invoker} on a static target only works from an
 * interface. If the accessor is ever renamed by a future Minecraft version, the caller falls back to
 * a sane default instead of crashing a furnace.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public interface FurnaceCookTimeInvoker {

    @Invoker("getTotalCookTime")
    static int eternalsnow$getTotalCookTime(Level level, AbstractFurnaceBlockEntity furnace) {
        throw new AssertionError("FurnaceCookTimeInvoker was not applied");
    }
}
