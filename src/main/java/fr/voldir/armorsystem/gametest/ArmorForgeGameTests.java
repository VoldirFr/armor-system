package fr.voldir.armorsystem.gametest;

import fr.voldir.armorsystem.ArmorSystem;
import fr.voldir.armorsystem.config.ArmorSystemConfig;
import fr.voldir.armorsystem.core.Mineral;
import fr.voldir.armorsystem.registry.ModDataComponents;
import fr.voldir.armorsystem.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Craft verification (server side): 1 base armor + N configured ingots -> set piece with the
 * ingot stored as armorsystem:material.
 */
@GameTestHolder(ArmorSystem.MOD_ID)
public class ArmorForgeGameTests {

    /** Empty structure file: gameteststructures/empty3x3.snbt */
    @GameTest(template = "empty3x3")
    public static void basePlusDiamondCraftsUltraUltraLightPhysicalDiamond(GameTestHelper helper) {
        ItemStack base = new ItemStack(ModItems.get("base_ultra_light_physical_chestplate"));
        ItemStack result = craft(helper, base, new ItemStack(Items.DIAMOND));

        Item expected = ModItems.get("ultra_light_physical_chestplate");
        if (result.isEmpty()) {
            helper.fail("Base ultra_light physical chestplate + diamond did not craft anything");
            return;
        }
        if (result.getItem() != expected) {
            helper.fail("Expected " + BuiltInRegistries.ITEM.getKey(expected) + " but crafted "
                    + BuiltInRegistries.ITEM.getKey(result.getItem()));
            return;
        }
        ResourceLocation material = result.get(ModDataComponents.MATERIAL.get());
        if (material == null || !material.toString().equals("minecraft:diamond")) {
            helper.fail("Expected material minecraft:diamond but got " + material);
            return;
        }
        String name = result.getHoverName().getString();
        if (!name.toLowerCase().contains("diamond")) {
            helper.fail("Crafted piece name should mention the mineral but is: " + name);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3")
    public static void everyConfiguredMineralCraftsItsArmor(GameTestHelper helper) {
        for (Mineral mineral : Mineral.values()) {
            ItemStack ingot = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(ArmorSystemConfig.mineralIngot(mineral))));
            ItemStack result = craft(helper, new ItemStack(ModItems.get("base_medium_physical_chestplate")), ingot);
            String expected = "armorsystem:medium_physical_chestplate";
            if (!BuiltInRegistries.ITEM.getKey(result.getItem()).toString().equals(expected)) {
                helper.fail("Mineral " + mineral.id() + " -> expected " + expected + " but got "
                        + BuiltInRegistries.ITEM.getKey(result.getItem()));
                return;
            }
            ResourceLocation material = result.get(ModDataComponents.MATERIAL.get());
            if (material == null || !material.toString().equals(ArmorSystemConfig.mineralIngot(mineral))) {
                helper.fail("Mineral " + mineral.id() + " -> expected material " + ArmorSystemConfig.mineralIngot(mineral)
                        + " but got " + material);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3")
    public static void vanillaBasePlusMineralCraftsMappedSet(GameTestHelper helper) {
        ItemStack result = craft(helper, new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.DIAMOND));
        String expected = "armorsystem:medium_physical_chestplate";
        if (!BuiltInRegistries.ITEM.getKey(result.getItem()).toString().equals(expected)) {
            helper.fail("Iron chestplate + diamond -> expected " + expected + " but got "
                    + BuiltInRegistries.ITEM.getKey(result.getItem()));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3")
    public static void nonMineralIngotDoesNotCraft(GameTestHelper helper) {
        ItemStack result = craft(helper, new ItemStack(ModItems.get("base_medium_physical_chestplate")), new ItemStack(Items.STONE));
        if (!result.isEmpty()) {
            helper.fail("Base + stone should not craft anything but crafted "
                    + BuiltInRegistries.ITEM.getKey(result.getItem()));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3")
    public static void twoDifferentMineralsDoNotCraft(GameTestHelper helper) {
        List<ItemStack> stacks = new ArrayList<>(List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, new ItemStack(ModItems.get("base_medium_physical_chestplate")), new ItemStack(Items.DIAMOND),
                ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY));
        CraftingInput input = CraftingInput.of(3, 3, stacks);
        var recipe = lookup(helper, input);
        if (recipe.isPresent()) {
            helper.fail("Base + diamond + iron should not match");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3")
    public static void placedAnywhereInGridCrafts(GameTestHelper helper) {
        List<ItemStack> stacks = new ArrayList<>(List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                new ItemStack(Items.DIAMOND), new ItemStack(ModItems.get("base_medium_hybrid_helmet")), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        CraftingInput input = CraftingInput.of(3, 3, stacks);
        var recipe = lookup(helper, input);
        if (recipe.isEmpty()) {
            helper.fail("Base + diamond in any arrangement should match");
            return;
        }
        ItemStack result = recipe.get().value().assemble(input, helper.getLevel().registryAccess());
        if (!BuiltInRegistries.ITEM.getKey(result.getItem()).toString().equals("armorsystem:medium_hybrid_helmet")) {
            helper.fail("Expected armorsystem:medium_hybrid_helmet but got "
                    + BuiltInRegistries.ITEM.getKey(result.getItem()));
            return;
        }
        helper.succeed();
    }

    private static ItemStack craft(GameTestHelper helper, ItemStack base, ItemStack ingot) {
        List<ItemStack> stacks = new ArrayList<>(List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        stacks.set(1, base);
        stacks.set(4, ingot);
        CraftingInput input = CraftingInput.of(3, 3, stacks);
        var recipe = lookup(helper, input);
        if (recipe.isEmpty()) return ItemStack.EMPTY;
        return recipe.get().value().assemble(input, helper.getLevel().registryAccess());
    }

    private static java.util.Optional<RecipeHolder<CraftingRecipe>> lookup(GameTestHelper helper, CraftingInput input) {
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
    }
}
