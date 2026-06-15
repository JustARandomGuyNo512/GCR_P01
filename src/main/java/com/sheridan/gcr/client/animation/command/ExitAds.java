package com.sheridan.gcr.client.animation.command;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.animation.IAnimationSequence;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.task.GunTaskHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExitAds extends Command{
    private long lastExec = 0;
    private int coolDown;

    public ExitAds(String command, float timeStamp) {
        super(command, timeStamp);
    }

    @Override
    public void bindDef(AnimationDef def) {
        super.bindDef(def);
        coolDown = (int) (def.lengthInSeconds() * 1000);
    }

    @Override
    public void onFrame(IAnimated animated, IAnimationSequence sequence, ModuleRenderContext context) {
        long l = System.currentTimeMillis();
        if (l - lastExec >= coolDown) {
            lastExec = l;
            Client.exitAds();
        }
    }
}
