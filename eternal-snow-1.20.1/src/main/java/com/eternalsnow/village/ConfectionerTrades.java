package com.eternalsnow.village;

import com.eternalsnow.registry.ModRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="eternalsnow")
public final class ConfectionerTrades {
    private ConfectionerTrades() {}
    private static VillagerTrades.ItemListing buy(Item item,int count,int xp) {
        return (entity,random)->new MerchantOffer(new ItemStack(item,count),new ItemStack(Items.EMERALD),16,xp,.05F);
    }
    private static VillagerTrades.ItemListing sell(Item item,int count,int emeralds,int xp) {
        return (entity,random)->new MerchantOffer(new ItemStack(Items.EMERALD,emeralds),new ItemStack(item,count),12,xp,.05F);
    }
    public static boolean ordinaryCandy(ItemStack stack) {
        return stack.is(ModRegistry.CANDY_RED.get()) || stack.is(ModRegistry.CANDY_YELLOW.get())
                || stack.is(ModRegistry.CANDY_GREEN.get()) || stack.is(ModRegistry.CANDY_BLUE.get())
                || stack.is(ModRegistry.CANDY_PURPLE.get());
    }
    @SubscribeEvent public static void trades(VillagerTradesEvent event) {
        if(event.getType()!=ConfectionerRegistry.PROFESSION.get())return;
        var trades=event.getTrades();
        trades.get(1).add(buy(Items.SUGAR,12,2));
        trades.get(1).add(sell(Items.COOKIE,5,3,1));
        trades.get(2).add(buy(Items.COCOA_BEANS,8,10));
        trades.get(2).add(sell(ModRegistry.COCOA_CUP.get(),1,2,5));
        trades.get(3).add(buy(ModRegistry.CARAMEL.get(),4,20));
        trades.get(3).add((entity,random)->{
            Item[] candies={ModRegistry.CANDY_RED.get(),ModRegistry.CANDY_YELLOW.get(),ModRegistry.CANDY_GREEN.get(),ModRegistry.CANDY_BLUE.get(),ModRegistry.CANDY_PURPLE.get()};
            return sell(candies[random.nextInt(candies.length)],3,2,10).getOffer(entity,random);
        });
        trades.get(4).add(buy(ModRegistry.CANDY_RED.get(),3,30));
        trades.get(4).add(buy(ModRegistry.CANDY_YELLOW.get(),3,30));
        // Vanilla selects two of the three master's colours; no custom trading screen.
        trades.get(5).add(buy(ModRegistry.CANDY_GREEN.get(),3,30));
        trades.get(5).add(buy(ModRegistry.CANDY_BLUE.get(),3,30));
        trades.get(5).add(buy(ModRegistry.CANDY_PURPLE.get(),3,30));
    }
}
