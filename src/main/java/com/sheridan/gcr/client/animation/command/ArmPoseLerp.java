package com.sheridan.gcr.client.animation.command;

import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.animation.IAnimationSequence;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.RenderConstants;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArmPoseLerp extends Command{
    public float lerpInStart;
    public float lerpInEnd;
    public float lerpOutStart;
    public float lerpOutEnd;

    public boolean leftArm;
    public float distIn;
    public float distOut;

    public ArmPoseLerp(String command, float timeStamp) {
        super(command, timeStamp);
        if (args.size() != 3) {
            throw new IllegalArgumentException("Invalid command args: " + command);
        }
        this.leftArm = this.command.contains("left");
        this.lerpInStart = this.timeStamp;
    }

    @Override
    public void bindDef(AnimationDef def) {
        super.bindDef(def);
        float length = def.lengthInSeconds();
        lerpInStart = timeStamp / length;
        lerpInEnd = Float.parseFloat(args.getFirst()) / length;
        lerpOutStart = Float.parseFloat(args.get(1)) / length;
        lerpOutEnd = Float.parseFloat(args.get(2)) / length;
        distIn = 1 / (lerpInEnd - lerpInStart);
        distOut = 1 / (lerpOutEnd - lerpOutStart);
    }

    @Override
    public void onFrame(IAnimated animated, IAnimationSequence sequence, ModuleRenderContext context) {
        float f = sequence.getCurrentAnimatingProgress();
        if (f < lerpInStart || f > lerpOutEnd) {
            return;
        }
        if (leftArm) {
            context.setLocalStorage(RenderConstants.ANIMATED_LEFT_ARM_MODEL, animated);
            float progress = getProgress(f);
            context.setLocalStorage(RenderConstants.LEFT_ARM_LERP_CONTROL, progress);
        } else {
            context.setLocalStorage(RenderConstants.ANIMATED_RIGHT_ARM_MODEL, animated);
            float progress = getProgress(f);
            context.setLocalStorage(RenderConstants.RIGHT_ARM_LERP_CONTROL, progress);
        }
    }

    private float getProgress(float animProgress) {
        if (animProgress >= lerpInEnd && animProgress <= lerpOutStart) {
            return 1;
        } else if (animProgress < lerpInEnd) {
            return Mth.clamp((animProgress - lerpInStart) * distIn, 0, 1);
        } else if (animProgress < lerpOutEnd) {
            return 1 - Mth.clamp((animProgress - lerpOutStart) * distOut, 0, 1);
        }
        return 0;
    }
}