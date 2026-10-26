package com.sheridan.gcr.modularSys.builder;

import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.ModuleRegister;
import com.sheridan.gcr.modularSys.IVoxel;
import com.sheridan.gcr.modularSys.modules.IVoxelHandlerModule;
import com.sheridan.gcr.modularSys.slot.IRail;
import com.sheridan.gcr.modularSys.slot.MLokRail;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests actual mounting space on a rail (physical footprint + engine
 * collisions/boundaries). A slot's maxCapacity is only an upper bound,
 * NOT evidence that an additional attachment will fit.
 *
 * Results are calculated against a copy of the workspace. Never move
 * installed attachments when searching for an empty position.
 */
public final class RailSpace {
    private static final float MIN_FOOTPRINT = 0.085f;
    private static final float CLEARANCE = 0.008f;
    private static final float EPS = 0.0001f;

    private RailSpace() {}

    /**
     * A regular one-capacity slot retains its traditional behavior.
     * For a rail, search real compatible items and stop on the first fit.
     */
    public static boolean hasRoom(WorkSpace workspace, SlotInstance slot) {
        if (slot == null || slot.isHidden()
                || workspace.getNodes(slot).size() >= slot.getSlot().maxCapacity()) return false;
        if (!(slot.getSlot() instanceof IRail)) return true;
        for (IModular module : ModuleRegister.all().values()) {
            if (module.getBindItem() != null && slot.accepts(module)
                    && findPosition(workspace, slot, module) >= 0f) return true;
        }
        return false;
    }

    /** Negative means the chosen attachment cannot be fitted anywhere. */
    public static float findPosition(WorkSpace workspace, SlotInstance slot, IModular module) {
        if (slot == null || module == null || !slot.accepts(module)
                || workspace.getNodes(slot).size() >= slot.getSlot().maxCapacity())
            return -1f;
        if (!(slot.getSlot() instanceof IRail rail)) return 0f;

        // First attachment is allowed to occupy the ordinary pivot position.
        // The expensive geometric search applies when there is already
        // another attachment occupying the same rail.
        if (workspace.getNodes(slot).isEmpty())
            return rail.getNormalizedOriginOffest();

        WorkSpace probe = workspace.deepCopy();
        Node parent = workspace.getSlotParent(slot);
        if (parent == null) return -1f;
        Unit copiedParent = probe.findUnitById(parent.getID()).orElse(null);
        Node copiedParentNode = copiedParent == null ? null : probe.getNode(copiedParent);
        SlotInstance copiedSlot = copiedParentNode == null
                ? null : copiedParentNode.getSlotByName(slot.slotName());
        if (copiedSlot == null) return -1f;

        Unit candidate = probe.addChild(copiedSlot, module).orElse(null);
        if (candidate == null) return -1f;
        CollisionHandler collision = new CollisionHandler(probe);
        Accessor writable = new Accessor(probe, true);
        Accessor readOnly = new Accessor(probe);

        float[] positions = positions(rail);
        for (float position : positions) {
            rail.setChildPosition(candidate, writable, position);
            // CollisionHandler caches transforms by mutation id; offsets
            // mutate in place and therefore need an explicit invalidation.
            probe.increaseMutateId();
            collision.update();
            Pair<Boolean, Boolean> boundary = collision.collisionWithRailBoundary(candidate);
            if (boundary.getLeft() || boundary.getRight()
                    || !collision.collision(candidate).isEmpty()) continue;
            if (overlapsRailFootprints(candidate, probe.getNodes(copiedSlot),
                    rail, readOnly)) continue;
            return position;
        }
        return -1f;
    }

    private static float[] positions(IRail rail) {
        if (rail instanceof MLokRail mlok)
            return mlok.getNormalizedDiscreteNodes().clone();

        // Prefer near the mounting pivot, then search along the whole rail.
        List<Float> values = new ArrayList<>();
        float origin = rail.getNormalizedOriginOffest();
        values.add(origin);
        for (int i = 0; i <= 64; i++) {
            float p = i / 64f;
            if (Math.abs(p - origin) > EPS) values.add(p);
        }
        values.sort((a, b) -> Float.compare(Math.abs(a - origin), Math.abs(b - origin)));
        float[] output = new float[values.size()];
        for (int i = 0; i < values.size(); i++) output[i] = values.get(i);
        return output;
    }

    private static boolean overlapsRailFootprints(Unit candidate, List<Node> siblings,
                                                   IRail rail, Accessor accessor) {
        float[] candidateSpan = span(candidate, accessor);
        for (Node existing : siblings) {
            Unit other = existing.getUnit();
            if (other == candidate) continue;
            float[] otherSpan = span(other, accessor);
            // Relative offsets are in local rail Z coordinates. Consider
            // the mounting footprint, not the entire visual screen position.
            if (candidateSpan[0] < otherSpan[1] + CLEARANCE
                    && candidateSpan[1] > otherSpan[0] - CLEARANCE)
                return true;
        }
        float half = rail.getLength();
        // The collision handler handles the physical bounds; this extra
        // check catches missing/underspecified collision voxels.
        float rear = rail.getNormalizedOriginOffest() * half;
        float front = -(1f - rail.getNormalizedOriginOffest()) * half;
        return candidateSpan[0] < front - EPS || candidateSpan[1] > rear + EPS;
    }

    private static float[] span(Unit unit, Accessor accessor) {
        float min = -MIN_FOOTPRINT * .5f;
        float max = MIN_FOOTPRINT * .5f;
        if (unit.getModule() instanceof IVoxelHandlerModule voxelModule
                && voxelModule.getHandler() != null) {
            IVoxel voxel = voxelModule.getHandler().getVoxel(unit, accessor);
            if (voxel != null) {
                AABB box = voxel.getBoundingBox();
                if (Double.isFinite(box.minZ) && Double.isFinite(box.maxZ)) {
                    min = (float) box.minZ;
                    max = (float) box.maxZ;
                    // Some models have a very small collision-box cross
                    // section; preserve a minimum rail mounting footprint.
                    float center = (min + max) * .5f;
                    if (max - min < MIN_FOOTPRINT) {
                        min = center - MIN_FOOTPRINT * .5f;
                        max = center + MIN_FOOTPRINT * .5f;
                    }
                }
            }
        }
        return new float[] { min + unit.getZOffset(), max + unit.getZOffset() };
    }
}
