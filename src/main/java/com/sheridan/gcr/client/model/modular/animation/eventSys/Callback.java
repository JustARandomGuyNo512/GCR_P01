package com.sheridan.gcr.client.model.modular.animation.eventSys;

import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;

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

        public ReadOnlyTag getStates() {
            return renderContext.currentRenderNode().getStates();
        }

        public EventContext(EventType type, ModuleRenderContext renderContext) {
            this.type = type;
            this.renderContext = renderContext;
            cancel = false;
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
