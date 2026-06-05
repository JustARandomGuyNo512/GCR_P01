package com.sheridan.gcr.client.model.modular.state;

import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.AnimationRegister;
import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.views.IStateView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class StateViewer<T extends IStateView> implements IStateViewer<T> {
    protected final Map<String, StaticState> states = new HashMap<>();
    protected final float DEFAULT_SCALE = 1/16f;
    private final T stateView;

    public StateViewer(T view) {
        if (view == null) {
            throw new IllegalArgumentException("StateView cannot be null");
        }
        this.stateView = view;
    }

    @Override
    public void addStateMapping(StaticState state) {
        if (state == null) {
            return;
        }
        states.put(state.name, state);
    }

    @Override
    public void addStateMappings(StaticState... state) {
        for (StaticState s : state) {
            addStateMapping(s);
        }
    }

    @Override
    public boolean removeStateMapping(String stateName) {
        return states.remove(stateName) != null;
    }

    @Override
    public void addStateMapping(String stateName, String animationPath, float scale, float progress) {
        progress = Mth.clamp(progress, 2.5e-3f, 1f - 2.5e-3f);
        createState(animationPath, stateName, scale, progress).ifPresent(state -> states.put(state.name, state));
    }

    @Override
    public Optional<AnimationDef> getAnimationDef(String path) {
        AnimationDef animationDef = AnimationRegister.get(ResourceLocation.parse(path));
        return animationDef== null ? Optional.empty() : Optional.of(animationDef);
    }

    @Override
    public StaticState getState(String stateName) {
        return states.get(stateName);
    }

    @Override
    public Optional<StaticState> createState(String animationPath, String stateName, float scale, float progress) {
        AnimationDef animationDef = AnimationRegister.get(ResourceLocation.parse(animationPath));
        if (animationDef == null) {
            throw new IllegalArgumentException("Animation not found: " + animationPath);
        }
        StaticState state = StaticState.fromAnimation(stateName, animationDef, scale, progress);
        return Optional.of(state);
    }

    protected void doPose(String staticStateName, IAnimated animated, ModuleRenderContext context) {
        StaticState state = getState(staticStateName);
        if (state != null) {
            state.apply(animated, context);
        }
    }

    @Override
    public T getStateView() {
        return stateView;
    }
}
