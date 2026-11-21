package fr.voldir.armorsystem.registry;

import fr.voldir.armorsystem.ArmorSystem;
import fr.voldir.armorsystem.core.ArmorSet;
import fr.voldir.armorsystem.item.ArmorSetItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ArmorSystem.MOD_ID);

    private static final ArmorItem.Type[] SLOTS = {
            ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS
    };

    private static final Map<String, DeferredHolder<Item, Item>> BY_ID = new LinkedHashMap<>();
    private static final List<DeferredHolder<Item, Item>> ALL = new ArrayList<>();

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        for (ArmorSet set : ArmorSet.all()) {
            for (ArmorItem.Type type : SLOTS) {
                String id = set.id() + "_" + type.getName();
                DeferredHolder<Item, Item> holder = ITEMS.register(id,
                        () -> new ArmorSetItem(ModArmorMaterials.holder(set), type, set, new Item.Properties()));
                BY_ID.put(id, holder);
                ALL.add(holder);
            }
        }
        ITEMS.register(modEventBus);
    }

    public static Item get(ArmorSet set, ArmorItem.Type type) {
        return BY_ID.get(set.id() + "_" + type.getName()).get();
    }

    public static ArmorItem.Type typeForSlot(net.minecraft.world.entity.EquipmentSlot slot) {
        for (ArmorItem.Type type : SLOTS) {
            if (type.getSlot() == slot) return type;
        }
        return null;
    }

    public static List<DeferredHolder<Item, Item>> all() {
        return ALL;
    }

    public static List<ArmorSetItem> armorItems() {
        List<ArmorSetItem> items = new ArrayList<>();
        for (DeferredHolder<Item, Item> holder : ALL) {
            items.add((ArmorSetItem) holder.get());
        }
        return items;
    }
}
