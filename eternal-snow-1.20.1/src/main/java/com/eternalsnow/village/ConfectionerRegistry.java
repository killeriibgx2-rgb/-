package com.eternalsnow.village;

import com.eternalsnow.block.ConfectionerTableBlock;
import com.eternalsnow.registry.ModRegistry;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ConfectionerRegistry {
    private ConfectionerRegistry() {}
    public static final DeferredRegister<PoiType> POIS=DeferredRegister.create(ForgeRegistries.POI_TYPES,"eternalsnow");
    public static final DeferredRegister<VillagerProfession> PROFESSIONS=DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS,"eternalsnow");
    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS=DeferredRegister.create(Registries.STRUCTURE_PROCESSOR,"eternalsnow");
    public static final RegistryObject<Block> TABLE=ModRegistry.BLOCKS.register("confectioner_table",()->new ConfectionerTableBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Item> TABLE_ITEM=ModRegistry.ITEMS.register("confectioner_table",()->new BlockItem(TABLE.get(),new Item.Properties()));
    public static final RegistryObject<PoiType> POI=POIS.register("confectioner",()->new PoiType(
            ImmutableSet.copyOf(TABLE.get().getStateDefinition().getPossibleStates()),1,1));
    public static final RegistryObject<VillagerProfession> PROFESSION=PROFESSIONS.register("confectioner",()->new VillagerProfession(
            "confectioner", holder->holder.value()==POI.get(), holder->holder.value()==POI.get(),
            ImmutableSet.of(),ImmutableSet.of(),SoundEvents.VILLAGER_WORK_LIBRARIAN));
    public static final RegistryObject<StructureProcessorType<ConfectionerHouseProcessor>> HOUSE_PROCESSOR=
            PROCESSORS.register("confectioner_house",()->()->Codec.unit(ConfectionerHouseProcessor.INSTANCE));
    public static void register(IEventBus bus) { POIS.register(bus);PROFESSIONS.register(bus);PROCESSORS.register(bus); }
}
