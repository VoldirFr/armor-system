package fr.voldir.armorsystem.registry;

import fr.voldir.armorsystem.ArmorSystem;
import fr.voldir.armorsystem.core.ArmorSet;
import fr.voldir.armorsystem.core.Mineral;
import fr.voldir.armorsystem.item.ArmorSetItem;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, ArmorSystem.MOD_ID);

    public static final ResourceLocation BASE_LAYER_ASSET = ResourceLocation.withDefaultNamespace("iron");
    public static final ResourceLocation LIGHT_SHIFT_ASSET = ResourceLocation.fromNamespaceAndPath(ArmorSystem.MOD_ID, "light_shift");
    public static final ResourceLocation LIGHT_LAYER1_ASSET = ResourceLocation.fromNamespaceAndPath(ArmorSystem.MOD_ID, "light_layer1");

    private static final Map<String, DeferredHolder<ArmorMaterial, ArmorMaterial>> HOLDERS = new HashMap<>();

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        for (ArmorSet set : ArmorSet.all()) {
            HOLDERS.put(set.id(), ARMOR_MATERIALS.register(set.id(), () -> create(set)));
        }
        ARMOR_MATERIALS.register(modEventBus);
    }

    public static DeferredHolder<ArmorMaterial, ArmorMaterial> holder(ArmorSet set) {
        return HOLDERS.get(set.id());
    }

    private static ArmorMaterial create(ArmorSet set) {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            double weight = switch (type) {
                case HELMET -> 0.15;
                case CHESTPLATE -> 0.40;
                case LEGGINGS -> 0.30;
                case BOOTS -> 0.15;
                default -> 0.0;
            };
            defense.put(type, Math.max(1, (int) Math.round(Mineral.IRON.armorTotal() * weight)));
        }
        // Light tiers use the custom 3D model with two layers: the hue-shifted garment
        // (shift texture, re-colored per mineral) and the fixed decoration layer on top.
        List<ArmorMaterial.Layer> layers = set.tier().usesLightModel()
                ? List.of(
                        new ArmorMaterial.Layer(LIGHT_SHIFT_ASSET, "", false),
                        new ArmorMaterial.Layer(LIGHT_LAYER1_ASSET, "", false))
                : List.of(
                        new ArmorMaterial.Layer(BASE_LAYER_ASSET, "", true),
                        new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ArmorSystem.MOD_ID, ArmorSetItem.COLOR_LAYER_NAME), "", true));
        return new ArmorMaterial(
                defense,
                Mineral.IRON.enchantability(),
                equipSound(),
                () -> Ingredient.of(ModTags.ARMOR_INGOTS),
                layers,
                (float) Mineral.IRON.toughness(),
                (float) Mineral.IRON.knockbackResistance()
        );
    }

    private static Holder<SoundEvent> equipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }
}
