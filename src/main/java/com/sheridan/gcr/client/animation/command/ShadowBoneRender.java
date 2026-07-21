package com.sheridan.gcr.client.animation.command;

import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.animation.IAnimationSequence;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.ModuleRenderNode;
import com.sheridan.gcr.modularSys.task.GunTaskHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShadowBoneRender extends Command{
    public String refBone;
    public String targetBone;
    public boolean copyRenderNode;
    public boolean copyRenderNodeStates;
    public float copyStatesTimestamp;
    public float startProgress;

    public ShadowBoneRender(String command, float timeStamp) {
        super(command, timeStamp);
    }

    @Override
    public void bindDef(AnimationDef def) {
        super.bindDef(def);
        startProgress = timeStamp / def.lengthInSeconds();
    }

    @Override
    public void onFrame(IAnimated animated, IAnimationSequence sequence, ModuleRenderContext context) {
        if (sequence.getCurrentAnimatingProgress() >= startProgress) {
            ModuleRenderNode moduleRenderNode = context.currentRenderNode();
        }
    }
}
