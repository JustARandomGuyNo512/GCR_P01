package com.sheridan.gcr.client.model.modular;

import com.sheridan.gcr.client.animation.AnimationInstance;
import com.sheridan.gcr.client.animation.IAnimationSequence;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.animation.eventSys.IAnimationController;
import com.sheridan.gcr.client.model.modular.animation.eventSys.Track;
import com.sheridan.gcr.client.model.modular.state.IStateViewer;
import com.sheridan.gcr.client.model.modular.state.IStateViewerModel;
import com.sheridan.gcr.client.render.FirstPersonRenderContext;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.views.IStateView;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class AnimatedModel<T extends IStateView> extends ModularModel implements IAnimationControllerModel, IStateViewerModel<T> {
    protected @Nullable IAnimationController<?> controller;
    protected IStateViewer<T> viewer;

    public AnimatedModel(MeshModelData root, ResourceLocation name, IStateViewer<T> viewer) {
        super(root, name);
        this.viewer = viewer;
    }

    @Override
    public Optional<IAnimationController<?>> getController() {
        if (controller == null) {
            return Optional.empty();
        }
        return Optional.of(controller);
    }

    @Override
    public void bindController(IAnimationController<?> controller) {
        if (controller == null) {
            throw new IllegalArgumentException("controller cannot be null");
        }
        if (!controller.assertCompatible(this)) {
            throw new IllegalArgumentException("controller is not compatible with model");
        }
        this.controller = controller;
    }

    @Override
    public void callInitEventSubscriptions() {
        if (controller != null) {
            withController(c -> c.firstPersonSubscriptions(this));
        }
    }

    @Override
    public void callInitAnimation() {
        if (controller != null) {
            withController(c -> c.initAnimation(this));
        }
    }

    @Override
    public void callInitTrack() {
        if (controller != null) {
            withController(c -> c.initTrack(this));
        }
    }

    @SuppressWarnings("unchecked")
    private <E extends AnimatedModel<?>> void withController(Consumer<IAnimationController<E>> action) {
        IAnimationController<E> typed = (IAnimationController<E>) controller;
        action.accept(typed);
    }

    @SuppressWarnings("unchecked")
    private <E extends IModularModel> void applyTrack(Track<E> track, ModuleRenderContext context) {
        track.applyToModel((E) this, context);
    }

    @Override
    public void applyFirstPersonAnimation(ModuleRenderContext context) {
        if (controller != null) {
            controller.onUsingContext(context);
            for (Track<?> track : controller.getAllTracks()) {
                track.onUsingNode(context.currentRenderNode().id);
                applyTrack(track, context);
                IAnimationSequence animating = track.getAnimating();
                track.clearNodeBinding();
                if (animating == null || !animating.applying()) {
                    continue;
                }
                AnimationInstance currentInstance = animating.getCurrentAnimating();
                if (currentInstance == null || !currentInstance.isCoverState()) {
                    continue;
                }
                if (currentInstance.hasExcludeCoverBones()) {
                    Set<String> strings = currentInstance.animation.allBones();
                    for (String s : strings) {
                        if (currentInstance.wasBoneExcludeCovered(s)) {
                            continue;
                        }
                        context.addStateLockBone(s);
                    }
                } else {
                    context.addStateLockBones(currentInstance.animation.allBones());
                }
            }
            controller.onUsingContext(null);
        }
    }

    @Override
    public void applyThirdPersonAnimation(ModuleRenderContext context) {
        callThirdPersonAnimation(this, context);
    }

    @Override
    public void applyCustomFirstPersonAnimation(FirstPersonRenderContext firstPersonRenderContext) {
        callCustomFirstPersonAnimation(this, firstPersonRenderContext);
    }

    @SuppressWarnings("unchecked")
    private <E extends IModularModel> void callCustomFirstPersonAnimation(E model, FirstPersonRenderContext context) {
        if(controller != null) {
            controller.onUsingContext(context);
            IAnimationController<E> typed = (IAnimationController<E>) controller;
            typed.customFirstPersonAnimation(model, context);
            controller.onUsingContext(null);
        }
    }

    @SuppressWarnings("unchecked")
    private <E extends IModularModel> void callThirdPersonAnimation(E model, ModuleRenderContext context) {
        if(controller != null) {
            controller.onUsingContext(context);
            IAnimationController<E> typed = (IAnimationController<E>) controller;
            typed.customThirdPersonAnimation(model, context);
            controller.onUsingContext(null);
        }
    }

    @Override
    public IStateViewer<T> getViewer() {
        return viewer;
    }

}
