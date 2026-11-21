package fr.voldir.armorsystem.item;

import fr.voldir.armorsystem.ArmorSystem;
import fr.voldir.armorsystem.config.ArmorSystemConfig;
import fr.voldir.armorsystem.core.ArmorSet;
import fr.voldir.armorsystem.core.Mineral;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class ArmorSetItem extends ArmorItem {

    private final ArmorSet set;

    public ArmorSetItem(Holder<ArmorMaterial> material, ArmorItem.Type type, ArmorSet set, Item.Properties properties) {
        super(material, type, withDurability(type, set, properties));
        this.set = set;
    }

    private static Item.Properties withDurability(ArmorItem.Type type, ArmorSet set, Item.Properties properties) {
        properties.durability(type.getDurability(set.mineral().durabilityBase()));
        if (set.mineral().fireResistant()) {
            properties.fireResistant();
        }
        return properties;
    }

    public ArmorSet getSet() {
        return set;
    }

    public Mineral mineral() {
        return set.mineral();
    }

    public int getColorHex() {
        String hex = ArmorSystemConfig.mineralColorHex(mineral());
        int parsed = ArmorSystemConfig.parseHexColor(hex);
        return parsed >= 0 ? parsed : mineral().defaultColor();
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return type.getDurability(ArmorSystemConfig.mineralDurability(mineral()));
    }

    @Override
    public int getEnchantmentValue() {
        return ArmorSystemConfig.mineralEnchantability(mineral());
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        Mineral mineral = mineral();
        double attackBonus = ArmorSystemConfig.tierAttackBonus(set.tier().index());
        double armorBonus = ArmorSystemConfig.tierArmorBonus(set.tier().index());
        double toughnessBonus = ArmorSystemConfig.tierToughnessBonus(set.tier().index());

        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(type.getSlot());
        String base = "armor_" + type.getName();

        builder.add(Attributes.ARMOR, modifier(base + "_value", ArmorSystemConfig.armorForSlot(mineral, type), AttributeModifier.Operation.ADD_VALUE), group);
        if (armorBonus != 0.0) {
            builder.add(Attributes.ARMOR, modifier(base + "_tier", armorBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), group);
        }
        builder.add(Attributes.ARMOR_TOUGHNESS, modifier(base + "_toughness_value", ArmorSystemConfig.mineralToughness(mineral), AttributeModifier.Operation.ADD_VALUE), group);
        if (toughnessBonus != 0.0) {
            builder.add(Attributes.ARMOR_TOUGHNESS, modifier(base + "_toughness_tier", toughnessBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), group);
        }
        double attackDamage = ArmorSystemConfig.mineralAttackDamage(mineral);
        if (attackDamage != 0.0) {
            builder.add(Attributes.ATTACK_DAMAGE, modifier("attack_damage_value", attackDamage, AttributeModifier.Operation.ADD_VALUE), group);
        }
        if (attackBonus != 0.0) {
            builder.add(Attributes.ATTACK_DAMAGE, modifier("attack_damage_tier", attackBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), group);
        }
        double knockback = ArmorSystemConfig.mineralKnockbackResistance(mineral);
        if (knockback != 0.0) {
            builder.add(Attributes.KNOCKBACK_RESISTANCE, modifier("knockback_resistance", knockback, AttributeModifier.Operation.ADD_VALUE), group);
        }
        return builder.build();
    }

    private static AttributeModifier modifier(String path, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(ResourceLocation.fromNamespaceAndPath(ArmorSystem.MOD_ID, path), amount, operation);
    }
}
