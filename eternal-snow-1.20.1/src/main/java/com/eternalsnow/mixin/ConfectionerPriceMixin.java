package com.eternalsnow.mixin;

import com.eternalsnow.village.ConfectionerRegistry;
import com.eternalsnow.village.ConfectionerTrades;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class ConfectionerPriceMixin {
    @Inject(method="updateSpecialPrices",at=@At("TAIL"))
    private void eternalsnow$noCandyArbitrage(Player player,CallbackInfo ci) {
        Villager self=(Villager)(Object)this;
        if(self.getVillagerData().getProfession()!=ConfectionerRegistry.PROFESSION.get())return;
        for(var offer:self.getOffers()) {
            // Prevent cure/Hero discounts turning 1 emerald -> 3 candies -> 3 emeralds.
            // Only the bidirectional commodity is protected; other discounts remain vanilla.
            if(ConfectionerTrades.ordinaryCandy(offer.getBaseCostA()) || ConfectionerTrades.ordinaryCandy(offer.getResult()))
                offer.resetSpecialPriceDiff();
        }
    }
}
