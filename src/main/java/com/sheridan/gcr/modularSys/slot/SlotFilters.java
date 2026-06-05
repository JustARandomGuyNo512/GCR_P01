package com.sheridan.gcr.modularSys.slot;

import com.sheridan.gcr.modularSys.IModular;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.function.Function;

public class SlotFilters {

    // ===== TAG 相关 =====

    public static ISlotFilter hasTag(String tag) {
        return m -> m.getTags().contains(tag);
    }

    public static ISlotFilter hasAnyTag(Set<String> tags) {
        return m -> {
            for (String tag : tags) {
                if (m.getTags().contains(tag)) return true;
            }
            return false;
        };
    }

    public static ISlotFilter hasAllTags(Set<String> tags) {
        return m -> m.getTags().containsAll(tags);
    }

    public static ISlotFilter hasAllTags(String... tags) {
        Set<String> set = Set.of(tags);
        return m -> m.getTags().containsAll(set);
    }

    public static ISlotFilter exactTags(Set<String> tags) {
        return m -> m.getTags().equals(tags);
    }

    public static ISlotFilter notHaveTags(Set<String> tags) {
        return m -> !m.getTags().containsAll(tags);
    }

    // ===== modular ID =====

    public static ISlotFilter modular(String id) {
        return m -> id.equals(m.getID());
    }

    public static ISlotFilter notModular(String id) {
        return m -> !id.equals(m.getID());
    }

    public static ISlotFilter notModular(ResourceLocation id) {
        return m -> !id.toString().equals(m.getID());
    }

    public static ISlotFilter modularIn(Set<String> ids) {
        return m -> ids.contains(m.getID());
    }

    // ===== 自定义 =====

    public static ISlotFilter custom(Function<IModular, Boolean> func) {
        return func::apply;
    }
}
