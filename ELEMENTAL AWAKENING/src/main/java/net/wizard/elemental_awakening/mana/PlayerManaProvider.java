package net.wizard.elemental_awakening.mana;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.wizard.elemental_awakening.ElementalAwakening;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerManaProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    // Регистрируем капабилити в системе Forge
    public static Capability<PlayerMana> PLAYER_MANA = CapabilityManager.get(new CapabilityToken<PlayerMana>() {});

    private PlayerMana playerMana = null;
    private final LazyOptional<PlayerMana> optional = LazyOptional.of(this::createPlayerMana);

    // Создаем объект данных, если он еще не существует
    private PlayerMana createPlayerMana() {
        if (this.playerMana == null) {
            this.playerMana = new PlayerMana();
        }
        return this.playerMana;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_MANA) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    // Сохранение данных в файл мира при выходе игрока или автосохранении
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        PlayerMana manaData = createPlayerMana();

        // Записываем ману
        nbt.putFloat("mana", manaData.getMana());
        // Записываем выбранную стихию (0, 1, 2, 3 или 4)
        nbt.putInt("player_element", manaData.getElement());

        return nbt;
    }

    // Загрузка данных из файла мира при входе игрока
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        PlayerMana manaData = createPlayerMana();

        // Считываем ману
        manaData.setMana(nbt.getFloat("mana"));
        // Считываем стихию (если игрока еще нет в базе, Майнкрафт вернет 0)
        manaData.setElement(nbt.getInt("player_element"));
    }
}
