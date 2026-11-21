package fr.voldir.armorsystem.config;

import fr.voldir.armorsystem.core.ArmorSubclass;
import fr.voldir.armorsystem.core.ArmorTier;
import fr.voldir.armorsystem.core.Mineral;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
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
    private static final ModConfigSpec.ConfigValue<Integer>[] MINERAL_DURABILITY = new ModConfigSpec.ConfigValue[Mineral.values().length];
    private static final ModConfigSpec.ConfigValue<Double>[] MINERAL_ATTACK = new ModConfigSpec.ConfigValue[Mineral.values().length];
    private static final ModConfigSpec.ConfigValue<Double>[] MINERAL_KNOCKBACK = new ModConfigSpec.ConfigValue[Mineral.values().length];
    private static final ModConfigSpec.ConfigValue<Integer>[] MINERAL_ENCHANT = new ModConfigSpec.ConfigValue[Mineral.values().length];
    private static final ModConfigSpec.ConfigValue<Integer>[] MINERAL_TIER = new ModConfigSpec.ConfigValue[Mineral.values().length];

    private static ModConfigSpec.ConfigValue<Integer> INGOTS_PER_ARMOR;
    private static ModConfigSpec.ConfigValue<List<? extends String>> BASE_ARMOR_SUBCLASS;
    private static ModConfigSpec.ConfigValue<List<? extends String>> INGOT_TIER;

    private static ModConfigSpec.ConfigValue<Boolean> SET_BONUS_ENABLED;
    private static ModConfigSpec.ConfigValue<Double> MAGIC_MANA;
    private static ModConfigSpec.ConfigValue<Double> HYBRID_MANA;
    private static ModConfigSpec.ConfigValue<Double> HYBRID_ATTACK_SPEED;
    private static ModConfigSpec.ConfigValue<Double> PHYSICAL_ATTACK_SPEED;

    private static List<? extends String> lastIngotTierRaw;
    private static Map<String, Integer> ingotTierMap;
    private static List<? extends String> lastBaseSubclassRaw;
    private static Map<String, String> baseSubclassMap;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Armor System - 9 weight tiers x 3 subclasses = 27 armor sets").push("tiers");
        for (ArmorTier tier : ArmorTier.values()) {
            builder.push(tier.id());
            TIER_ATTACK_BONUS[tier.index()] = builder.defineInRange("attackBonusPct", 0.10 - 0.0125 * tier.index(), 0.0, 10.0);
            TIER_ARMOR_BONUS[tier.index()] = builder.defineInRange("armorBonusPct", 0.0075 * tier.index(), 0.0, 10.0);
            TIER_TOUGHNESS_BONUS[tier.index()] = builder.defineInRange("toughnessBonusPct", 0.00375 * tier.index(), 0.0, 10.0);
            builder.pop();
        }
        builder.pop();

        builder.comment("Vanilla minerals used as ingot source (color hex + final power of crafted armor)").push("minerals");
        for (Mineral mineral : Mineral.values()) {
            builder.push(mineral.id());
            MINERAL_INGOT[mineral.ordinal()] = builder.define("ingot", mineral.ingotId());
            MINERAL_COLOR[mineral.ordinal()] = builder.define("colorHex", "");
            MINERAL_ARMOR[mineral.ordinal()] = builder.defineInRange("armor", mineral.armorTotal(), 0, 100);
            MINERAL_TOUGHNESS[mineral.ordinal()] = builder.defineInRange("toughness", mineral.toughness(), 0.0, 20.0);
            MINERAL_DURABILITY[mineral.ordinal()] = builder.defineInRange("durability", mineral.durabilityBase(), 1, 100000);
            MINERAL_ATTACK[mineral.ordinal()] = builder.defineInRange("attackDamage", mineral.attackDamage(), 0.0, 100.0);
            MINERAL_KNOCKBACK[mineral.ordinal()] = builder.defineInRange("knockbackResistance", mineral.knockbackResistance(), 0.0, 1.0);
            MINERAL_ENCHANT[mineral.ordinal()] = builder.defineInRange("enchantability", mineral.enchantability(), 0, 100);
            MINERAL_TIER[mineral.ordinal()] = builder.defineInRange("tier", mineral.tierIndex(), 0, ArmorTier.values().length - 1);
            builder.pop();
        }
        builder.pop();

        builder.comment("Crafting: base armor + ingots -> set piece").push("craft");
        INGOTS_PER_ARMOR = builder.defineInRange("ingotsPerArmor", 4, 1, 64);
        BASE_ARMOR_SUBCLASS = builder.defineList("baseArmorSubclass", List.of(
                "minecraft:leather=physical",
                "minecraft:chainmail=physical",
                "minecraft:iron=hybrid",
                "minecraft:gold=hybrid",
                "minecraft:diamond=magic",
                "minecraft:netherite=magic"
        ), o -> o instanceof String);
        INGOT_TIER = builder.defineList("ingotTier", List.of(
                "minecraft:quartz=0",
                "minecraft:redstone=1",
                "minecraft:lapis_lazuli=2",
                "minecraft:emerald=3",
                "minecraft:copper_ingot=4",
                "minecraft:gold_ingot=5",
                "minecraft:iron_ingot=6",
                "minecraft:diamond=7",
                "minecraft:netherite_ingot=8"
        ), o -> o instanceof String);
        builder.pop();

        builder.comment("Full-set bonuses (only when the 4 pieces of the same set are equipped)").push("setBonus");
        SET_BONUS_ENABLED = builder.define("enabled", true);
        MAGIC_MANA = builder.defineInRange("magicMana", 200.0, 0.0, 100000.0);
        HYBRID_MANA = builder.defineInRange("hybridMana", 100.0, 0.0, 100000.0);
        HYBRID_ATTACK_SPEED = builder.defineInRange("hybridAttackSpeedPct", 0.15, 0.0, 10.0);
        PHYSICAL_ATTACK_SPEED = builder.defineInRange("physicalAttackSpeedPct", 0.30, 0.0, 10.0);
        builder.pop();

        SPEC = builder.build();
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

    public static int mineralDurability(Mineral mineral) {
        return MINERAL_DURABILITY[mineral.ordinal()].get();
    }

    public static double mineralAttackDamage(Mineral mineral) {
        return MINERAL_ATTACK[mineral.ordinal()].get();
    }

    public static double mineralKnockbackResistance(Mineral mineral) {
        return MINERAL_KNOCKBACK[mineral.ordinal()].get();
    }

    public static int mineralEnchantability(Mineral mineral) {
        return MINERAL_ENCHANT[mineral.ordinal()].get();
    }

    public static int ingotsPerArmor() {
        return INGOTS_PER_ARMOR.get();
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

    public static int tierOfItem(Item item) {
        Map<String, Integer> map = ingotTierMap();
        Integer tier = map.get(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString());
        if (tier != null) return tier;
        Mineral mineral = Mineral.byIngotItem(item);
        return mineral == null ? -1 : MINERAL_TIER[mineral.ordinal()].get();
    }

    public static ArmorSubclass subclassOf(Holder<ArmorMaterial> baseArmorMaterial) {
        Map<String, String> map = baseSubclassMap();
        String key = baseArmorMaterial.getKey() == null ? "" : baseArmorMaterial.getKey().location().toString();
        String subclass = map.get(key);
        if (subclass == null) {
            String path = baseArmorMaterial.getKey() == null ? "" : baseArmorMaterial.getKey().location().getPath();
            if (path.contains("leather") || path.contains("chain")) subclass = ArmorSubclass.PHYSICAL.id();
            else if (path.contains("iron") || path.contains("gold")) subclass = ArmorSubclass.HYBRID.id();
            else subclass = ArmorSubclass.MAGIC.id();
        }
        return ArmorSubclass.byId(subclass);
    }

    private static Map<String, Integer> ingotTierMap() {
        List<? extends String> raw = INGOT_TIER.get();
        if (raw != lastIngotTierRaw) {
            Map<String, Integer> map = new HashMap<>();
            for (String entry : raw) {
                String[] parts = entry.split("=", 2);
                if (parts.length == 2) {
                    try {
                        map.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            ingotTierMap = map;
            lastIngotTierRaw = raw;
        }
        return ingotTierMap;
    }

    private static Map<String, String> baseSubclassMap() {
        List<? extends String> raw = BASE_ARMOR_SUBCLASS.get();
        if (raw != lastBaseSubclassRaw) {
            Map<String, String> map = new HashMap<>();
            for (String entry : raw) {
                String[] parts = entry.split("=", 2);
                if (parts.length == 2) map.put(parts[0].trim(), parts[1].trim());
            }
            baseSubclassMap = map;
            lastBaseSubclassRaw = raw;
        }
        return baseSubclassMap;
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
