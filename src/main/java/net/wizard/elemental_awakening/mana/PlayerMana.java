package net.wizard.elemental_awakening.mana;

public class PlayerMana {
    // Переменные для хранения текущей маны и максимальной
    private float mana = 100.0F;
    private final float maxMana = 100.0F;

    public float getMana() {
        return mana;
    }

    public float getMaxMana() {
        return maxMana;
    }

    // Метод для траты маны (например, 10 единиц на стену)
    public void consumeMana(float amount) {
        this.mana = Math.max(0.0F, this.mana - amount);
    }

    // Метод для регенерации маны (0.2 за тик, как мы считали)
    public void regenMana() {
        if (this.mana < this.maxMana) {
            this.mana = Math.min(this.maxMana, this.mana + 0.2F);
        }
    }

    // Метод, чтобы насильно установить ману (пригодится при синхронизации)
    public void setMana(float amount) {
        this.mana = amount;
    }
}
