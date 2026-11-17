package com.eternalsnow.registry;

import com.eternalsnow.EternalSnowMod;
import com.eternalsnow.block.ChimneyBlock;
import com.eternalsnow.block.CocoaPotBlock;
import com.eternalsnow.block.CocoaPotBlockEntity;
import com.eternalsnow.block.FireplaceBlock;
import com.eternalsnow.block.FireplaceBlockEntity;
import com.eternalsnow.block.FireplacePartBlock;
import com.eternalsnow.block.HeatAccumulatorBlock;
import com.eternalsnow.block.HeatAccumulatorBlockEntity;
import com.eternalsnow.block.IcicleBlock;
import com.eternalsnow.block.ThermalShutterBlock;
import com.eternalsnow.block.WallThermometerBlock;
import com.eternalsnow.block.WallThermometerBlockEntity;
import com.eternalsnow.effect.ColdImmunityEffect;
import com.eternalsnow.effect.CozyHearthEffect;
import com.eternalsnow.effect.WarmRecoveryEffect;
import com.eternalsnow.effect.WetEffect;
import com.eternalsnow.enchantment.SnowShovelEnchantment;
import com.eternalsnow.entity.FestiveCopperGolemEntity;
import com.eternalsnow.item.CandyCaneItem;
import com.eternalsnow.item.CaramelItem;
import com.eternalsnow.item.CopperThermosItem;
import com.eternalsnow.item.EmptyCupItem;
import com.eternalsnow.item.HeatScannerItem;
import com.eternalsnow.item.LocalThermometerItem;
import com.eternalsnow.item.HotDrinkCupItem;
import com.eternalsnow.item.SnowShovelItem;
import com.eternalsnow.item.ThermometerItem;
import com.eternalsnow.item.WarmingPillItem;
import com.eternalsnow.item.WoodenFirestarterItem;
import com.eternalsnow.menu.CocoaPotMenu;
import com.eternalsnow.recipe.ArmorInsulationRecipe;
import com.eternalsnow.recipe.RainbowCandyRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    private ModRegistry() {
    }

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, EternalSnowMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EternalSnowMod.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, EternalSnowMod.MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, EternalSnowMod.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, EternalSnowMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EternalSnowMod.MODID);
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, EternalSnowMod.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EternalSnowMod.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, EternalSnowMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EternalSnowMod.MODID);

    // Blocks
    public static final RegistryObject<Block> COCOA_POT = BLOCKS.register(
            "cocoa_pot",
            () -> new CocoaPotBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion())
    );
    public static final RegistryObject<Block> WALL_THERMOMETER = BLOCKS.register(
            "wall_thermometer",
            () -> new WallThermometerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.6F)
                    .sound(SoundType.WOOD)
                    .noCollission()
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY))
    );
    public static final RegistryObject<Block> THERMAL_SHUTTER = BLOCKS.register(
            "thermal_shutter",
            () -> new ThermalShutterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.8F)
                    .sound(SoundType.WOOD))
    );
    public static final RegistryObject<Block> HEAT_ACCUMULATOR = BLOCKS.register(
            "heat_accumulator",
            () -> new HeatAccumulatorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion())
    );
    public static final RegistryObject<Block> FIREPLACE = BLOCKS.register(
            "fireplace",
            () -> new FireplaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.5F, 8.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .lightLevel(FireplaceBlock::getLightEmissionForState))
    );
    public static final RegistryObject<Block> FIREPLACE_PART = BLOCKS.register(
            "fireplace_part",
            () -> new FireplacePartBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.5F, 8.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK))
    );
    public static final RegistryObject<Block> CHIMNEY = BLOCKS.register(
            "chimney",
            () -> new ChimneyBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE))
    );
    public static final RegistryObject<Block> ICICLE = BLOCKS.register(
            "icicle",
            () -> new IcicleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(0.2F)
                    .sound(SoundType.GLASS)
                    .noCollission()
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY))
    );

    // Block Entities
    public static final RegistryObject<BlockEntityType<CocoaPotBlockEntity>> COCOA_POT_BE = BLOCK_ENTITIES.register(
            "cocoa_pot",
            () -> BlockEntityType.Builder.of(CocoaPotBlockEntity::new, COCOA_POT.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<WallThermometerBlockEntity>> WALL_THERMOMETER_BE = BLOCK_ENTITIES.register(
            "wall_thermometer",
            () -> BlockEntityType.Builder.of(WallThermometerBlockEntity::new, WALL_THERMOMETER.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<HeatAccumulatorBlockEntity>> HEAT_ACCUMULATOR_BE = BLOCK_ENTITIES.register(
            "heat_accumulator",
            () -> BlockEntityType.Builder.of(HeatAccumulatorBlockEntity::new, HEAT_ACCUMULATOR.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<FireplaceBlockEntity>> FIREPLACE_BE = BLOCK_ENTITIES.register(
            "fireplace",
            () -> BlockEntityType.Builder.of(FireplaceBlockEntity::new, FIREPLACE.get()).build(null)
    );

    // Menus
    public static final RegistryObject<MenuType<CocoaPotMenu>> COCOA_POT_MENU = MENUS.register(
            "cocoa_pot",
            () -> IForgeMenuType.create(CocoaPotMenu::new)
    );

    // Mob Effects
    public static final RegistryObject<MobEffect> COLD_IMMUNITY = MOB_EFFECTS.register(
            "cold_immunity",
            ColdImmunityEffect::new
    );
    public static final RegistryObject<MobEffect> WARM_RECOVERY = MOB_EFFECTS.register(
            "warm_recovery",
            WarmRecoveryEffect::new
    );
    public static final RegistryObject<MobEffect> WET_EFFECT = MOB_EFFECTS.register(
            "wet",
            WetEffect::new
    );
    public static final RegistryObject<MobEffect> COZY_HEARTH = MOB_EFFECTS.register(
            "cozy_hearth",
            CozyHearthEffect::new
    );

    // Recipe Serializers
    public static final RegistryObject<RecipeSerializer<ArmorInsulationRecipe>> ARMOR_INSULATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("armor_insulation", () -> new SimpleCraftingRecipeSerializer<>(ArmorInsulationRecipe::new));
    public static final RegistryObject<RecipeSerializer<RainbowCandyRecipe>> RAINBOW_CANDY_SERIALIZER =
            RECIPE_SERIALIZERS.register("rainbow_candy", () -> new SimpleCraftingRecipeSerializer<>(RainbowCandyRecipe::new));

    // Entities
    public static final RegistryObject<EntityType<FestiveCopperGolemEntity>> FESTIVE_COPPER_GOLEM = ENTITIES.register(
            "festive_copper_golem",
            () -> EntityType.Builder.<FestiveCopperGolemEntity>of(FestiveCopperGolemEntity::new, MobCategory.MISC)
                    .sized(0.7F, 1.55F)
                    .clientTrackingRange(8)
                    .build(new ResourceLocation(EternalSnowMod.MODID, "festive_copper_golem").toString())
    );

    // Tools, Blocks & Survival Items
    public static final RegistryObject<Item> THERMOMETER = ITEMS.register(
            "thermometer",
            () -> new ThermometerItem(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> LOCAL_THERMOMETER = ITEMS.register(
            "local_thermometer",
            () -> new LocalThermometerItem(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> HEAT_SCANNER = ITEMS.register(
            "heat_scanner",
            () -> new HeatScannerItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE))
    );
    public static final RegistryObject<Item> SNOW_SHOVEL = ITEMS.register(
            "snow_shovel",
            () -> new SnowShovelItem(new Item.Properties().durability(512))
    );
    public static final RegistryObject<Item> WOODEN_FIRESTARTER = ITEMS.register(
            "wooden_firestarter",
            () -> new WoodenFirestarterItem(new Item.Properties().durability(14))
    );
    public static final RegistryObject<Item> FESTIVE_COPPER_GOLEM_SPAWN_EGG = ITEMS.register(
            "festive_copper_golem_spawn_egg",
            () -> new ForgeSpawnEggItem(FESTIVE_COPPER_GOLEM, 0xC86D3B, 0xDCEFFF, new Item.Properties())
    );

    public static final RegistryObject<Item> FIREPLACE_ITEM = ITEMS.register(
            "fireplace",
            () -> new BlockItem(FIREPLACE.get(), new Item.Properties())
    );
    public static final RegistryObject<Item> CHIMNEY_ITEM = ITEMS.register(
            "chimney",
            () -> new BlockItem(CHIMNEY.get(), new Item.Properties())
    );
    public static final RegistryObject<Item> ICICLE_ITEM = ITEMS.register(
            "icicle",
            () -> new BlockItem(ICICLE.get(), new Item.Properties())
    );
    public static final RegistryObject<Item> THERMAL_SHUTTER_ITEM = ITEMS.register(
            "thermal_shutter",
            () -> new BlockItem(THERMAL_SHUTTER.get(), new Item.Properties())
    );
    public static final RegistryObject<Item> HEAT_ACCUMULATOR_ITEM = ITEMS.register(
            "heat_accumulator",
            () -> new BlockItem(HEAT_ACCUMULATOR.get(), new Item.Properties())
    );
    public static final RegistryObject<Item> WALL_THERMOMETER_ITEM = ITEMS.register(
            "wall_thermometer",
            () -> new BlockItem(WALL_THERMOMETER.get(), new Item.Properties())
    );
    public static final RegistryObject<Item> COCOA_POT_ITEM = ITEMS.register(
            "cocoa_pot",
            () -> new BlockItem(COCOA_POT.get(), new Item.Properties())
    );
    public static final RegistryObject<Item> COPPER_THERMOS = ITEMS.register(
            "copper_thermos",
            () -> new CopperThermosItem(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> WARMING_PILL = ITEMS.register(
            "warming_pill",
            () -> new WarmingPillItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON))
    );
    public static final RegistryObject<Item> EMPTY_CUP = ITEMS.register(
            "empty_cup",
            () -> new EmptyCupItem(new Item.Properties().stacksTo(16))
    );
    public static final RegistryObject<Item> COCOA_CUP = ITEMS.register(
            "cocoa_cup",
            () -> new HotDrinkCupItem(new Item.Properties(), false)
    );
    public static final RegistryObject<Item> CARAMEL_CUP = ITEMS.register(
            "caramel_cup",
            () -> new HotDrinkCupItem(new Item.Properties(), true)
    );
    public static final RegistryObject<Item> CARAMEL = ITEMS.register(
            "caramel",
            () -> new CaramelItem(new Item.Properties())
    );
    public static final RegistryObject<Item> CANDY_YELLOW = ITEMS.register(
            "candy_yellow",
            () -> new CandyCaneItem(new Item.Properties(), 3, 0.35F, "eternalsnow.tooltip.candy.craft_single", false)
    );
    public static final RegistryObject<Item> CANDY_RED = ITEMS.register(
            "candy_red",
            () -> new CandyCaneItem(new Item.Properties(), 3, 0.35F, "eternalsnow.tooltip.candy.craft_single", false)
    );
    public static final RegistryObject<Item> CANDY_GREEN = ITEMS.register(
            "candy_green",
            () -> new CandyCaneItem(new Item.Properties(), 3, 0.35F, "eternalsnow.tooltip.candy.craft_single", false)
    );
    public static final RegistryObject<Item> CANDY_BLUE = ITEMS.register(
            "candy_blue",
            () -> new CandyCaneItem(new Item.Properties(), 3, 0.35F, "eternalsnow.tooltip.candy.craft_single", false)
    );
    public static final RegistryObject<Item> CANDY_PURPLE = ITEMS.register(
            "candy_purple",
            () -> new CandyCaneItem(new Item.Properties(), 3, 0.35F, "eternalsnow.tooltip.candy.craft_single", false)
    );
    public static final RegistryObject<Item> CANDY_RAINBOW = ITEMS.register(
            "candy_rainbow",
            () -> new CandyCaneItem(new Item.Properties().rarity(Rarity.UNCOMMON), 5, 0.60F, "eternalsnow.tooltip.candy.craft_rainbow", true)
    );

    // Enchantments
    public static final RegistryObject<Enchantment> SNOW_WIDTH = ENCHANTMENTS.register(
            "snow_width",
            SnowShovelEnchantment::new
    );
    public static final RegistryObject<Enchantment> SNOW_LENGTH = ENCHANTMENTS.register(
            "snow_length",
            SnowShovelEnchantment::new
    );
    public static final RegistryObject<Enchantment> SNOW_HEIGHT = ENCHANTMENTS.register(
            "snow_height",
            SnowShovelEnchantment::new
    );

    // Custom Winter Sounds
    public static final RegistryObject<SoundEvent> WIND_CALM = registerSound("wind_calm");
    public static final RegistryObject<SoundEvent> WIND_GALE = registerSound("wind_gale");
    public static final RegistryObject<SoundEvent> WIND_BLIZZARD = registerSound("wind_blizzard");
    public static final RegistryObject<SoundEvent> FROST_SHIVER = registerSound("frost_shiver");
    public static final RegistryObject<SoundEvent> SCANNER_TOGGLE = registerSound("scanner_toggle");

    // Creative Tab
    public static final RegistryObject<CreativeModeTab> CREATIVE_TAB = TABS.register(
            "eternalsnow_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.eternalsnow"))
                    .icon(() -> new ItemStack(FIREPLACE_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(FIREPLACE_ITEM.get());
                        output.accept(CHIMNEY_ITEM.get());
                        output.accept(WOODEN_FIRESTARTER.get());
                        output.accept(THERMOMETER.get());
                        output.accept(LOCAL_THERMOMETER.get());
                        output.accept(WALL_THERMOMETER_ITEM.get());
                        output.accept(HEAT_SCANNER.get());
                        output.accept(THERMAL_SHUTTER_ITEM.get());
                        output.accept(HEAT_ACCUMULATOR_ITEM.get());
                        output.accept(SNOW_SHOVEL.get());
                        output.accept(FESTIVE_COPPER_GOLEM_SPAWN_EGG.get());
                        output.accept(WARMING_PILL.get());
                        output.accept(COCOA_POT_ITEM.get());
                        output.accept(COPPER_THERMOS.get());
                        output.accept(EMPTY_CUP.get());
                        output.accept(COCOA_CUP.get());
                        output.accept(CARAMEL_CUP.get());
                        output.accept(CARAMEL.get());
                        output.accept(CANDY_RED.get());
                        output.accept(CANDY_YELLOW.get());
                        output.accept(CANDY_GREEN.get());
                        output.accept(CANDY_BLUE.get());
                        output.accept(CANDY_PURPLE.get());
                        output.accept(CANDY_RAINBOW.get());
                        output.accept(ICICLE_ITEM.get());
                    })
                    .build()
    );

    private static RegistryObject<SoundEvent> registerSound(String name) {
        ResourceLocation id = new ResourceLocation(EternalSnowMod.MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        MOB_EFFECTS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        ENCHANTMENTS.register(modBus);
        SOUNDS.register(modBus);
        TABS.register(modBus);
    }
}
