package fr.voldir.armorsystem.item;

import fr.voldir.armorsystem.ArmorSystem;
import fr.voldir.armorsystem.config.ArmorSystemConfig;
import fr.voldir.armorsystem.core.ArmorSet;
import fr.voldir.armorsystem.core.ArmorSubclass;
import fr.voldir.armorsystem.core.Mineral;
import fr.voldir.armorsystem.registry.ModDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class ArmorSetItem extends ArmorItem {

    public static final String COLOR_LAYER_NAME = "color_layer";

    private final ArmorSet set;

    public ArmorSetItem(Holder<ArmorMaterial> material, ArmorItem.Type type, ArmorSet set, Item.Properties properties) {
        super(material, type, withDurability(type, properties));
        this.set = set;
    }

    private static Item.Properties withDurability(ArmorItem.Type type, Item.Properties properties) {
        properties.durability(type.getDurability(Mineral.IRON.durabilityBase()));
        return properties;
    }

    public ArmorSet getSet() {
        return set;
    }

    public Mineral getMineral(ItemStack stack) {
        ResourceLocation id = stack.get(ModDataComponents.MATERIAL.get());
        return ArmorSystemConfig.mineralForIngotId(id == null ? null : id.toString());
    }

    public int getColorHex(ItemStack stack) {
        Mineral mineral = getMineral(stack);
        int parsed = ArmorSystemConfig.parseHexColor(ArmorSystemConfig.mineralColorHex(mineral));
        return parsed >= 0 ? parsed : mineral.defaultColor();
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return type.getDurability(ArmorSystemConfig.mineralDurability(getMineral(stack)));
    }

    @Override
    public int getEnchantmentValue() {
        return ArmorSystemConfig.mineralEnchantability(ArmorSystemConfig.mineralForIngotId(ArmorSystemConfig.defaultMaterial()));
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        Mineral mineral = getMineral(stack);
        double attackPct = ArmorSystemConfig.mineralAttackDamagePct(mineral) + ArmorSystemConfig.tierAttackBonus(set.tier().index());
        double armorPct = ArmorSystemConfig.tierArmorBonus(set.tier().index());
        double toughnessPct = ArmorSystemConfig.tierToughnessBonus(set.tier().index());

        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(type.getSlot());
        String base = "armor_" + type.getName();

        builder.add(Attributes.ARMOR, modifier(base + "_value", ArmorSystemConfig.armorForSlot(mineral, type), AttributeModifier.Operation.ADD_VALUE), group);
        if (armorPct != 0.0) {
            builder.add(Attributes.ARMOR, modifier(base + "_tier", armorPct, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), group);
        }
        builder.add(Attributes.ARMOR_TOUGHNESS, modifier(base + "_toughness_value", ArmorSystemConfig.mineralToughness(mineral), AttributeModifier.Operation.ADD_VALUE), group);
        if (toughnessPct != 0.0) {
            builder.add(Attributes.ARMOR_TOUGHNESS, modifier(base + "_toughness_tier", toughnessPct, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), group);
        }
        double knockback = ArmorSystemConfig.mineralKnockbackResistance(mineral);
        if (knockback != 0.0) {
            builder.add(Attributes.KNOCKBACK_RESISTANCE, modifier("knockback_resistance", knockback, AttributeModifier.Operation.ADD_VALUE), group);
        }
        if (attackPct != 0.0) {
            builder.add(Attributes.ATTACK_DAMAGE, modifier("attack_damage_pct", attackPct, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), group);
        }
        double moveSpeed = ArmorSystemConfig.mineralMovementSpeed(mineral);
        if (moveSpeed != 0.0) {
            builder.add(Attributes.MOVEMENT_SPEED, modifier("movement_speed", moveSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), group);
        }
        double maxHealth = ArmorSystemConfig.mineralMaxHealth(mineral);
        if (maxHealth != 0.0) {
            builder.add(Attributes.MAX_HEALTH, modifier("max_health", maxHealth, AttributeModifier.Operation.ADD_VALUE), group);
        }
        double attackSpeed = ArmorSystemConfig.mineralAttackSpeed(mineral);
        if (attackSpeed != 0.0) {
            builder.add(Attributes.ATTACK_SPEED, modifier("attack_speed", attackSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), group);
        }
        double luck = ArmorSystemConfig.mineralLuck(mineral);
        if (luck != 0.0) {
            builder.add(Attributes.LUCK, modifier("luck", luck, AttributeModifier.Operation.ADD_VALUE), group);
        }
        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Mineral mineral = getMineral(stack);
        String ingotId = ArmorSystemConfig.mineralIngot(mineral);
        Item ingot = BuiltInRegistries.ITEM.get(ResourceLocation.parse(ingotId));
        tooltip.add(Component.translatable("tooltip.armorsystem.material", new ItemStack(ingot).getHoverName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.armorsystem.full_set_bonus", fullSetBonusText()).withStyle(ChatFormatting.GOLD));
    }

    private Component fullSetBonusText() {
        ArmorSubclass subclass = set.subclass();
        return switch (subclass) {
            case MAGIC -> Component.literal("+" + (int) ArmorSystemConfig.magicMana() + " ")
                    .append(Component.translatable("attribute.irons_spellbooks.max_mana"));
            case HYBRID -> Component.literal("+" + (int) ArmorSystemConfig.hybridMana() + " ")
                    .append(Component.translatable("attribute.irons_spellbooks.max_mana"))
                    .append(Component.literal(", +" + (int) (ArmorSystemConfig.hybridAttackSpeedPct() * 100) + "% "))
                    .append(Component.translatable("attribute.name.generic.attack_speed"));
            case PHYSICAL -> Component.literal("+" + (int) (ArmorSystemConfig.physicalAttackSpeedPct() * 100) + "% ")
                    .append(Component.translatable("attribute.name.generic.attack_speed"));
        };
    }

    private static AttributeModifier modifier(String path, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(ResourceLocation.fromNamespaceAndPath(ArmorSystem.MOD_ID, path), amount, operation);
    }
}
