package com.sheridan.gcr.client.model.modular.state;

import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.views.IStateView;

import java.util.Optional;

public interface IStateViewer<T extends IStateView> {
    EmptyStateViewer EMPTY = new EmptyStateViewer();
    void onRegisterStateMapping();

    void addStateMapping(StaticState state);

    void addStateMappings(StaticState... state);

    void addStateMapping(String stateName, String animationPath, float scale, float progress);

    boolean removeStateMapping(String stateName);
    /**
     * 从资源池中获取动画定义
     * */
    Optional<AnimationDef> getAnimationDef(String path);

    StaticState getState(String stateName);

    Optional<StaticState> createState(String animationPath, String stateName, float scale, float progress);

    T getStateView();

    void applyState(IAnimated animated, ModuleRenderContext context, ReadOnlyTag states);


    class EmptyStateViewer implements IStateViewer<IStateView> {
        @Override
        public void onRegisterStateMapping() {
        }

        @Override
        public void addStateMapping(StaticState state) {
        }

        @Override
        public void addStateMappings(StaticState... state) {

        }

        @Override
        public void addStateMapping(String stateName, String animationPath, float scale, float progress) {
        }

        @Override
        public boolean removeStateMapping(String stateName) {
            return false;
        }

        @Override
        public Optional<AnimationDef> getAnimationDef(String path) {
            return Optional.empty();
        }

        @Override
        public StaticState getState(String stateName) {
            return StaticState.EMPTY;
        }

        @Override
        public Optional<StaticState> createState(String animationPath, String stateName, float scale, float progress) {
            return Optional.empty();
        }

        @Override
        public IStateView getStateView() {
            return null;
        }

        @Override
        public void applyState(IAnimated animated, ModuleRenderContext context, ReadOnlyTag states) {

        }
    }
}
