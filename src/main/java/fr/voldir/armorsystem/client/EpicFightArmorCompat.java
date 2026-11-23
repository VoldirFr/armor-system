package fr.voldir.armorsystem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import yesman.epicfight.api.client.model.MeshPartDefinition;
import yesman.epicfight.api.client.model.SingleGroupVertexBuilder;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.transformer.HumanoidModelBaker;
import yesman.epicfight.api.client.model.transformer.HumanoidModelTransformer;
import yesman.epicfight.api.client.model.transformer.VanillaModelTransformer;
import yesman.epicfight.api.utils.math.Vec2f;
import yesman.epicfight.api.utils.math.Vec3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Epic Fight compatibility (registered only when the epicfight mod is present).
 *
 * <p>Epic Fight replaces the humanoid renderers: it converts every armor's
 * {@link HumanoidModel} (through {@code IClientItemExtensions#getHumanoidArmorModel})
 * into a {@link SkinnedMesh} bound to its biped armature, via
 * {@code HumanoidModelBaker.bakeArmor}. Its built-in {@code VanillaModelTransformer}
 * splits every cube across joints ("vertex weights" between chest/torso, upper/lower
 * limbs...) which tears apart the large cubes of our heavy armor (huge flat "wings").
 *
 * <p>This transformer registers itself with the public API
 * ({@link HumanoidModelBaker#registerNewTransformer}); it produces a rigid skinning:
 * every cube's vertices get a single joint, no clipping, so the geometry can never
 * tear. Bend points (elbow / knee) are still cut cleanly at the same y-coordinates
 * Epic Fight uses, so sleeves and pants follow the limbs.
 *
 * <p>Armor models not baked by this mod are not handled here (returns null), so they
 * fall through to Epic Fight's own transformers unchanged.
 */
@OnlyIn(Dist.CLIENT)
public final class EpicFightArmorCompat {

    /** Joint ids of Epic Fight's biped armature — same values as its VanillaModelTransformer. */
    private static final int JOINT_HEAD = 9;
    private static final int JOINT_CHEST = 8;
    private static final int JOINT_RIGHT_UPPER_ARM = 11;
    private static final int JOINT_RIGHT_LOWER_ARM = 12;
    private static final int JOINT_LEFT_UPPER_ARM = 16;
    private static final int JOINT_LEFT_LOWER_ARM = 17;
    private static final int JOINT_RIGHT_UPPER_LEG = 1;
    private static final int JOINT_RIGHT_LOWER_LEG = 2;
    private static final int JOINT_LEFT_UPPER_LEG = 4;
    private static final int JOINT_LEFT_LOWER_LEG = 5;

    /** Bend cut heights (Epic Fight space: y up, ground at 0, units of 1/16 block). */
    private static final float ARM_CLIP_Y = 19.0F;
    private static final float LEG_CLIP_Y = 6.0F;

    private EpicFightArmorCompat() {
    }

    /** Call once from the client mod constructor, only if {@code ModList.isLoaded("epicfight")}. */
    public static void register() {
        HumanoidModelBaker.registerNewTransformer(new Transformer());
    }

    private static final class Transformer extends HumanoidModelTransformer {
        @Override
        public SkinnedMesh transformArmorModel(HumanoidModel<?> humanoidModel) {
            if (!(humanoidModel instanceof CustomArmorModels.CustomArmor armor)) {
                return null; // not our model: defer to Epic Fight's own transformers
            }
            List<SingleGroupVertexBuilder> vertices = new ArrayList<>();
            Map<MeshPartDefinition, IntList> indices = new HashMap<>();
            int[] vertexCount = {0};

            switch (armor.slot) {
                case HEAD -> bakeSubtree(newPoseStack(), "head", humanoidModel.head,
                        new Rigid(JOINT_HEAD), vertices, indices, vertexCount);
                case CHEST -> {
                    bakeSubtree(newPoseStack(), "body", humanoidModel.body,
                            new Rigid(JOINT_CHEST), vertices, indices, vertexCount);
                    bakeSubtree(newPoseStack(), "rightArm", humanoidModel.rightArm,
                            new Bend(JOINT_RIGHT_UPPER_ARM, JOINT_RIGHT_LOWER_ARM, ARM_CLIP_Y), vertices, indices, vertexCount);
                    bakeSubtree(newPoseStack(), "leftArm", humanoidModel.leftArm,
                            new Bend(JOINT_LEFT_UPPER_ARM, JOINT_LEFT_LOWER_ARM, ARM_CLIP_Y), vertices, indices, vertexCount);
                }
                case LEGS -> {
                    bakeSubtree(newPoseStack(), "rightLeg", humanoidModel.rightLeg,
                            new Bend(JOINT_RIGHT_UPPER_LEG, JOINT_RIGHT_LOWER_LEG, LEG_CLIP_Y), vertices, indices, vertexCount);
                    bakeSubtree(newPoseStack(), "leftLeg", humanoidModel.leftLeg,
                            new Bend(JOINT_LEFT_UPPER_LEG, JOINT_LEFT_LOWER_LEG, LEG_CLIP_Y), vertices, indices, vertexCount);
                }
                default -> {
                    // boots: rigid on the calves
                    bakeSubtree(newPoseStack(), "rightLeg", humanoidModel.rightLeg,
                            new Rigid(JOINT_RIGHT_LOWER_LEG), vertices, indices, vertexCount);
                    bakeSubtree(newPoseStack(), "leftLeg", humanoidModel.leftLeg,
                            new Rigid(JOINT_LEFT_LOWER_LEG), vertices, indices, vertexCount);
                }
            }
            return SingleGroupVertexBuilder.loadVertexInformation(vertices, indices);
        }
    }

    private static PoseStack newPoseStack() {
        // vanilla humanoid model space -> Epic Fight space (y up, ground at 0, x mirrored)
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -24.0F, 0.0F);
        return poseStack;
    }

    private static void bakeSubtree(PoseStack poseStack, String name, ModelPart part, Rig rig,
                                    List<SingleGroupVertexBuilder> vertices,
                                    Map<MeshPartDefinition, IntList> indices, int[] vertexCount) {
        PartPose pose = part.getInitialPose();
        poseStack.pushPose();
        poseStack.translate(pose.x, pose.y, pose.z);
        poseStack.mulPose(new Quaternionf().rotationZYX(pose.zRot, pose.yRot, pose.xRot));
        bakePart(poseStack, name, rig, part, vertices, indices, vertexCount);
        poseStack.popPose();
    }

    private static void bakePart(PoseStack poseStack, String name, Rig rig, ModelPart part,
                                 List<SingleGroupVertexBuilder> vertices,
                                 Map<MeshPartDefinition, IntList> indices, int[] vertexCount) {
        if (part.visible && !part.skipDraw) {
            MeshPartDefinition definition = VanillaModelTransformer.VanillaMeshPartDefinition.of(name);
            for (ModelPart.Cube cube : part.cubes) {
                rig.bakeCube(poseStack, definition, cube, vertices, indices, vertexCount);
            }
        }
        for (Map.Entry<String, ModelPart> child : part.children.entrySet()) {
            PartPose pose = child.getValue().getInitialPose();
            poseStack.pushPose();
            poseStack.translate(pose.x, pose.y, pose.z);
            poseStack.mulPose(new Quaternionf().rotationZYX(pose.zRot, pose.yRot, pose.xRot));
            bakePart(poseStack, name + "." + child.getKey(), rig, child.getValue(), vertices, indices, vertexCount);
            poseStack.popPose();
        }
    }

    /** Joint assignment strategy for a subtree of cubes. */
    private interface Rig {
        void bakeCube(PoseStack poseStack, MeshPartDefinition definition, ModelPart.Cube cube,
                      List<SingleGroupVertexBuilder> vertices,
                      Map<MeshPartDefinition, IntList> indices, int[] vertexCount);
    }

    /** Every vertex is skinned to a single joint (no clipping, cannot tear). */
    private record Rigid(int joint) implements Rig {
        @Override
        public void bakeCube(PoseStack poseStack, MeshPartDefinition definition, ModelPart.Cube cube,
                             List<SingleGroupVertexBuilder> vertices,
                             Map<MeshPartDefinition, IntList> indices, int[] vertexCount) {
            Matrix3f normalMatrix = poseStack.last().normal();
            for (ModelPart.Polygon quad : cube.polygons) {
                Vector3f normal = new Vector3f(quad.normal).mul(normalMatrix);
                List<Vert> poly = new ArrayList<>(4);
                for (ModelPart.Vertex vertex : quad.vertices) {
                    Vector4f pos = new Vector4f(vertex.pos, 1.0F).mul(poseStack.last().pose());
                    poly.add(new Vert(pos.x(), pos.y(), pos.z(), vertex.u, vertex.v));
                }
                emit(poly, this.joint, normal, definition, vertices, indices, vertexCount);
            }
        }
    }

    /** Cubes crossing the cut height are split into an upper piece and a lower piece. */
    private record Bend(int upperJoint, int lowerJoint, float clipY) implements Rig {
        @Override
        public void bakeCube(PoseStack poseStack, MeshPartDefinition definition, ModelPart.Cube cube,
                             List<SingleGroupVertexBuilder> vertices,
                             Map<MeshPartDefinition, IntList> indices, int[] vertexCount) {
            Matrix3f normalMatrix = poseStack.last().normal();
            for (ModelPart.Polygon quad : cube.polygons) {
                Vector3f normal = new Vector3f(quad.normal).mul(normalMatrix);
                List<Vert> poly = new ArrayList<>(4);
                for (ModelPart.Vertex vertex : quad.vertices) {
                    Vector4f pos = new Vector4f(vertex.pos, 1.0F).mul(poseStack.last().pose());
                    poly.add(new Vert(pos.x(), pos.y(), pos.z(), vertex.u, vertex.v));
                }
                List<Vert> above = clip(poly, this.clipY, true);
                List<Vert> below = clip(poly, this.clipY, false);
                if (!above.isEmpty()) emit(above, this.upperJoint, normal, definition, vertices, indices, vertexCount);
                if (!below.isEmpty()) emit(below, this.lowerJoint, normal, definition, vertices, indices, vertexCount);
            }
        }
    }

    /** A transformed vertex (Epic Fight space, y up, ground at 0). */
    private record Vert(float x, float y, float z, float u, float v) {
        Vert lerp(Vert other, float t) {
            return new Vert(
                    this.x + (other.x - this.x) * t,
                    this.y + (other.y - this.y) * t,
                    this.z + (other.z - this.z) * t,
                    this.u + (other.u - this.u) * t,
                    this.v + (other.v - this.v) * t);
        }
    }

    /** Sutherland-Hodgman clip of a polygon against the plane y &gt;/&lt;= clipY, keeping uv interpolation. */
    private static List<Vert> clip(List<Vert> poly, float clipY, boolean keepAbove) {
        List<Vert> result = new ArrayList<>();
        int size = poly.size();
        for (int i = 0; i < size; i++) {
            Vert current = poly.get(i);
            Vert next = poly.get((i + 1) % size);
            boolean currentIn = keepAbove ? current.y() > clipY : current.y() < clipY;
            boolean nextIn = keepAbove ? next.y() > clipY : next.y() < clipY;
            if (currentIn) result.add(current);
            if (currentIn != nextIn) {
                float t = (clipY - current.y()) / (next.y() - current.y());
                result.add(current.lerp(next, t));
            }
        }
        return result;
    }

    private static void emit(List<Vert> poly, int joint, Vector3f normal, MeshPartDefinition definition,
                             List<SingleGroupVertexBuilder> vertices,
                             Map<MeshPartDefinition, IntList> indices, int[] vertexCount) {
        int count = poly.size();
        if (count < 3) return;
        int base = vertexCount[0];
        IntList list = indices.computeIfAbsent(definition, key -> new IntArrayList());
        for (int i = 0; i < count; i++) {
            Vert vert = poly.get(i);
            vertices.add(new SingleGroupVertexBuilder()
                    .setPosition(new Vec3f(vert.x(), vert.y(), vert.z()).scale(0.0625F))
                    .setNormal(new Vec3f(normal.x(), normal.y(), normal.z()))
                    .setTextureCoordinate(new Vec2f(vert.u(), vert.v()))
                    .setEffectiveJointIDs(new Vec3f(joint, 0, 0))
                    .setEffectiveJointWeights(new Vec3f(1.0F, 0.0F, 0.0F))
                    .setEffectiveJointNumber(1));
        }
        // The IntList is NOT a triangle index list: its entries are read in triples
        // (position, uv, normal) defining the vertices of the part in draw order
        // (see SingleGroupVertexBuilder#loadVertexInformation -> VertexBuilder.create).
        // So every drawn vertex is emitted as one triple (i, i, i). Fan triangulation:
        // triangles (0, i, i+1), same as EF's quad pattern (0,1,3)(3,1,2).
        for (int i = 1; i < count - 1; i++) {
            list.add(base);        list.add(base);        list.add(base);
            list.add(base + i);    list.add(base + i);    list.add(base + i);
            list.add(base + i + 1);list.add(base + i + 1);list.add(base + i + 1);
        }
        vertexCount[0] = base + count;
    }
}
