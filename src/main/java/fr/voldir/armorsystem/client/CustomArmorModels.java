package fr.voldir.armorsystem.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.voldir.armorsystem.ArmorSystem;
import fr.voldir.armorsystem.core.ArmorTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom 3D armor models, loaded from Blockbench Bedrock geometry files (128x128 UV),
 * one per model class (see {@link ArmorTier.ModelClass}):
 * <ul>
 *   <li>light  -> {@code models/armor/light/tempest_robe.geo.json}</li>
 *   <li>medium -> {@code models/armor/medium/archer_armor.geo.json}</li>
 *   <li>heavy  -> {@code models/armor/heavy/paladin_armor.geo.json}</li>
 * </ul>
 *
 * <p>Bedrock bones are converted to vanilla {@link HumanoidModel} parts:
 * {@code bipedHead -> "head"}, {@code bipedBody -> "body"},
 * {@code bipedLeftArm/RightArm -> "left_arm"/"right_arm"},
 * {@code bipedLeftLeg/RightLeg -> "left_leg"/"right_leg"}.
 * Vanilla requires the "hat" part to be a direct child of the root, so an empty stub
 * named "hat" is created there; the real hat cubes stay nested under the head part so
 * they follow the head animation.
 *
 * <p>Conversion from Bedrock space (Y up, feet at 0, 32 px figure) to Java model space
 * (Y down): {@code y_java = 24 - y_bedrock}; X and Z keep their sign; rotations are
 * negated on all three axes.
 *
 * <p>Vanilla {@code CubeListBuilder} does not support per-cube rotation, so each cube
 * becomes a child part (non-rotated cubes use {@link PartPose#ZERO}).
 */
@OnlyIn(Dist.CLIENT)
public final class CustomArmorModels {

    private static final Map<String, String> ROOT_BONES = Map.of(
            "bipedHead", "head",
            "bipedBody", "body",
            "bipedLeftArm", "left_arm",
            "bipedRightArm", "right_arm",
            "bipedLeftLeg", "left_leg",
            "bipedRightLeg", "right_leg");

    private static final Map<ArmorTier.ModelClass, Map<EquipmentSlot, HumanoidModel<LivingEntity>>> MODELS =
            new EnumMap<>(ArmorTier.ModelClass.class);
    private static final Map<ArmorTier.ModelClass, LayerDefinition> LAYER_DEFINITIONS = new ConcurrentHashMap<>();

    private CustomArmorModels() {
    }

    /** Returns the (cached) humanoid model for the given model class + slot, with the right sub-parts enabled. */
    public static HumanoidModel<LivingEntity> modelFor(ArmorTier.ModelClass modelClass, EquipmentSlot slot) {
        Map<EquipmentSlot, HumanoidModel<LivingEntity>> bySlot = MODELS.computeIfAbsent(modelClass, c -> new EnumMap<>(EquipmentSlot.class));
        return bySlot.computeIfAbsent(slot, s -> new HumanoidModel<>(bake(modelClass, s)));
    }

    /**
     * Hides garment parts that must not appear twice for a given equipment slot.
     * All 3 geometries share the same hierarchy: the leg garment ({@code armor*leg})
     * and the boots ({@code armor*boot}) are both children of {@code left_leg}/{@code right_leg}.
     */
    private static ModelPart bake(ArmorTier.ModelClass modelClass, EquipmentSlot slot) {
        ModelPart root = layerDefinition(modelClass).bakeRoot();
        switch (slot) {
            case LEGS -> {
                setVisible(root, "left_leg", "armorleftboot");
                setVisible(root, "right_leg", "armorrightboot");
            }
            case FEET -> {
                setVisible(root, "left_leg", "armorleftleg");
                setVisible(root, "right_leg", "armorrightleg");
            }
            default -> {
            }
        }
        return root;
    }

    private static void setVisible(ModelPart root, String parentName, String childName) {
        ModelPart parent = getChildOrNull(root, parentName);
        if (parent == null) return;
        ModelPart child = getChildOrNull(parent, childName);
        if (child != null) child.visible = false;
    }

    private static ModelPart getChildOrNull(ModelPart parent, String name) {
        try {
            return parent.getChild(name);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public static LayerDefinition layerDefinition(ArmorTier.ModelClass modelClass) {
        return LAYER_DEFINITIONS.computeIfAbsent(modelClass, CustomArmorModels::parseLayerDefinition);
    }

    private static LayerDefinition parseLayerDefinition(ArmorTier.ModelClass modelClass) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        ResourceLocation path = ResourceLocation.fromNamespaceAndPath(
                ArmorSystem.MOD_ID, modelClass.geometryPath());
        JsonObject geometry = readGeometry(path);
        JsonObject description = geometry.getAsJsonObject("description");
        int textureWidth = description.get("texture_width").getAsInt();
        int textureHeight = description.get("texture_height").getAsInt();

        Map<String, JsonObject> bones = new HashMap<>();
        for (JsonElement element : geometry.getAsJsonArray("bones")) {
            JsonObject bone = element.getAsJsonObject();
            bones.put(bone.get("name").getAsString(), bone);
        }

        Map<String, PartDefinition> parts = new HashMap<>();
        for (String name : bones.keySet()) {
            createPart(bones, parts, root, name);
        }
        for (Map.Entry<String, JsonObject> entry : bones.entrySet()) {
            attachCubes(entry.getKey(), entry.getValue(), parts);
        }

        return LayerDefinition.create(mesh, textureWidth, textureHeight);
    }

    private static float[] pivotOf(JsonObject bone) {
        return bone.has("pivot") ? floats(bone.getAsJsonArray("pivot")) : new float[]{0f, 24f, 0f};
    }

    /** Creates the part of a bone (and its parent chain). */
    private static void createPart(Map<String, JsonObject> bones, Map<String, PartDefinition> parts,
                                   PartDefinition root, String name) {
        if (parts.containsKey(name)) return;
        JsonObject bone = bones.get(name);
        String parentName = bone.has("parent") ? bone.get("parent").getAsString() : null;

        PartDefinition parent;
        if (parentName == null) {
            parent = root;
        } else {
            createPart(bones, parts, root, parentName);
            parent = parts.get(parentName);
        }

        float[] pivot = pivotOf(bone);
        float dx, dy, dz;
        if (parent == root) {
            dx = pivot[0];
            dy = 24f - pivot[1];
            dz = pivot[2];
        } else {
            float[] parentPivot = pivotOf(bones.get(parentName));
            dx = pivot[0] - parentPivot[0];
            dy = parentPivot[1] - pivot[1];
            dz = pivot[2] - parentPivot[2];
        }

        float[] rot = bone.has("rotation") ? floats(bone.getAsJsonArray("rotation")) : new float[3];
        PartPose pose = PartPose.offsetAndRotation(dx, dy, dz,
                Mth.DEG_TO_RAD * -rot[0], Mth.DEG_TO_RAD * -rot[1], Mth.DEG_TO_RAD * -rot[2]);

        // A root-level biped* bone IS the vanilla humanoid part; other bones nest under it.
        boolean isVanillaPart = parent == root && ROOT_BONES.containsKey(name);
        String partName = isVanillaPart ? ROOT_BONES.get(name) : sanitizeName(name);
        PartDefinition def = parent.addOrReplaceChild(partName, CubeListBuilder.create(), pose);
        parts.put(name, def);
        if (isVanillaPart) parts.put(ROOT_BONES.get(name), def);

        // Vanilla HumanoidModel requires root.getChild("hat").
        if (name.equals("hat") && parent != root) {
            root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        }
    }

    private static void attachCubes(String boneName, JsonObject bone, Map<String, PartDefinition> parts) {
        if (!bone.has("cubes")) return;
        PartDefinition part = parts.get(boneName);
        float[] bonePivot = pivotOf(bone);

        JsonArray cubes = bone.getAsJsonArray("cubes");
        for (int i = 0; i < cubes.size(); i++) {
            JsonObject cube = cubes.get(i).getAsJsonObject();
            float[] origin = floats(cube.getAsJsonArray("origin"));
            float[] size = floats(cube.getAsJsonArray("size"));
            float inflate = cube.has("inflate") ? cube.get("inflate").getAsFloat() : 0f;
            boolean mirror = cube.has("mirror") && cube.get("mirror").getAsBoolean();
            int[] uv = ints(cube.getAsJsonArray("uv"));
            String childName = sanitizeName(boneName) + "_" + i;

            PartPose pose;
            float[] rel;
            if (cube.has("rotation")) {
                float[] pivot = floats(cube.getAsJsonArray("pivot"));
                float[] rot = floats(cube.getAsJsonArray("rotation"));
                pose = PartPose.offsetAndRotation(
                        pivot[0] - bonePivot[0],
                        bonePivot[1] - pivot[1],
                        pivot[2] - bonePivot[2],
                        Mth.DEG_TO_RAD * -rot[0],
                        Mth.DEG_TO_RAD * -rot[1],
                        Mth.DEG_TO_RAD * -rot[2]);
                rel = new float[]{origin[0] - pivot[0], pivot[1] - origin[1] - size[1], origin[2] - pivot[2]};
            } else {
                pose = PartPose.ZERO;
                rel = new float[]{origin[0] - bonePivot[0], bonePivot[1] - origin[1] - size[1], origin[2] - bonePivot[2]};
            }

            CubeListBuilder builder = CubeListBuilder.create().texOffs(uv[0], uv[1]);
            if (mirror) builder.mirror();
            builder.addBox(rel[0], rel[1], rel[2], size[0], size[1], size[2], new CubeDeformation(inflate));
            part.addOrReplaceChild(childName, builder, pose);
        }
    }

    private static String sanitizeName(String name) {
        return name.toLowerCase().replace(':', '_');
    }

    private static float[] floats(JsonArray array) {
        float[] result = new float[array.size()];
        for (int i = 0; i < result.length; i++) result[i] = array.get(i).getAsFloat();
        return result;
    }

    private static int[] ints(JsonArray array) {
        int[] result = new int[array.size()];
        for (int i = 0; i < result.length; i++) result[i] = array.get(i).getAsInt();
        return result;
    }

    private static JsonObject readGeometry(ResourceLocation path) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().getResource(path).get().open()) {
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return JsonParser.parseString(json).getAsJsonObject()
                    .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load custom armor geometry " + path, e);
        }
    }
}
