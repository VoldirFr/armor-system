package fr.voldir.armorsystem.core;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum Mineral {
    QUARTZ("quartz", "minecraft:quartz", 0xE3D7C0, 8, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 12, 20, false),
    REDSTONE("redstone", "minecraft:redstone", 0x9C1A00, 9, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 13, 18, false),
    LAPIS("lapis", "minecraft:lapis_lazuli", 0x2A4BA0, 10, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 14, 22, false),
    EMERALD("emerald", "minecraft:emerald", 0x17DD62, 11, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 16, 24, false),
    COPPER("copper", "minecraft:copper_ingot", 0xB4684B, 12, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 18, 14, false),
    GOLD("gold", "minecraft:gold_ingot", 0xFCEE4B, 13, 1.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 10, 30, false),
    IRON("iron", "minecraft:iron_ingot", 0xD8D8D8, 15, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20, 9, false),
    DIAMOND("diamond", "minecraft:diamond", 0x6EECD2, 20, 3.0, 0.1, 0.0, 0.0, 0.0, 0.0, 0.0, 33, 10, false),
    NETHERITE("netherite", "minecraft:netherite_ingot", 0x625859, 22, 4.0, 0.15, 0.0, 0.0, 0.0, 0.0, 0.0, 40, 15, true);

    private final String id;
    private final String ingotId;
    private final int defaultColor;
    private final int armorTotal;
    private final double toughness;
    private final double knockbackResistance;
    private final double movementSpeed;
    private final double maxHealth;
    private final double attackSpeed;
    private final double luck;
    private final double attackDamagePct;
    private final int durabilityBase;
    private final int enchantability;
    private final boolean fireResistant;

    Mineral(String id, String ingotId, int defaultColor, int armorTotal, double toughness, double knockbackResistance,
            double movementSpeed, double maxHealth, double attackSpeed, double luck, double attackDamagePct,
            int durabilityBase, int enchantability, boolean fireResistant) {
        this.id = id;
        this.ingotId = ingotId;
        this.defaultColor = defaultColor;
        this.armorTotal = armorTotal;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.movementSpeed = movementSpeed;
        this.maxHealth = maxHealth;
        this.attackSpeed = attackSpeed;
        this.luck = luck;
        this.attackDamagePct = attackDamagePct;
        this.durabilityBase = durabilityBase;
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

    public int defaultColor() {
        return defaultColor;
    }

    public int armorTotal() {
        return armorTotal;
    }

    public double toughness() {
        return toughness;
    }

    public double knockbackResistance() {
        return knockbackResistance;
    }

    public double movementSpeed() {
        return movementSpeed;
    }

    public double maxHealth() {
        return maxHealth;
    }

    public double attackSpeed() {
        return attackSpeed;
    }

    public double luck() {
        return luck;
    }

    public double attackDamagePct() {
        return attackDamagePct;
    }

    public int durabilityBase() {
        return durabilityBase;
    }

    public int enchantability() {
        return enchantability;
    }

    public boolean fireResistant() {
        return fireResistant;
    }

    public static Mineral byIngotId(String ingotId) {
        for (Mineral mineral : values()) {
            if (mineral.ingotId.equals(ingotId)) return mineral;
        }
        return null;
    }

    public static Mineral byIngotItem(Item item) {
        if (item == null || item == Items.AIR) return null;
        return byIngotId(BuiltInRegistries.ITEM.getKey(item).toString());
    }
}
