package fr.voldir.armorsystem.client;

import com.mojang.blaze3d.platform.NativeImage;
import fr.voldir.armorsystem.config.ArmorSystemConfig;
import fr.voldir.armorsystem.core.Mineral;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class IngotColorResolver {

    private static final Map<String, Integer> CACHE = new ConcurrentHashMap<>();

    private IngotColorResolver() {
    }

    public static int resolve(Mineral mineral) {
        return CACHE.computeIfAbsent(ArmorSystemConfig.mineralIngot(mineral), IngotColorResolver::compute);
    }

    private static int compute(String ingotId) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            Item ingot = BuiltInRegistries.ITEM.get(ResourceLocation.parse(ingotId));
            BakedModel model = minecraft.getItemRenderer().getModel(new ItemStack(ingot), minecraft.level, null, 0);
            TextureAtlasSprite sprite = model.getParticleIcon();
            NativeImage image = sprite.contents().getOriginalImage();
            long red = 0;
            long green = 0;
            long blue = 0;
            long count = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getPixelRGBA(x, y);
                    if (FastColor.ARGB32.alpha(pixel) < 16) continue;
                    red += FastColor.ARGB32.red(pixel);
                    green += FastColor.ARGB32.green(pixel);
                    blue += FastColor.ARGB32.blue(pixel);
                    count++;
                }
            }
            if (count == 0) return 0xD8D8D8;
            return FastColor.ARGB32.color(255, (int) (red / count), (int) (green / count), (int) (blue / count));
        } catch (Throwable throwable) {
            return 0xD8D8D8;
        }
    }
}
