package fr.voldir.armorsystem.event;

import fr.voldir.armorsystem.ArmorSystem;
import fr.voldir.armorsystem.config.ArmorSystemConfig;
import fr.voldir.armorsystem.core.ArmorSet;
import fr.voldir.armorsystem.item.ArmorSetItem;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ArmorSystem.MOD_ID)
public class ArmorSetBonusHandler {

    private static final ResourceLocation MANA_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath(ArmorSystem.MOD_ID, "set_bonus_mana");
    private static final ResourceLocation ATTACK_SPEED_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath(ArmorSystem.MOD_ID, "set_bonus_attack_speed");

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.tickCount % 10 != 0) return;

        double mana = 0.0;
        double attackSpeed = 0.0;
        if (ArmorSystemConfig.setBonusEnabled()) {
            ArmorSet fullSet = fullSetOf(player);
            if (fullSet != null) {
                switch (fullSet.subclass()) {
                    case MAGIC -> mana = ArmorSystemConfig.magicMana();
                    case HYBRID -> {
                        mana = ArmorSystemConfig.hybridMana();
                        attackSpeed = ArmorSystemConfig.hybridAttackSpeedPct();
                    }
                    case PHYSICAL -> attackSpeed = ArmorSystemConfig.physicalAttackSpeedPct();
                }
            }
        }
        apply(player, AttributeRegistry.MAX_MANA, MANA_BONUS_ID, mana, AttributeModifier.Operation.ADD_VALUE);
        apply(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_BONUS_ID, attackSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static ArmorSet fullSetOf(Player player) {
        ArmorSet found = null;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorSetItem armorItem)) return null;
            if (found == null) found = armorItem.getSet();
            else if (!found.equals(armorItem.getSet())) return null;
        }
        return found;
    }

    private static void apply(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        if (amount != 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }
}
