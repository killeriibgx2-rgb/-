package com.zzynex.immersive_matrix;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.UUID;

public class MatrixDoorBlockEntity extends BlockEntity {
    // Регистратор для Block Entities под наш Mod ID
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ImmersiveMatrix.MOD_ID);

    // Регистрируем наш тип контейнера в игре
    public static final RegistryObject<BlockEntityType<MatrixDoorBlockEntity>> MATRIX_DOOR_BE =
            BLOCK_ENTITIES.register("matrix_door_be", () ->
                    BlockEntityType.Builder.of(MatrixDoorBlockEntity::new, ModBlocks.MATRIX_DOOR_BLOCK.get()).build(null));

    private UUID ownerUUID;
    private String ownerName = "";

    public MatrixDoorBlockEntity(BlockPos pos, BlockState state) {
        super(MATRIX_DOOR_BE.get(), pos, state);
    }

    // Сохраняем UUID владельца в NBT файл сохранения мира
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
            tag.putString("OwnerName", this.ownerName);
        }
    }

    // Загружаем UUID владельца при загрузке чанка игрой
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
            this.ownerName = tag.getString("OwnerName");
        }
    }

    public void setOwner(UUID uuid, String name) {
        this.ownerUUID = uuid;
        this.ownerName = name;
        this.setChanged(); // Уведомляем игру, что данные обновились и их нужно сохранить
    }

    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    public String getOwnerName() {
        return this.ownerName;
    }
}
