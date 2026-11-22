package fr.voldir.armorsystem.registry;

import fr.voldir.armorsystem.ArmorSystem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArmorSystem.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("armorsystem", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.armorsystem"))
                    .icon(() -> new ItemStack(ModItems.get("ultra_heavy_physical_chestplate")))
                    .displayItems((parameters, output) -> {
                        ModItems.armorItems().forEach(holder -> output.accept(holder.get()));
                        ModItems.baseItems().forEach(holder -> output.accept(holder.get()));
                    })
                    .build());

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}
