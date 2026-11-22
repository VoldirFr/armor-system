package fr.voldir.armorsystem;

import fr.voldir.armorsystem.client.IngotColorResolver;
import fr.voldir.armorsystem.config.ArmorSystemConfig;
import fr.voldir.armorsystem.item.ArmorSetItem;
import fr.voldir.armorsystem.registry.ModItems;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@Mod(value = ArmorSystem.MOD_ID, dist = Dist.CLIENT)
public class ArmorSystemClient {

    public ArmorSystemClient(IEventBus modEventBus) {
        modEventBus.addListener(ArmorSystemClient::onRegisterItemColors);
        modEventBus.addListener(ArmorSystemClient::onRegisterClientExtensions);
    }

    private static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        ItemColor color = (stack, tintIndex) -> tintIndex == 1 && stack.getItem() instanceof ArmorSetItem armor
                ? 0xFF000000 | colorOf(armor, stack)
                : 0xFFFFFFFF;
        for (ArmorSetItem item : ModItems.armorSetItems()) {
            event.register(color, item);
        }
    }

    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        for (ArmorSetItem item : ModItems.armorSetItems()) {
            event.registerItem(new IClientItemExtensions() {
                @Override
                public int getArmorLayerTintColor(ItemStack stack, LivingEntity entity, ArmorMaterial.Layer layer, int layerIndex, int fallbackColor) {
                    if (layer.texture(false).getPath().contains(ArmorSetItem.COLOR_LAYER_NAME)) {
                        return 0xFF000000 | colorOf(item, stack);
                    }
                    return 0xFFFFFFFF;
                }
            }, item);
        }
    }

    private static int colorOf(ArmorSetItem item, ItemStack stack) {
        var mineral = item.getMineral(stack);
        int configured = ArmorSystemConfig.parseHexColor(ArmorSystemConfig.mineralColorHex(mineral));
        if (configured >= 0) return configured;
        return IngotColorResolver.resolve(mineral);
    }
}
