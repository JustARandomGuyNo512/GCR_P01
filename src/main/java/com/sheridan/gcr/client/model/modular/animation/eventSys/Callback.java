package com.sheridan.gcr.client.model.modular.animation.eventSys;

import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.ModuleRenderNode;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public interface Callback {
    void onTriggered(
            EventContext event
    );

    class EventContext {
        public final EventType type;
        public final ModuleRenderContext renderContext;
        private boolean cancel;
        private Map<String, String> params;
        private ModuleRenderNode eventRenderNode;
        private ReadOnlyTag states;

        public EventContext(EventType type, ModuleRenderContext renderContext) {
            this.type = type;
            this.renderContext = renderContext;
            this.cancel = false;
            this.states = new ReadOnlyTag(new CompoundTag());
        }

        public ReadOnlyTag getStates() {
            return states;
        }

        void setStates(ReadOnlyTag states) {
            if (states != null) {
                this.states = states;
            }
        }

        void setEventRenderNode(ModuleRenderNode node) {
            this.eventRenderNode = node;
        }

        @Nullable
        public ModuleRenderNode getEventRenderNode() {
            return eventRenderNode;
        }

        public Optional<ModuleRenderNode> getCurrentRenderNode() {
            return Optional.ofNullable(eventRenderNode);
        }

        public void cancel() {
            cancel = true;
        }

        public boolean isCanceled() {
            return cancel;
        }

        public void setParams(Map<String, String> params) {
            this.params = params;
        }

        public String getParam(String key) {
            if (params == null) {
                return null;
            }
            return params.get(key);
        }
    }
}
