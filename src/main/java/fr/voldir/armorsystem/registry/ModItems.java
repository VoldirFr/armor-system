package fr.voldir.armorsystem.registry;

import fr.voldir.armorsystem.ArmorSystem;
import fr.voldir.armorsystem.core.ArmorSet;
import fr.voldir.armorsystem.item.ArmorSetItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ArmorSystem.MOD_ID);

    public static final ArmorItem.Type[] SLOTS = {
            ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS
    };

    public static final String BASE_PREFIX = "base_";

    private static final Map<String, DeferredHolder<Item, Item>> BY_ID = new LinkedHashMap<>();
    private static final List<DeferredHolder<Item, Item>> ARMOR_ITEMS = new ArrayList<>();
    private static final List<DeferredHolder<Item, Item>> BASE_ITEMS = new ArrayList<>();

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        for (ArmorSet set : ArmorSet.all()) {
            for (ArmorItem.Type type : SLOTS) {
                String id = set.id() + "_" + type.getName();
                DeferredHolder<Item, Item> armor = ITEMS.register(id,
                        () -> new ArmorSetItem(ModArmorMaterials.holder(set), type, set, new Item.Properties()));
                BY_ID.put(id, armor);
                ARMOR_ITEMS.add(armor);
                DeferredHolder<Item, Item> base = ITEMS.register(BASE_PREFIX + id,
                        () -> new Item(new Item.Properties().stacksTo(1)));
                BY_ID.put(BASE_PREFIX + id, base);
                BASE_ITEMS.add(base);
            }
        }
        ITEMS.register(modEventBus);
    }

    public static Item get(String id) {
        DeferredHolder<Item, Item> holder = BY_ID.get(id);
        return holder == null ? null : holder.get();
    }

    public static ArmorItem.Type typeForSlot(EquipmentSlot slot) {
        for (ArmorItem.Type type : SLOTS) {
            if (type.getSlot() == slot) return type;
        }
        return null;
    }

    public static List<DeferredHolder<Item, Item>> armorItems() {
        return ARMOR_ITEMS;
    }

    public static List<DeferredHolder<Item, Item>> baseItems() {
        return BASE_ITEMS;
    }

    public static List<ArmorSetItem> armorSetItems() {
        List<ArmorSetItem> items = new ArrayList<>();
        for (DeferredHolder<Item, Item> holder : ARMOR_ITEMS) {
            items.add((ArmorSetItem) holder.get());
        }
        return items;
    }
}
