package fr.voldir.armorsystem.registry;

import fr.voldir.armorsystem.ArmorSystem;
import fr.voldir.armorsystem.recipe.ArmorForgeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ArmorSystem.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ArmorForgeRecipe>> ARMOR_FORGE =
            RECIPE_SERIALIZERS.register("armor_forge", ArmorForgeRecipe.Serializer::new);

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
