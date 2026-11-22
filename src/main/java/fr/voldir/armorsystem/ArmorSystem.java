package fr.voldir.armorsystem;

import fr.voldir.armorsystem.config.ArmorSystemConfig;
import fr.voldir.armorsystem.registry.ModArmorMaterials;
import fr.voldir.armorsystem.registry.ModCreativeTabs;
import fr.voldir.armorsystem.registry.ModDataComponents;
import fr.voldir.armorsystem.registry.ModItems;
import fr.voldir.armorsystem.registry.ModRecipeSerializers;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(ArmorSystem.MOD_ID)
public class ArmorSystem {

    public static final String MOD_ID = "armorsystem";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArmorSystem(IEventBus modEventBus, ModContainer modContainer) {
        ModDataComponents.register(modEventBus);
        ModArmorMaterials.register(modEventBus);
        ModItems.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, ArmorSystemConfig.SPEC);
    }
}
