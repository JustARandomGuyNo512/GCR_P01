package com.sheridan.gcr.client.animation.command;

import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.animation.IAnimationSequence;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.ModuleRenderNode;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ShadowNodeRender extends Command{
    public static final int SHADOW_NODE_RENDER_KEY = 70000;
    public String refBoneName;
    public String targetBoneName;
    public boolean copyNodeStatus;
    public float startProgress;
    public float startTimestamp;
    private final ShadowEntry tempEntry;
    private long lastStartTime = 0;
    private IAnimationSequence lastSequence = null;

    public ShadowNodeRender(String command, float timeStamp) {
        super(command, timeStamp);
        if (args.size() != 4) {
            throw new IllegalArgumentException("Invalid command args: " + command);
        }
        startTimestamp = Float.parseFloat(args.getFirst());
        refBoneName = args.get(1);
        targetBoneName = args.get(2);
        copyNodeStatus = Boolean.parseBoolean(args.get(3));
        tempEntry = new ShadowEntry(targetBoneName);
    }

    @Override
    public void bindDef(AnimationDef def) {
        super.bindDef(def);
        startProgress = startTimestamp / def.lengthInSeconds();
    }

    @Override
    public void onFrame(IAnimated animated, IAnimationSequence sequence, ModuleRenderContext context) {
        if (sequence.getCurrentAnimatingProgress() >= startProgress) {
            ModuleRenderNode moduleRenderNode = context.currentRenderNode();
            List<ModuleRenderNode> moduleRenderNodes = moduleRenderNode.slots.get(refBoneName);
            if (moduleRenderNodes == null || moduleRenderNodes.isEmpty()) {
                return;
            }
            if (tempEntry.shadowNode == null || (sequence.getStartTime() != lastStartTime || lastSequence != sequence)) {//copy status if enabled
                ModuleRenderNode shadowRefNode = moduleRenderNodes.getFirst();
                tempEntry.shadowNode = shadowRefNode.copySelfOnly(copyNodeStatus);
                lastStartTime = sequence.getStartTime();
                lastSequence = sequence;
            }
            ShadowEntries localStorage = context.getLocalStorage(SHADOW_NODE_RENDER_KEY, ShadowEntries.class);
            if (localStorage == null) {
                localStorage = new ShadowEntries();
                context.setLocalStorage(SHADOW_NODE_RENDER_KEY, localStorage);
            }
            tempEntry.node = moduleRenderNode;
            localStorage.entries.add(tempEntry);
        }
    }

    public static class ShadowEntries{
        public List<ShadowEntry> entries;

        public ShadowEntries() {
            entries = new ArrayList<>();
        }
    }

    public static class ShadowEntry {
        public ModuleRenderNode node;
        public String targetBoneName;
        public ModuleRenderNode shadowNode;

        public ShadowEntry(String targetBoneName) {
            this.targetBoneName = targetBoneName;
        }
    }
}
