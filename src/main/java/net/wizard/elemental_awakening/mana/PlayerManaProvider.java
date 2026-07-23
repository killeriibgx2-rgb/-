package net.wizard.elemental_awakening.mana;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerManaProvider implements ICapabilitySerializable<CompoundTag> {
    // Регистрируем уникальный токен капабилити для Forge
    public static Capability<PlayerMana> PLAYER_MANA = CapabilityManager.get(new CapabilityToken<>(){});

    private PlayerMana mana = null;
    private final LazyOptional<PlayerMana> optional = LazyOptional.of(this::createPlayerMana);

    private PlayerMana createPlayerMana() {
        if (this.mana == null) {
            this.mana = new PlayerMana();
        }
        return this.mana;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_MANA) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    // Сохраняем ману в файл сохранения мира (NBT), чтобы она не пропадала при выходе
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createPlayerMana();
        nbt.putFloat("mana", this.mana.getMana());
        return nbt;
    }

    // Загружаем ману из файла сохранения мира при входе в игру
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createPlayerMana();
        this.mana.setMana(nbt.getFloat("mana"));
    }
}
