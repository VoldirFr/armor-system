package fr.voldir.armorsystem.config;

import fr.voldir.armorsystem.core.ArmorTier;
import fr.voldir.armorsystem.core.Mineral;
import java.util.List;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ArmorSystemConfig {

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.ConfigValue<Double>[] TIER_ATTACK_BONUS = new ModConfigSpec.ConfigValue[ArmorTier.values().length];
    private static final ModConfigSpec.ConfigValue<Double>[] TIER_ARMOR_BONUS = new ModConfigSpec.ConfigValue[ArmorTier.values().length];
    private static final ModConfigSpec.ConfigValue<Double>[] TIER_TOUGHNESS_BONUS = new ModConfigSpec.ConfigValue[ArmorTier.values().length];

    private static final ModConfigSpec.ConfigValue<String>[] MINERAL_INGOT = new ModConfigSpec.ConfigValue[Mineral.values().length];
    private static final ModConfigSpec.ConfigValue<String>[] MINERAL_COLOR = new ModConfigSpec.ConfigValue[Mineral.values().length];
    private static final ModConfigSpec.ConfigValue<Integer>[] MINERAL_ARMOR = new ModConfigSpec.ConfigValue[Mineral.values().length];
    private static final ModConfigSpec.ConfigValue<Double>[] MINERAL_TOUGHNESS = new ModConfigSpec.ConfigValue[Mineral.values().length];
    private static final ModConfigSpec.ConfigValue<Double>[] MINERAL_KNOCKBACK = new ModConfigSpec.ConfigValue[Mineral.values().length];
    private static final ModConfigSpec.ConfigValue<Double>[] MINERAL_MOVE_SPEED = new ModConfigSpec.ConfigValue[Mineral.values().length];
    private static final ModConfigSpec.ConfigValue<Double>[] MINERAL_MAX_HEALTH = new ModConfigSpec.ConfigValue[Mineral.values().length];
    private static final ModConfigSpec.ConfigValue<Double>[] MINERAL_ATTACK_SPEED = new ModConfigSpec.ConfigValue[Mineral.values().length];
    private static final ModConfigSpec.ConfigValue<Double>[] MINERAL_LUCK = new ModConfigSpec.ConfigValue[Mineral.values().length];
    private static final ModConfigSpec.ConfigValue<Double>[] MINERAL_ATTACK_PCT = new ModConfigSpec.ConfigValue[Mineral.values().length];
    private static final ModConfigSpec.ConfigValue<Integer>[] MINERAL_DURABILITY = new ModConfigSpec.ConfigValue[Mineral.values().length];
    private static final ModConfigSpec.ConfigValue<Integer>[] MINERAL_ENCHANT = new ModConfigSpec.ConfigValue[Mineral.values().length];

    private static ModConfigSpec.ConfigValue<Integer> INGOTS_PER_ARMOR;
    private static ModConfigSpec.ConfigValue<String> DEFAULT_MATERIAL;

    private static ModConfigSpec.ConfigValue<Boolean> SET_BONUS_ENABLED;
    private static ModConfigSpec.ConfigValue<Double> MAGIC_MANA;
    private static ModConfigSpec.ConfigValue<Double> HYBRID_MANA;
    private static ModConfigSpec.ConfigValue<Double> HYBRID_ATTACK_SPEED;
    private static ModConfigSpec.ConfigValue<Double> PHYSICAL_ATTACK_SPEED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Armor System - 27 archetypes (9 weight tiers x 3 subclasses) x any material = full grid.",
                "Generated sorted at config/armorsystem-common.toml; every value below is editable.").push("tiers");
        for (ArmorTier tier : ArmorTier.values()) {
            builder.comment("Weight tier '" + tier.id() + "' (index " + tier.index() + ") - bonuses applied per piece, multiplicative.").push(tier.id());
            TIER_ATTACK_BONUS[tier.index()] = builder.comment("Attack damage bonus per piece, in percent (0.10 = +10%). Default: +10% ultra light, -1.25% per tier step, +0% ultra heavy.")
                    .defineInRange("attackDamagePct", round4(0.10 - 0.0125 * tier.index()), 0.0, 10.0);
            TIER_ARMOR_BONUS[tier.index()] = builder.comment("Armor bonus per piece, in percent (0.06 = +6%). Default: +6% ultra heavy, -0.75% per step, 0% ultra light.")
                    .defineInRange("armorPct", round4(0.0075 * tier.index()), 0.0, 10.0);
            TIER_TOUGHNESS_BONUS[tier.index()] = builder.comment("Armor toughness bonus per piece, in percent (0.03 = +3%). Default: +3% ultra heavy, -0.375% per step, 0% ultra light.")
                    .defineInRange("toughnessPct", round4(0.00375 * tier.index()), 0.0, 10.0);
            builder.pop();
        }
        builder.pop();

        builder.comment("Materials (vanilla minerals). The ingot item chosen at craft time is stored on the armor stack:",
                "its hex color (texture average, or colorHex override below) tints the 'color_layer' layer,",
                "and these values define the final power of the crafted armor piece.").push("minerals");
        for (Mineral mineral : Mineral.values()) {
            builder.comment("Material '" + mineral.id() + "'.").push(mineral.id());
            MINERAL_INGOT[mineral.ordinal()] = builder.comment("Source ingot item stored on the armor stack when crafting with it.").define("ingot", mineral.ingotId());
            MINERAL_COLOR[mineral.ordinal()] = builder.comment("Hex color (#RRGGBB) used as color shift on the color layer. Empty = computed from the ingot item texture (average of all pixels).").define("colorHex", "");
            MINERAL_ARMOR[mineral.ordinal()] = builder.comment("Armor points of the full set (distributed 15/40/30/15 over helmet/chestplate/leggings/boots).").defineInRange("armor", mineral.armorTotal(), 0, 100);
            MINERAL_TOUGHNESS[mineral.ordinal()] = builder.comment("Armor toughness per piece.").defineInRange("armorToughness", mineral.toughness(), 0.0, 20.0);
            MINERAL_KNOCKBACK[mineral.ordinal()] = builder.comment("Knockback resistance per piece (0..1).").defineInRange("knockbackResistance", mineral.knockbackResistance(), 0.0, 1.0);
            MINERAL_MOVE_SPEED[mineral.ordinal()] = builder.comment("Movement speed per piece, percent (0.01 = +1%).").defineInRange("movementSpeed", mineral.movementSpeed(), -1.0, 1.0);
            MINERAL_MAX_HEALTH[mineral.ordinal()] = builder.comment("Max health per piece (flat).").defineInRange("maxHealth", mineral.maxHealth(), -100.0, 100.0);
            MINERAL_ATTACK_SPEED[mineral.ordinal()] = builder.comment("Attack speed per piece, percent (0.05 = +5%).").defineInRange("attackSpeed", mineral.attackSpeed(), -1.0, 1.0);
            MINERAL_LUCK[mineral.ordinal()] = builder.comment("Luck per piece (flat).").defineInRange("luck", mineral.luck(), -10.0, 10.0);
            MINERAL_ATTACK_PCT[mineral.ordinal()] = builder.comment("Attack damage per piece, percent (0.075 = +7.5%, ADD_MULTIPLIED_TOTAL), added to the tier bonus.").defineInRange("attackDamagePct", mineral.attackDamagePct(), 0.0, 10.0);
            MINERAL_DURABILITY[mineral.ordinal()] = builder.comment("Base durability (x vanilla slot multiplier 11/16/15/13).").defineInRange("durability", mineral.durabilityBase(), 1, 100000);
            MINERAL_ENCHANT[mineral.ordinal()] = builder.comment("Enchantability.").defineInRange("enchantability", mineral.enchantability(), 0, 100);
            builder.pop();
        }
        builder.pop();

        builder.comment("Crafting: base armor template (tag armorsystem:base_armors) + ingots -> set piece with the ingot as material.").push("craft");
        INGOTS_PER_ARMOR = builder.comment("Number of ingots required per crafted armor piece.").defineInRange("ingotsPerArmor", 4, 1, 64);
        DEFAULT_MATERIAL = builder.comment("Ingot item used as material for armor stacks that have no material component yet.").define("defaultMaterial", "minecraft:iron_ingot");
        builder.pop();

        builder.comment("Full-set bonuses, applied only when the 4 pieces of the same archetype are equipped, displayed at the bottom of the tooltip.").push("setBonus");
        SET_BONUS_ENABLED = builder.comment("Master switch for full-set bonuses.").define("enabled", true);
        MAGIC_MANA = builder.comment("Magic subclass full set: +max mana (irons_spellbooks:max_mana).").defineInRange("magicMana", 200.0, 0.0, 100000.0);
        HYBRID_MANA = builder.comment("Hybrid subclass full set: +max mana.").defineInRange("hybridMana", 100.0, 0.0, 100000.0);
        HYBRID_ATTACK_SPEED = builder.comment("Hybrid subclass full set: +attack speed, percent (0.15 = +15%).").defineInRange("hybridAttackSpeedPct", 0.15, 0.0, 10.0);
        PHYSICAL_ATTACK_SPEED = builder.comment("Physical subclass full set: +attack speed, percent (0.30 = +30%).").defineInRange("physicalAttackSpeedPct", 0.30, 0.0, 10.0);
        builder.pop();

        SPEC = builder.build();
    }

    private static double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    public static double tierAttackBonus(int tierIndex) {
        return TIER_ATTACK_BONUS[tierIndex].get();
    }

    public static double tierArmorBonus(int tierIndex) {
        return TIER_ARMOR_BONUS[tierIndex].get();
    }

    public static double tierToughnessBonus(int tierIndex) {
        return TIER_TOUGHNESS_BONUS[tierIndex].get();
    }

    public static String mineralIngot(Mineral mineral) {
        String configured = MINERAL_INGOT[mineral.ordinal()].get();
        return configured == null || configured.isBlank() ? mineral.ingotId() : configured;
    }

    public static String mineralColorHex(Mineral mineral) {
        return MINERAL_COLOR[mineral.ordinal()].get();
    }

    public static int mineralArmor(Mineral mineral) {
        return MINERAL_ARMOR[mineral.ordinal()].get();
    }

    public static double mineralToughness(Mineral mineral) {
        return MINERAL_TOUGHNESS[mineral.ordinal()].get();
    }

    public static double mineralKnockbackResistance(Mineral mineral) {
        return MINERAL_KNOCKBACK[mineral.ordinal()].get();
    }

    public static double mineralMovementSpeed(Mineral mineral) {
        return MINERAL_MOVE_SPEED[mineral.ordinal()].get();
    }

    public static double mineralMaxHealth(Mineral mineral) {
        return MINERAL_MAX_HEALTH[mineral.ordinal()].get();
    }

    public static double mineralAttackSpeed(Mineral mineral) {
        return MINERAL_ATTACK_SPEED[mineral.ordinal()].get();
    }

    public static double mineralLuck(Mineral mineral) {
        return MINERAL_LUCK[mineral.ordinal()].get();
    }

    public static double mineralAttackDamagePct(Mineral mineral) {
        return MINERAL_ATTACK_PCT[mineral.ordinal()].get();
    }

    public static int mineralDurability(Mineral mineral) {
        return MINERAL_DURABILITY[mineral.ordinal()].get();
    }

    public static int mineralEnchantability(Mineral mineral) {
        return MINERAL_ENCHANT[mineral.ordinal()].get();
    }

    public static int ingotsPerArmor() {
        return INGOTS_PER_ARMOR.get();
    }

    public static String defaultMaterial() {
        return DEFAULT_MATERIAL.get();
    }

    public static boolean setBonusEnabled() {
        return SET_BONUS_ENABLED.get();
    }

    public static double magicMana() {
        return MAGIC_MANA.get();
    }

    public static double hybridMana() {
        return HYBRID_MANA.get();
    }

    public static double hybridAttackSpeedPct() {
        return HYBRID_ATTACK_SPEED.get();
    }

    public static double physicalAttackSpeedPct() {
        return PHYSICAL_ATTACK_SPEED.get();
    }

    public static int armorForSlot(Mineral mineral, ArmorItem.Type type) {
        double weight = switch (type) {
            case HELMET -> 0.15;
            case CHESTPLATE -> 0.40;
            case LEGGINGS -> 0.30;
            case BOOTS -> 0.15;
            default -> 0.0;
        };
        return Math.max(1, (int) Math.round(mineralArmor(mineral) * weight));
    }

    public static Mineral mineralForIngotId(String ingotId) {
        if (ingotId != null) {
            for (Mineral mineral : Mineral.values()) {
                if (mineralIngot(mineral).equals(ingotId)) return mineral;
            }
            Mineral byDefault = Mineral.byIngotId(ingotId);
            if (byDefault != null) return byDefault;
        }
        String fallback = defaultMaterial();
        if (fallback != null && !fallback.equals(ingotId)) return mineralForIngotId(fallback);
        return Mineral.IRON;
    }

    public static int parseHexColor(String hex) {
        if (hex == null) return -1;
        String value = hex.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (value.length() != 6) return -1;
        try {
            return Integer.parseInt(value, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
