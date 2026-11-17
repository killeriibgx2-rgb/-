package com.eternalsnow.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Client-only: change presentation coordinates, never slot IDs or container indices. */
@Mixin(Slot.class)
public interface SlotPositionAccessor {
    @Mutable
    @Accessor("x")
    void eternalsnow$setX(int x);

    @Mutable
    @Accessor("y")
    void eternalsnow$setY(int y);
}
