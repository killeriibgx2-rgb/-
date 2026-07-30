package com.zzynex.immersive_matrix;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    // Создаем регистратор блоков под наш Mod ID
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ImmersiveMatrix.MOD_ID);

    // --- РЕГИСТРАЦИЯ ЦВЕТНЫХ БЛОКОВ ---

    public static final RegistryObject<Block> BLACK_BLOCK = BLOCKS.register("black",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .lightLevel((state) -> 15)));

    public static final RegistryObject<Block> GLITCH_BLOCK = BLOCKS.register("glitch_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.STONE)
                    .lightLevel((state) -> 15)));

    public static final RegistryObject<Block> BLU_BLOCK = BLOCKS.register("blu",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .lightLevel((state) -> 15)));

    public static final RegistryObject<Block> RED_BLOCK = BLOCKS.register("red",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .lightLevel((state) -> 15)));

    public static final RegistryObject<Block> WHITE_BLOCK = BLOCKS.register("white",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .lightLevel((state) -> 15)));

    public static final RegistryObject<Block> YELLOW_BLOCK = BLOCKS.register("yellow",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .lightLevel((state) -> 15)));

    // --- РЕГИСТРАЦИЯ БЛОКА ДВЕРИ ---
    public static final RegistryObject<Block> MATRIX_DOOR_BLOCK = BLOCKS.register("matrix_door",
            () -> new MatrixDoorBlock());

    // Метод для подключения этого списка к главной шине мода
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
