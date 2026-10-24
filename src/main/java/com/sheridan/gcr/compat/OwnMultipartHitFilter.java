package com.sheridan.gcr.compat;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.entity.PartEntity;

/**
 * Keeps the firing entity's own multipart hitboxes from intercepting its
 * bullets and laser rays. Other entities' anatomical parts remain hittable.
 *
 * Uses only the standard Forge PartEntity API, so AftermatchDayZ is an
 * optional runtime integration rather than a required compile dependency.
 */
public final class OwnMultipartHitFilter {
    private OwnMultipartHitFilter() {}

    public static boolean isOwnerOrOwnedPart(Entity candidate, Entity owner) {
        if (candidate == null || owner == null) {
            return false;
        }
        if (candidate == owner) {
            return true;
        }
        return candidate instanceof PartEntity<?> part && part.getParent() == owner;
    }
}
