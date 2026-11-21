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
        ItemColor color = (stack, tintIndex) -> tintIndex == 0 ? 0xFF000000 | colorOf((ArmorSetItem) stack.getItem()) : 0xFFFFFFFF;
        for (ArmorSetItem item : ModItems.armorItems()) {
            event.register(color, item);
        }
    }

    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        for (ArmorSetItem item : ModItems.armorItems()) {
            event.registerItem(new IClientItemExtensions() {
                @Override
                public int getArmorLayerTintColor(ItemStack stack, LivingEntity entity, ArmorMaterial.Layer layer, int layerIndex, int fallbackColor) {
                    return layer.dyeable() ? 0xFF000000 | colorOf(item) : 0xFFFFFFFF;
                }
            }, item);
        }
    }

    private static int colorOf(ArmorSetItem item) {
        int configured = ArmorSystemConfig.parseHexColor(ArmorSystemConfig.mineralColorHex(item.mineral()));
        if (configured >= 0) return configured;
        return IngotColorResolver.resolve(item.mineral());
    }
}
