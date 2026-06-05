package com.sheridan.gcr.client.model.modular.animation.eventSys;

import com.sheridan.gcr.client.animation.*;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class Track<T extends IModularModel> {
    private final String name;
    private boolean activated;
    private List<Consumer<IAnimationSequence>> onPlayedCallbacks;
    private List<AppliedCallback<T>> onAppliedCallbacks;
    private String currName = null;

    public Track(String name) {
        this.name = name;
        this.activated = true;
    }

    public Track<T> addOnPlayed(Consumer<IAnimationSequence> onPlayed) {
        if (onPlayed == null) {
            return this;
        }
        if (onPlayedCallbacks == null) {
            onPlayedCallbacks = new ArrayList<>();
        }
        onPlayedCallbacks.add(onPlayed);
        return this;
    }

    public Track<T> addOnApplied(AppliedCallback<T> onApplied) {
        if (onApplied == null) {
            return this;
        }
        if (onAppliedCallbacks == null) {
            onAppliedCallbacks = new ArrayList<>();
        }
        onAppliedCallbacks.add(onApplied);
        return this;
    }

    public boolean isActivated() {
        return activated;
    }

    public void activate() {
        this.activated = true;
    }

    public void deactivate() {
        this.activated = false;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public String getName() {
        return name;
    }

    public void clear() {
        AnimationHandler.INSTANCE.clearAnimation(getCurrName());
    }

    public boolean hasAnimation() {
        return AnimationHandler.INSTANCE.has(getCurrName());
    }

    public void play(IAnimationSequence sequence) {
        AnimationHandler.INSTANCE.startAnimation(getCurrName(), sequence);
        handlePlayedCallback(sequence);
    }

    public void play(AnimationInstance instance) {
        SingleAnimationSequence sequence = new SingleAnimationSequence(instance);
        AnimationHandler.INSTANCE.startAnimation(getCurrName(), sequence);
        handlePlayedCallback(sequence);
    }

    public void play(AnimationInstance... instance) {
        AnimationSequence sequence = new AnimationSequence(instance);
        AnimationHandler.INSTANCE.startAnimation(getCurrName(), sequence);
        handlePlayedCallback(sequence);
    }

    private void handlePlayedCallback(IAnimationSequence sequence) {
        if (onPlayedCallbacks != null) {
            for (Consumer<IAnimationSequence> onPlayed : onPlayedCallbacks) {
                onPlayed.accept(sequence);
            }
        }
    }

    /**
     * 获取当前正在执行的动画序列
     *
     * @return 返回与当前名称关联的动画序列，如果不存在则返回null
     */
    @Nullable
    public IAnimationSequence getAnimating() {
        return AnimationHandler.INSTANCE.get(getCurrName());
    }

    @Nullable
    public AnimationInstance getCurrentInstance() {
        IAnimationSequence animating = getAnimating();
        if (animating == null) {
            return null;
        }
        return animating.getCurrentAnimating();
    }

    @Nullable
    public AnimationDef getAnimatingDef() {
        AnimationInstance animatingInstance = getCurrentInstance();
        return animatingInstance != null ? animatingInstance.animation : null;
    }

    /**
     * 获取当前动画序列的总进度
     *
     * @return 返回动画进度值，范围为0.0到1.0之间，如果无动画则返回0
     */
    public float getProgress() {
        IAnimationSequence animating = getAnimating();
        if (animating == null) {
            return 0;
        } else {
            return animating.getProgress();
        }
    }

    /**
     * 获取当前动画序列中正在播放的动画的进度
     *
     * @return 返回动画进度值，范围为0.0到1.0之间，如果无动画则返回0
     */
    public float getCurrentAnimatingProgress() {
        IAnimationSequence animating = getAnimating();
        if (animating == null) {
            return 0;
        } else {
            return animating.getCurrentAnimatingProgress();
        }
    }

    /**
     * 每帧将动画应用到指定的模型对象上
     *
     * @param model 需要应用动画的模型对象
     */
    public void applyToModel(T model, ModuleRenderContext context) {
        // 如果动画已激活，则通过动画处理器将指定名称的动画应用到模型上
        if (activated && hasAnimation()) {
            AnimationHandler.INSTANCE.apply(model, getCurrName(), context);
            if (onAppliedCallbacks != null) {
                for (AppliedCallback<T> onApplied : onAppliedCallbacks) {
                    onApplied.onApplied(context, model);
                }
            }
        }
    }

    public String getCurrName() {
        return currName == null ? name : currName;
    }

    public void clearNodeBinding() {
        currName = null;
    }

    public void onUsingNode(String id) {
        this.currName = id + getName();
    }

    public interface AppliedCallback<E extends IModularModel> {
        void onApplied(ModuleRenderContext context, E model);
    }
}
