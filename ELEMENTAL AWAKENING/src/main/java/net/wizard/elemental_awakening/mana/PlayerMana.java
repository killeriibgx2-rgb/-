package net.wizard.elemental_awakening.mana;

public class PlayerMana {
    private float mana = 100.0F;
    private float maxMana = 100.0F;

    // Новая переменная для хранения выбранной стихии
    // 0 = Еще не выбрана (Обычный человек)
    // 1 = Земля, 2 = Огонь, 3 = Вода, 4 = Воздух
    private int element = 0;

    // --- СТАНДАРТНЫЕ МЕТОДЫ ДЛЯ МАНЫ ---

    public float getMana() {
        return mana;
    }

    public void setMana(float mana) {
        // Math.max и Math.min защищают ману от ухода в минус или превышения лимита
        this.mana = Math.max(0, Math.min(mana, maxMana));
    }

    public float getMaxMana() {
        return maxMana;
    }

    public void consumeMana(float amount) {
        this.mana = Math.max(0, this.mana - amount);
    }

    public void regenMana() {
        if (this.mana < this.maxMana) {
            this.mana = Math.min(this.maxMana, this.mana + 0.2F);
        }
    }

    // --- НОВЫЕ МЕТОДЫ ДЛЯ СТИХИЙ (ЭЛЕМЕНТОВ) ---

    // Этот метод вызывается, когда игре нужно узнать стихию игрока
    public int getElement() {
        return this.element;
    }

    // Этот метод вызывается, когда сервер записывает игроку выбранную стихию
    public void setElement(int element) {
        this.element = element;
    }
}
