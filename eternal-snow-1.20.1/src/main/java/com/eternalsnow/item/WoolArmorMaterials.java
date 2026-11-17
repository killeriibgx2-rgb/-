package com.eternalsnow.item;

import com.eternalsnow.EternalSnowMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/**
 * v2.6.6: knitted wool / cloth armour materials (1.20.1 ArmorMaterial is a getName()-based
 * interface, so every colour needs its own material name — that name is also the texture
 * path: textures/models/armor/&lt;name&gt;_layer_1.png).
 *
 * Defence is deliberately poor (wool is not armour), but ArmorInsulationHelper treats these
 * materials as "naturally warm", which is the whole point of wearing them.
 */
public final class WoolArmorMaterials {

    private static final int[] DURABILITY_MULT = {11, 16, 15, 13}; // helmet, chest, legs, boots (x8)
    private static final int[] WOOL_DEFENCE = {1, 2, 2, 1};
    private static final int[] CLOTH_DEFENCE = {0, 1, 1, 0};

    public static final ArmorMaterial WOOL = new Cloth("wool", WOOL_DEFENCE, 5, SoundEvents.ARMOR_EQUIP_LEATHER);
    public static final ArmorMaterial WOOL_RED = new Cloth("wool_red", WOOL_DEFENCE, 5, SoundEvents.ARMOR_EQUIP_LEATHER);
    public static final ArmorMaterial WOOL_BLUE = new Cloth("wool_blue", WOOL_DEFENCE, 5, SoundEvents.ARMOR_EQUIP_LEATHER);
    public static final ArmorMaterial WOOL_GREEN = new Cloth("wool_green", WOOL_DEFENCE, 5, SoundEvents.ARMOR_EQUIP_LEATHER);
    public static final ArmorMaterial WOOL_YELLOW = new Cloth("wool_yellow", WOOL_DEFENCE, 5, SoundEvents.ARMOR_EQUIP_LEATHER);
    public static final ArmorMaterial WOOL_PURPLE = new Cloth("wool_purple", WOOL_DEFENCE, 5, SoundEvents.ARMOR_EQUIP_LEATHER);

    public static final ArmorMaterial JACKET_LEATHER = new Cloth("jacket_leather", CLOTH_DEFENCE, 8, SoundEvents.ARMOR_EQUIP_LEATHER);
    public static final ArmorMaterial JACKET_WOOL = new Cloth("jacket_wool", CLOTH_DEFENCE, 6, SoundEvents.ARMOR_EQUIP_LEATHER);
    public static final ArmorMaterial JACKET_PADDED = new Cloth("jacket_padded", CLOTH_DEFENCE, 7, SoundEvents.ARMOR_EQUIP_LEATHER);

    private WoolArmorMaterials() {
    }

    /**
     * HumanoidArmorLayer builds the texture path as "textures/models/armor/" + materialName +
     * "_layer_1.png" and parses it WITHOUT a namespace, so a custom material name has to carry
     * its own namespace ("eternalsnow:wool" -> assets/eternalsnow/.../wool_layer_1.png).
     */
    private static String namespaced(String id) {
        return EternalSnowMod.MODID + ":" + id;
    }

    /** Strips the namespace so the warmth table keeps reading like item ids. */
    public static String bareName(String materialName) {
        if (materialName == null) {
            return "";
        }
        int idx = materialName.indexOf(':');
        return idx >= 0 ? materialName.substring(idx + 1) : materialName;
    }

    /** Warmth points per piece: a full set is 4 pieces, so multiply by 4 for the total. */
    public static float warmthPerPiece(String materialName) {
        String name = bareName(materialName);
        if (name.isEmpty()) {
            return 0.0F;
        }
        return switch (name) {
            case "jacket_padded" -> 2.40F;
            case "jacket_wool" -> 2.00F;
            case "jacket_leather" -> 1.60F;
            default -> name.startsWith("wool") ? 1.60F : 0.0F;
        };
    }

    public static boolean isWoolOrCloth(String materialName) {
        return warmthPerPiece(materialName) > 0.0F;
    }

    private static final class Cloth implements ArmorMaterial {
        private final String name;
        private final int[] defence;
        private final int enchantmentValue;
        private final Supplier<SoundEvent> equipSound;
        private final Ingredient repairIngredient = Ingredient.of(ItemTags.WOOL);

        private Cloth(String name, int[] defence, int enchantmentValue, SoundEvent equipSound) {
            this.name = namespaced(name);
            this.defence = defence;
            this.enchantmentValue = enchantmentValue;
            this.equipSound = () -> equipSound;
        }

        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return DURABILITY_MULT[type.ordinal()] * 8;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return this.defence[type.ordinal()];
        }

        @Override
        public int getEnchantmentValue() {
            return this.enchantmentValue;
        }

        @Override
        public SoundEvent getEquipSound() {
            return this.equipSound.get();
        }

        @Override
        public Ingredient getRepairIngredient() {
            return this.repairIngredient;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public float getToughness() {
            return 0.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    }
}
