package fr.voldir.armorsystem.client;

import fr.voldir.armorsystem.ArmorSystem;
import fr.voldir.armorsystem.core.ArmorTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side generation of the hue-shifted "shift" texture for the custom armor models
 * (one per model class: light / medium / heavy, see {@link ArmorTier.ModelClass}).
 *
 * <p>Each base texture ({@code textures/models/armor/<class>_shift_layer_1.png}) is pure
 * grayscale: it holds the shading (luminance) of the garment. Instead of brute-force
 * multiplying a tint over it (which crushes the shading), every pixel is re-colored by
 * mapping its luminance onto the mineral color while keeping the grayscale luminance:
 * dark areas become deep and saturated, light areas become pale, mid tones carry the
 * mineral hue. One texture is generated (and cached) per mineral color.
 *
 * <p>NativeImage stores pixels packed as ABGR (red in the lowest byte); conversion is
 * done manually.
 */
@OnlyIn(Dist.CLIENT)
public final class CustomArmorTextures {

    private static final Map<String, ResourceLocation> CACHE = new ConcurrentHashMap<>();

    private CustomArmorTextures() {
    }

    /** Returns the hue-shifted texture for a model class + mineral key + color, generating it on first use. */
    public static ResourceLocation hueShifted(ArmorTier.ModelClass modelClass, String mineralKey, int colorArgb) {
        String key = modelClass.id() + "/" + safeKey(mineralKey);
        return CACHE.computeIfAbsent(key, k -> register(modelClass, safeKey(mineralKey), colorArgb));
    }

    private static String safeKey(String mineralKey) {
        StringBuilder sb = new StringBuilder();
        for (char c : mineralKey.toLowerCase().toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private static ResourceLocation register(ArmorTier.ModelClass modelClass, String mineralKey, int colorArgb) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                ArmorSystem.MOD_ID, "dynamic/" + modelClass.id() + "_shift/" + mineralKey);
        ResourceLocation baseTexture = ResourceLocation.fromNamespaceAndPath(
                ArmorSystem.MOD_ID, "textures/models/armor/" + modelClass.id() + "_shift_layer_1.png");
        try (InputStream in = Minecraft.getInstance().getResourceManager().getResource(baseTexture).get().open()) {
            com.mojang.blaze3d.platform.NativeImage image = com.mojang.blaze3d.platform.NativeImage.read(in);
            hueShift(image, colorArgb);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate hue-shifted " + modelClass.id()
                    + " armor texture for " + mineralKey, e);
        }
        return id;
    }

    /** Re-colors the grayscale image in place, preserving its luminance shading. */
    private static void hueShift(com.mojang.blaze3d.platform.NativeImage image, int colorArgb) {
        float[] hsv = rgbToHsv(
                FastColor.ARGB32.red(colorArgb) / 255f,
                FastColor.ARGB32.green(colorArgb) / 255f,
                FastColor.ARGB32.blue(colorArgb) / 255f);
        float hue = hsv[0];
        float sat = hsv[1];

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int packed = image.getPixelRGBA(x, y); // ABGR
                int a = (packed >>> 24) & 0xFF;
                if (a == 0) continue;
                int r = packed & 0xFF;
                int g = (packed >>> 8) & 0xFF;
                int b = (packed >>> 16) & 0xFF;
                float luminance = (r + g + b) / (3f * 255f);

                // Hue shift, not a flat tint: shadows get more saturation, highlights pale out,
                // with a subtle complementary hue rotation (shadows cooler, highlights warmer).
                float outHue = hue + (0.5f - luminance) * 0.05f;
                float outSat = sat * (1f - 0.85f * luminance * luminance);
                float outVal = (float) Math.pow(luminance, 0.85f);
                int rgb = Mth.hsvToRgb(outHue, outSat, outVal); // RRGGBB (no alpha)
                int outR = (rgb >> 16) & 0xFF;
                int outG = (rgb >> 8) & 0xFF;
                int outB = rgb & 0xFF;
                int outPacked = (a << 24) | (outB << 16) | (outG << 8) | outR;
                image.setPixelRGBA(x, y, outPacked);
            }
        }
    }

    private static float[] rgbToHsv(float r, float g, float b) {
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h;
        if (delta <= 1e-6f) {
            h = 0f;
        } else if (max == r) {
            h = ((g - b) / delta) % 6f;
        } else if (max == g) {
            h = (b - r) / delta + 2f;
        } else {
            h = (r - g) / delta + 4f;
        }
        h /= 6f;
        if (h < 0) h += 1f;
        float s = max <= 1e-6f ? 0f : delta / max;
        return new float[]{h, s, max};
    }
}
