package com.sheridan.gcr.client.model.modular.animation.eventSys;

import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.ModuleRenderNode;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class AnimationEventBus {
    Map<EventType, List<Triple<EventRegistry, ModuleRenderNode, IAnimationController<?>>>> events = new Object2ObjectOpenHashMap<>();

    public static AnimationEventBus empty() {
        return new AnimationEventBus();
    }

    public void register(EventRegistry registry, ModuleRenderNode node, IAnimationController<?> controller) {
        this.events.computeIfAbsent(registry.event, k -> new ArrayList<>()).add(Triple.of(registry, node, controller));
    }

    public void finish() {
        for (List<Triple<EventRegistry, ModuleRenderNode, IAnimationController<?>>> list : this.events.values()) {
            list.sort((o1, o2) -> {
                int priority1 = o1.getLeft().getPriority();
                int priority2 = o2.getLeft().getPriority();
                return priority2 - priority1;
            });
        }
    }

    public void dispatch(EventType type, ModuleRenderContext context, @Nullable Map<String, String> params) {
        List<Triple<EventRegistry, ModuleRenderNode, IAnimationController<?>>> triples = events.get(type);
        if (triples != null) {
            Callback.EventContext eventContext = new Callback.EventContext(type, context);
            if (params != null && !params.isEmpty()) {
                eventContext.setParams(params);
            }
            for (Triple<EventRegistry, ModuleRenderNode, IAnimationController<?>> triple : triples) {
                ModuleRenderNode node = triple.getMiddle();
                EventRegistry registry = triple.getLeft();
                IAnimationController<?> controller = triple.getRight();
                controller.onUsingNode(node.id);
                eventContext.setEventRenderNode(node);
                eventContext.setStates(node.getStates());
                registry.callback.onTriggered(eventContext);
                if (eventContext.isCanceled()) {
                    break;
                }
                controller.clearNode();
            }
        }
    }

}
