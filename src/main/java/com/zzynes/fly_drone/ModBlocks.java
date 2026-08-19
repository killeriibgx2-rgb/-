package com.zzynes.fly_drone;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    // Создаем регистратор для блоков мода
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, FlyDroneMod.MOD_ID);

    // НАША ЛИТИЕВАЯ РУДА (блр)
    // Настраиваем свойства: прочность как у железной руды, звук камня, требует кирку
    public static final RegistryObject<Block> LITHIUM_ORE = BLOCKS.register("lithium_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_ORE)
                    .strength(3.0f, 3.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    // Метод для подключения регистрации в главный класс
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
