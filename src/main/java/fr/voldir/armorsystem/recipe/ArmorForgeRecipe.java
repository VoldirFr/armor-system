package fr.voldir.armorsystem.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.voldir.armorsystem.config.ArmorSystemConfig;
import fr.voldir.armorsystem.item.ArmorSetItem;
import fr.voldir.armorsystem.registry.ModDataComponents;
import fr.voldir.armorsystem.registry.ModItems;
import fr.voldir.armorsystem.registry.ModRecipeSerializers;
import fr.voldir.armorsystem.registry.ModRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class ArmorForgeRecipe implements Recipe<CraftingInput> {

    private final Ingredient base;
    private final Ingredient ingot;

    public ArmorForgeRecipe(Ingredient base, Ingredient ingot) {
        this.base = base;
        this.ingot = ingot;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack baseStack = null;
        ItemStack ingotStack = null;
        int ingotCount = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (base.test(stack)) {
                if (baseStack != null) return false;
                baseStack = stack;
            } else if (ingot.test(stack)) {
                if (ingotStack != null && !ItemStack.isSameItem(ingotStack, stack)) return false;
                ingotStack = stack;
                ingotCount++;
            } else {
                return false;
            }
        }
        if (baseStack == null || ingotStack == null) return false;
        if (ingotCount != ArmorSystemConfig.ingotsPerArmor()) return false;
        return resolveResult(baseStack) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack baseStack = null;
        ItemStack ingotStack = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (base.test(stack)) baseStack = stack;
            else if (ingot.test(stack)) ingotStack = stack;
        }
        Item result = resolveResult(baseStack);
        if (result == null) return ItemStack.EMPTY;
        ItemStack out = new ItemStack(result);
        out.set(ModDataComponents.MATERIAL.get(), BuiltInRegistries.ITEM.getKey(ingotStack.getItem()));
        return out;
    }

    private static Item resolveResult(ItemStack baseStack) {
        if (baseStack == null) return null;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(baseStack.getItem());
        String path = key.getPath();
        if (!path.startsWith(ModItems.BASE_PREFIX)) return null;
        Item result = ModItems.get(path.substring(ModItems.BASE_PREFIX.length()));
        return result instanceof ArmorSetItem ? result : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        Item item = ModItems.get("medium_hybrid_chestplate");
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(base);
        list.add(ingot);
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.ARMOR_FORGE.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.ARMOR_FORGE.get();
    }

    public static class Serializer implements RecipeSerializer<ArmorForgeRecipe> {
        private static final MapCodec<ArmorForgeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("base").forGetter(recipe -> recipe.base),
                Ingredient.CODEC.fieldOf("ingot").forGetter(recipe -> recipe.ingot)
        ).apply(instance, ArmorForgeRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ArmorForgeRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.base,
                Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.ingot,
                ArmorForgeRecipe::new
        );

        @Override
        public MapCodec<ArmorForgeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ArmorForgeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
