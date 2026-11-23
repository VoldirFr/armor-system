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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
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
 * (Y down): {@code y_java = 24 - y_bedrock}; X and Z keep their sign. Rotations are
 * converted to the Java composition (Rz*Ry*Rx) by conjugating the Bedrock matrix
 * (Rx*Ry*Rz, right-handed) with the Y-flip flip: the resulting angles are
 * {@code (-x, +y, -z)}, and multi-axis rotations use the exact ZYX Euler extraction
 * (see {@link #toJavaAngles}).
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

    /** Per-cube UV fix metadata, gathered while parsing the geometry, applied after baking. */
    private record CubeUVFix(List<String> partPath, String childName,
                             float u, float v, float w, float h, float d, boolean mirror) {
    }

    /** Texture size from the geometry description, plus the fixes. */
    private static final Map<ArmorTier.ModelClass, float[]> TEXTURE_SIZES = new ConcurrentHashMap<>();
    private static final Map<ArmorTier.ModelClass, List<CubeUVFix>> CUBE_UV_FIXES = new ConcurrentHashMap<>();

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
        applyCubeUVFixes(root, modelClass, CUBE_UV_FIXES.get(modelClass));
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
        TEXTURE_SIZES.put(modelClass, new float[]{textureWidth, textureHeight});
        CUBE_UV_FIXES.put(modelClass, new ArrayList<>());

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
            attachCubes(entry.getKey(), entry.getValue(), bones, parts, modelClass);
        }

        return LayerDefinition.create(mesh, textureWidth, textureHeight);
    }

    private static float[] pivotOf(JsonObject bone) {
        return bone.has("pivot") ? floats(bone.getAsJsonArray("pivot")) : new float[]{0f, 24f, 0f};
    }

    /**
     * Converts a Bedrock rotation (degrees, composition Rx(rx)*Ry(ry)*Rz(rz), right-handed)
     * to vanilla {@link net.minecraft.client.model.geom.PartPose} angles (radians, composition
     * Rz(zRot)*Ry(yRot)*Rx(xRot)) under the Bedrock&larr;&rarr;Java mapping {@code (x, -y, z)}.
     *
     * <p>For single-axis rotations this reduces to {@code (-rx, +ry, -rz)} — note that Y keeps
     * its sign, contrary to X and Z. For multi-axis rotations the exact ZYX Euler angles of the
     * conjugated matrix are extracted so the composition order matches vanilla.
     */
    private static float[] toJavaAngles(float bx, float by, float bz) {
        if (bx == 0f && by == 0f && bz == 0f) {
            return new float[3];
        }
        // B = Rx(rx) * Ry(ry) * Rz(rz), right-handed, angles in radians
        double rx = Math.toRadians(bx), ry = Math.toRadians(by), rz = Math.toRadians(bz);
        double cx = Math.cos(rx), sx = Math.sin(rx);
        double cy = Math.cos(ry), sy = Math.sin(ry);
        double cz = Math.cos(rz), sz = Math.sin(rz);
        double[][] b = {
                {cy * cz, -cy * sz, sy},
                {cx * sz + sx * sy * cz, cx * cz - sx * sy * sz, -sx * cy},
                {sx * sz - cx * sy * cz, sx * cz + cx * sy * sz, cx * cy}
        };
        // Conjugate by the Y-flip (diag(1, -1, 1)) to land in Java model space.
        // Then extract ZYX Euler angles: A = Rz(z)*Ry(y)*Rx(x) with
        //   y = -asin(A[2][0]), x = atan2(A[2][1], A[2][2]), z = atan2(A[1][0], A[0][0]).
        // Conjugation flips the sign of every entry with exactly one index equal to 1 (the Y row/column).
        double[][] a = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                double s = (i == 1) == (j == 1) ? 1.0 : -1.0;
                a[i][j] = s * b[i][j];
            }
        }
        double yRot = -Math.asin(Mth.clamp(a[2][0], -1.0, 1.0));
        double xRot;
        double zRot;
        if (Math.abs(Math.abs(a[2][0]) - 1.0) > 1.0e-7) {
            xRot = Math.atan2(a[2][1], a[2][2]);
            zRot = Math.atan2(a[1][0], a[0][0]);
        } else {
            // Gimbal lock: pitch is +-90deg; fold the residual into the roll.
            xRot = Math.atan2(-a[1][2], a[1][1]);
            zRot = 0.0;
        }
        return new float[]{(float) xRot, (float) yRot, (float) zRot};
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
        float[] jr = toJavaAngles(rot[0], rot[1], rot[2]);
        PartPose pose = PartPose.offsetAndRotation(dx, dy, dz,
                Mth.DEG_TO_RAD * jr[0], Mth.DEG_TO_RAD * jr[1], Mth.DEG_TO_RAD * jr[2]);

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

    private static void attachCubes(String boneName, JsonObject bone, Map<String, JsonObject> bones,
                                    Map<String, PartDefinition> parts, ArmorTier.ModelClass modelClass) {
        if (!bone.has("cubes")) return;
        PartDefinition part = parts.get(boneName);
        float[] bonePivot = pivotOf(bone);
        List<String> partPath = partPathOf(bones, boneName);
        List<CubeUVFix> fixes = CUBE_UV_FIXES.get(modelClass);

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
                float[] jr = toJavaAngles(rot[0], rot[1], rot[2]);
                pose = PartPose.offsetAndRotation(
                        pivot[0] - bonePivot[0],
                        bonePivot[1] - pivot[1],
                        pivot[2] - bonePivot[2],
                        Mth.DEG_TO_RAD * jr[0],
                        Mth.DEG_TO_RAD * jr[1],
                        Mth.DEG_TO_RAD * jr[2]);
                rel = new float[]{origin[0] - pivot[0], pivot[1] - origin[1] - size[1], origin[2] - pivot[2]};
            } else {
                pose = PartPose.ZERO;
                rel = new float[]{origin[0] - bonePivot[0], bonePivot[1] - origin[1] - size[1], origin[2] - bonePivot[2]};
            }

            CubeListBuilder builder = CubeListBuilder.create().texOffs(uv[0], uv[1]);
            if (mirror) builder.mirror();
            builder.addBox(rel[0], rel[1], rel[2], size[0], size[1], size[2], new CubeDeformation(inflate));
            part.addOrReplaceChild(childName, builder, pose);

            // Bedrock/Blockbench uses a different per-face uv assignment than the
            // vanilla Cube constructor (see applyCubeUVFixes); the fix is applied
            // to the baked cube because CubeListBuilder has no per-face uv API.
            fixes.add(new CubeUVFix(partPath, childName, uv[0], uv[1], size[0], size[1], size[2], mirror));
        }
    }

    /** Part-name path from the root part down to the part of the given bone. */
    private static List<String> partPathOf(Map<String, JsonObject> bones, String boneName) {
        List<String> path = new ArrayList<>();
        String current = boneName;
        while (true) {
            JsonObject bone = bones.get(current);
            String parentName = bone.has("parent") ? bone.get("parent").getAsString() : null;
            if (parentName == null) {
                path.add(0, ROOT_BONES.containsKey(current) ? ROOT_BONES.get(current) : sanitizeName(current));
                return path;
            }
            path.add(0, sanitizeName(current));
            current = parentName;
        }
    }

    /**
     * Overwrites every cube's per-face uv with the Bedrock/Blockbench convention
     * (the one the {@code *_layer1_layer_*} textures were painted with).
     *
     * <p>Differences vs the vanilla box-uv layout computed by {@code ModelPart.Cube}:
     * <ul>
     *   <li>north/south: same uv rect, but the corner uv assignment is rotated 180&deg;.</li>
     *   <li>east/west: the side slabs of the box unwrap are swapped.</li>
     *   <li>up/down: the top slabs are swapped (and down's v-range runs inverted).</li>
     * </ul>
     */
    private static void applyCubeUVFixes(ModelPart root, ArmorTier.ModelClass modelClass, List<CubeUVFix> fixes) {
        float texW = TEXTURE_SIZES.get(modelClass)[0];
        float texH = TEXTURE_SIZES.get(modelClass)[1];
        int applied = 0;
        int missing = 0;
        for (CubeUVFix fix : fixes) {
            try {
                ModelPart parent = getChildOrNull(root, fix.partPath().get(0));
                for (int i = 1; i < fix.partPath().size(); i++) {
                    parent = getChildOrNull(parent, fix.partPath().get(i));
                }
                ModelPart cubePart = getChildOrNull(parent, fix.childName());
                if (cubePart == null) {
                    missing++;
                    continue;
                }
                for (ModelPart.Cube cube : cubePart.cubes) {
                    setGeckoUVs(cube, fix.u(), fix.v(), fix.w(), fix.h(), fix.d(), fix.mirror(), texW, texH);
                    applied++;
                }
            } catch (NoSuchElementException | IndexOutOfBoundsException ignored) {
                // part missing (e.g. hidden stub) — leave vanilla uvs untouched
            }
        }
        ArmorSystem.LOGGER.debug("UVFIX {}: {} cubes found, {} fixed, {} missing, tex {}x{}",
                modelClass, fixes.size(), applied, missing, (int) texW, (int) texH);
    }

    private static void setGeckoUVs(ModelPart.Cube cube, float u, float v, float w, float h, float d,
                                    boolean mirror, float texW, float texH) {
        float cx = (cube.minX + cube.maxX) * 0.5f;
        float cy = (cube.minY + cube.maxY) * 0.5f;
        float cz = (cube.minZ + cube.maxZ) * 0.5f;
        for (ModelPart.Polygon poly : cube.polygons) {
            Direction face = faceOf(poly, cx, cy, cz);
            if (face == null) continue;
            // The cube is unwrapped as one Bedrock "box": a u-row of
            // [E | N | W | S] followed by an u-row of [U | D], with e1..s2 the
            // four corner u's of the horizontal strip (see break-down below).
            // Blockbench's Box-UV unwrap TRUNCATES the cube dimensions to whole
            // texels (a 10.9-deep cube unwraps as if 10 deep), so the texture
            // painted in Blockbench only aligns when we floor() the dimensions
            // for the uv computation. The geometry itself keeps its exact size.
            float fw = (float) Math.floor(w + 1.0e-4f);
            float fh = (float) Math.floor(h + 1.0e-4f);
            float fd = (float) Math.floor(d + 1.0e-4f);
            float e1 = u;                 // east quad, first column
            float e2 = u + fd;            // east quad, second column
            float w1 = u + fd + fw;       // west quad, first column
            float w2 = u + fd + fw + fd;  // west quad, second column
            float n1 = u + fd;            // north quad, first column
            float n2 = u + fd + fw;       // north quad, second column
            float s1 = u + fd + fw + fd;  // south quad, first column
            float s2 = u + fd + fw + fd + fw; // south quad, second column
            float p1 = u + fd;            // up quad, first column
            float p2 = u + fd + fw;       // up quad, second column
            float q1 = u + fd + fw;       // down quad, first column
            float q2 = u + fd + 2 * fw;   // down quad, second column
            for (ModelPart.Vertex vertex : poly.vertices) {
                // flags of the cube corner this vertex sits on: onXplus = the
                // +x side (bedrock x=0 corners), top = the bedrock top row
                // (y=h), right = the +z side (bedrock z=d). Under mirror the
                // east/west quads swap physical sides, so their slabs swap too;
                // the u order just inverts on north/south/up/down.
                boolean onXplus = vertex.pos.x() > cx;
                boolean top = vertex.pos.y() < cy;
                boolean right = vertex.pos.z() > cz;
                float uu = switch (face) {
                    case EAST -> mirror ? (right ? e1 : e2) : (right ? w2 : w1);
                    case WEST -> mirror ? (right ? w2 : w1) : (right ? e1 : e2);
                    case NORTH -> mirror ? (onXplus ? n1 : n2) : (onXplus ? n2 : n1);
                    case SOUTH -> mirror ? (onXplus ? s2 : s1) : (onXplus ? s1 : s2);
                    case UP -> mirror ? (onXplus ? p1 : p2) : (onXplus ? p2 : p1);
                    case DOWN -> mirror ? (onXplus ? q1 : q2) : (onXplus ? q2 : q1);
                    default -> Float.NaN;
                };
                if (Float.isNaN(uu)) continue;
                // v: side faces run top->bottom over the v+fd..v+fd+fh strip;
                // up/down slabs sit in the header strip and run along z:
                // right(+z)->v, back(-z)->v+fd
                float vv = face == Direction.UP || face == Direction.DOWN
                        ? (right ? v : v + fd)
                        : (top ? v + fd : v + fd + fh);
                vertex.u = uu / texW;
                vertex.v = vv / texH;
            }
        }
    }

    private static Direction faceOf(ModelPart.Polygon poly, float cx, float cy, float cz) {
        // face = the axis whose coordinate is constant across the 4 vertices
        float x0 = poly.vertices[0].pos.x();
        float y0 = poly.vertices[0].pos.y();
        float z0 = poly.vertices[0].pos.z();
        boolean xConst = true;
        boolean yConst = true;
        boolean zConst = true;
        for (ModelPart.Vertex vertex : poly.vertices) {
            if (Math.abs(vertex.pos.x() - x0) > 1.0e-5f) xConst = false;
            if (Math.abs(vertex.pos.y() - y0) > 1.0e-5f) yConst = false;
            if (Math.abs(vertex.pos.z() - z0) > 1.0e-5f) zConst = false;
        }
        if (xConst) return x0 < cx ? Direction.WEST : Direction.EAST;
        if (yConst) return y0 < cy ? Direction.UP : Direction.DOWN;
        if (zConst) return z0 < cz ? Direction.NORTH : Direction.SOUTH;
        return null;
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
