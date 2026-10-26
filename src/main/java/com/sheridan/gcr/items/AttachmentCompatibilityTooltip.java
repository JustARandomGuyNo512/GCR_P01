package com.sheridan.gcr.items;

import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.ISlotProviderModular;
import com.sheridan.gcr.modularSys.ModuleRegister;
import com.sheridan.gcr.modularSys.modules.Mag;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.slot.ISlot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Conservative list of gun families whose attachment trees expose a slot
 * compatible with a module, including slots added by intermediate adapters,
 * rails, handguards and muzzle mounts. Derived from registered slot filters
 * instead of hardcoded per-item names. This is informative, not a replacement
 * for builder collision/compatibility validation.
 *
 * Universal rail-mounted items are intentionally omitted: their usability
 * depends on the rail/adapter installed, not on the bare gun model.
 */
public final class AttachmentCompatibilityTooltip {
    private static volatile Map<String, List<String>> byModule;
    private AttachmentCompatibilityTooltip() {}

    public static void append(IModular attachment, List<Component> tooltip) {
        if (attachment instanceof Mag || attachment instanceof IGun
                || attachment.getBindItem() == null
                || attachment.hasTag("on_rail")
                || attachment.hasTag("m_lok_rail_fit")) return;
        Map<String, List<String>> index = byModule;
        if (index == null) {
            synchronized (AttachmentCompatibilityTooltip.class) {
                index = byModule;
                if (index == null) byModule = index = buildIndex();
            }
        }
        List<String> compatible = index.getOrDefault(attachment.getID(), List.of());
        if (compatible.isEmpty()) return;
        tooltip.add(Component.translatable("tooltip.gcr.attachment_compatible")
                .withStyle(ChatFormatting.GRAY));
        for (String gunId : compatible)
            tooltip.add(Component.literal("  • ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.translatable(gunId).withStyle(ChatFormatting.GRAY)));
    }

    private static Map<String, List<String>> buildIndex() {
        List<IModular> all = new ArrayList<>(ModuleRegister.all().values());
        all.sort(Comparator.comparing(IModular::getID));
        List<IModular> guns = all.stream()
                .filter(module -> module instanceof IGun && module instanceof ISlotProviderModular)
                .toList();
        Map<String, List<String>> compatible = new HashMap<>();
        // Cache the filter decisions shared by multiple guns and by all
        // possible intermediate attachment/adapter paths.
        Map<ISlot, List<IModular>> choicesBySlot = new IdentityHashMap<>();
        for (IModular gun : guns) {
            Set<String> visited = new HashSet<>();
            ArrayDeque<IModular> queue = new ArrayDeque<>();
            queue.add(gun);
            visited.add(gun.getID());
            while (!queue.isEmpty()) {
                IModular parent = queue.removeFirst();
                if (!(parent instanceof ISlotProviderModular slotted)) continue;
                for (ISlot slot : slotted.getSlotProvider().getSlots()) {
                    List<IModular> choices = choicesBySlot.computeIfAbsent(slot, key -> {
                        List<IModular> accepted = new ArrayList<>();
                        for (IModular candidate : all) {
                            if (!(candidate instanceof IGun) && key.accepts(candidate))
                                accepted.add(candidate);
                        }
                        return accepted;
                    });
                    for (IModular candidate : choices) {
                        if (!visited.add(candidate.getID())) continue;
                        if (candidate.getBindItem() != null)
                            compatible.computeIfAbsent(candidate.getID(), id -> new ArrayList<>())
                                    .add(gun.getID());
                        if (candidate instanceof ISlotProviderModular)
                            queue.addLast(candidate);
                    }
                }
            }
        }
        Map<String, List<String>> result = new HashMap<>();
        compatible.forEach((id, names) -> result.put(id, List.copyOf(names)));
        return Map.copyOf(result);
    }
}
