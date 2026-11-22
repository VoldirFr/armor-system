package fr.voldir.armorsystem.registry;

import fr.voldir.armorsystem.ArmorSystem;
import fr.voldir.armorsystem.recipe.ArmorForgeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeTypes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ArmorSystem.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ArmorForgeRecipe>> ARMOR_FORGE =
            RECIPE_TYPES.register("armor_forge",
                    () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(ArmorSystem.MOD_ID, "armor_forge")));

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
    }
}
