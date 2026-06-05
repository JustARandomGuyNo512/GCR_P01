package com.sheridan.gcr.client.animation.command;

import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.animation.IAnimationSequence;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.task.GunTaskHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MaskMagAmmo extends Command{
    public static final String VARIABLE_KEY = "mask_mag_ammo";
    public static final int MASK_AMMO_LEFT_KEY = 30001;

    private float startProgress;
    public MaskMagAmmo(String command, float timeStamp) {
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
            int taskCustomVariable = GunTaskHandler.INSTANCE.getTaskCustomVariable(VARIABLE_KEY);
            if (taskCustomVariable != -1) {
                context.setLocalStorage(MASK_AMMO_LEFT_KEY, taskCustomVariable);
            }
        }
    }
}
