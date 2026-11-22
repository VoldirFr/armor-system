package fr.voldir.armorsystem.core;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum Mineral {
    QUARTZ("quartz", "minecraft:quartz", 0, 0xE3D7C0, 8, 0.0, 12, 0.0, 0.0, 20, false),
    REDSTONE("redstone", "minecraft:redstone", 1, 0x9C1A00, 9, 0.0, 13, 0.5, 0.0, 18, false),
    LAPIS("lapis", "minecraft:lapis_lazuli", 2, 0x2A4BA0, 10, 0.5, 14, 0.5, 0.0, 22, false),
    EMERALD("emerald", "minecraft:emerald", 3, 0x17DD62, 11, 1.0, 16, 1.0, 0.0, 24, false),
    COPPER("copper", "minecraft:copper_ingot", 4, 0xB4684B, 12, 1.0, 18, 1.0, 0.0, 14, false),
    GOLD("gold", "minecraft:gold_ingot", 5, 0xFCEE4B, 13, 1.5, 10, 1.0, 0.0, 30, false),
    IRON("iron", "minecraft:iron_ingot", 6, 0xD8D8D8, 15, 2.0, 20, 1.5, 0.0, 9, false),
    DIAMOND("diamond", "minecraft:diamond", 7, 0x6EECD2, 20, 3.0, 33, 2.0, 0.1, 10, false),
    NETHERITE("netherite", "minecraft:netherite_ingot", 8, 0x625859, 22, 4.0, 40, 2.5, 0.15, 15, true);

    private final String id;
    private final String ingotId;
    private final int tierIndex;
    private final int defaultColor;
    private final int armorTotal;
    private final double toughness;
    private final int durabilityBase;
    private final double attackDamage;
    private final double knockbackResistance;
    private final int enchantability;
    private final boolean fireResistant;

    Mineral(String id, String ingotId, int tierIndex, int defaultColor, int armorTotal, double toughness,
            int durabilityBase, double attackDamage, double knockbackResistance, int enchantability, boolean fireResistant) {
        this.id = id;
        this.ingotId = ingotId;
        this.tierIndex = tierIndex;
        this.defaultColor = defaultColor;
        this.armorTotal = armorTotal;
        this.toughness = toughness;
        this.durabilityBase = durabilityBase;
        this.attackDamage = attackDamage;
        this.knockbackResistance = knockbackResistance;
        this.enchantability = enchantability;
        this.fireResistant = fireResistant;
    }

    public String id() {
        return id;
    }

    public String ingotId() {
        return ingotId;
    }

    public Item ingotItem() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(ingotId));
    }

    public int tierIndex() {
        return tierIndex;
    }

    public int defaultColor() {
        return defaultColor;
    }

    public int armorTotal() {
        return armorTotal;
    }

    public double toughness() {
        return toughness;
    }

    public int durabilityBase() {
        return durabilityBase;
    }

    public double attackDamage() {
        return attackDamage;
    }

    public double knockbackResistance() {
        return knockbackResistance;
    }

    public int enchantability() {
        return enchantability;
    }

    public boolean fireResistant() {
        return fireResistant;
    }

    public static Mineral byTierIndex(int tierIndex) {
        for (Mineral mineral : values()) {
            if (mineral.tierIndex == tierIndex) return mineral;
        }
        return null;
    }

    public static Mineral byIngotItem(Item item) {
        if (item == null || item == Items.AIR) return null;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        for (Mineral mineral : values()) {
            if (mineral.ingotId.equals(key.toString())) return mineral;
        }
        return null;
    }
}
