package fr.voldir.armorsystem.registry;

import fr.voldir.armorsystem.ArmorSystem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {

    public static final TagKey<Item> ARMOR_INGOTS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ArmorSystem.MOD_ID, "armor_ingots"));

    public static final TagKey<Item> BASE_ARMORS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ArmorSystem.MOD_ID, "base_armors"));
}
