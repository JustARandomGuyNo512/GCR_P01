package com.sheridan.gcr.client.animation.command;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.animation.IAnimationSequence;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExitAds extends Command{

    public ExitAds(String command, float timeStamp) {
        super(command, timeStamp);
    }

    @Override
    public void bindDef(AnimationDef def) {
        super.bindDef(def);
    }

    @Override
    public void onFrame(IAnimated animated, IAnimationSequence sequence, ModuleRenderContext context) {
        Client.exitAds();
    }
}
