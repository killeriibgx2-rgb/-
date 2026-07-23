package net.wizard.elemental_awakening;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import net.wizard.elemental_awakening.networking.ModMessages;

@Mod(ElementalAwakening.MOD_ID)
public class ElementalAwakening {
    public static final String MOD_ID = "elemental_awakening";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<Item> EARTH_STAFF = ITEMS.register("earth_staff",
            () -> new net.wizard.elemental_awakening.item.EarthStaffItem(new Item.Properties().stacksTo(1)));

    public ElementalAwakening() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);

        // Автоматически включаем наш сетевой канал при запуске игры
        ModMessages.register();
    }
}
