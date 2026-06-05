package com.sheridan.gcr.client.animation;

import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public interface IAnimationSequence {

    IAnimationSequence append(AnimationInstance instance);

    void apply(IAnimated root, ModuleRenderContext context);

    /**
     * returns true if sequence is finished
     * */
    boolean tick();

    boolean applying();

    long getStartTime();

    float getLength();

    IAnimationSequence finishBuild();

    @Nullable
    AnimationInstance getCurrentAnimating();

    float getProgress();

    float getCurrentAnimatingProgress();

    int size();

    AnimationInstance get(int index);

    void onRemoved(Consumer<IAnimationSequence> onRemoved);

    void removed();

    IAnimationSequence prepare();
}
