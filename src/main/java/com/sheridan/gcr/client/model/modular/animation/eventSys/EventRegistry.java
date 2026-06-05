package com.sheridan.gcr.client.model.modular.animation.eventSys;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EventRegistry {
    public int priority;
    public EventType event;
    public Callback callback;

    public EventRegistry(int priority, EventType event, Callback callback) {
        this.priority = priority;
        this.event = event;
        this.callback = callback;
    }

    public int getPriority() {
        return priority;
    }
}
