package com.sheridan.gcr.client.model.modular.animation.controllers;

import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.AnimationInstance;
import com.sheridan.gcr.client.animation.AnimationRegister;
import com.sheridan.gcr.client.animation.KeyframeAnimator;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.animation.eventSys.*;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public abstract class AnimationController<T extends IModularModel> implements IAnimationController<T> {
    protected final Map<String, Track<T>> trackMap = new HashMap<>();
    protected final Map<String, AnimationDef> animationPool = new HashMap<>();
    protected final List<EventRegistry> eventRegistries = new ArrayList<>();
    private ModuleRenderContext tempContext = null;

    @Override
    public AnimationDef registerAnimation(String simpleName, ResourceLocation path) {
        AnimationDef def = AnimationRegister.get(path);
        if (def == null) {
            throw new IllegalArgumentException("Animation not found at: " + path);
        }
        animationPool.put(simpleName, def);
        return def;
    }

    public void registerAnimation(String simpleName, String path) {
        registerAnimation(simpleName, ResourceLocation.parse(path));
    }

    public void registerAnimations(String... nameAndPathPairs) {
        if (nameAndPathPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid number of arguments");
        }
        for (int i = 0; i < nameAndPathPairs.length; i += 2) {
            registerAnimation(nameAndPathPairs[i], nameAndPathPairs[i + 1]);
        }
    }

    @Override
    public Track<T> defineTrack(String name) {
        if (trackMap.containsKey(name)) {
            throw new IllegalArgumentException("Track already exists: " + name);
        }
        Track<T> track = new Track<>(name);
        trackMap.put(name, track);
        return track;
    }

    @Override
    public Track<T> getTrack(String name) {
        return trackMap.get(name);
    }


    @Override
    public List<Track<T>> getAllTracks() {
        return new ArrayList<>(trackMap.values());
    }

    @Override
    public void subscribe(EventType eventType, int priority, Callback callback) {
        eventRegistries.add(new EventRegistry(priority, eventType, callback));
    }

    @Override
    public List<EventRegistry> getAllSubscriptions() {
        return eventRegistries;
    }


    protected AnimationInstance anim(String name) {
        AnimationDef def = animationPool.get(name);
        if (def == null) {
            throw new IllegalStateException("Animation not found: " + name);
        }
        return def.asInstance();
    }

    protected AnimationDef animDef(String name) {
        return animationPool.get(name);
    }


    @Override
    public void clearNode() {
        for (Track<?> track : trackMap.values()) {
            track.clearNodeBinding();
        }
    }

    @Override
    public void onUsingNode(String id) {
        for (Track<?> track : trackMap.values()) {
            track.onUsingNode(id);
        }
    }

    @Override
    public boolean isTrackClear(String trackName) {
        Track<T> track = getTrack(trackName);
        return track == null || !track.hasAnimation();
    }

    @Override
    public boolean areTracksClear(String... trackNames) {
        for (String trackName : trackNames) {
            Track<T> track = getTrack(trackName);
            if (track != null && track.hasAnimation()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onUsingContext(ModuleRenderContext context) {
        this.tempContext = context;
    }

    public void animate(String animationName, long startTime) {
        AnimationDef animationDef = animDef(animationName);
        if (animationDef == null) {
            return;
        }
        if (tempContext != null) {
            this._animate(animationDef, startTime);
        }
    }

    public void stateOrAnimate(String animationName, long startTime, String coverState) {
        if (tempContext == null) {
            return;
        }
        AnimationDef animation = animDef(animationName);
        if (animation == null) {
            return;
        }
        this._animate(animation, startTime);
        float timeDist = KeyframeAnimator.dist(startTime, 0, animation.looping(), animation.keepOnLastFrame(), animation, 1);
        if (!Float.isNaN(timeDist)) {
            tempContext.addStateLockBone(coverState);
        }
    }

    public void statesOrAnimate(String animationName, long startTime, String... coverStates) {
        if (tempContext == null) {
            return;
        }
        AnimationDef animation = animDef(animationName);
        if (animation == null) {
            return;
        }
        this._animate(animation, startTime);
        float timeDist = KeyframeAnimator.dist(startTime, 0, animation.looping(), animation.keepOnLastFrame(), animation, 1);
        if (!Float.isNaN(timeDist)) {
            for (String coverState : coverStates) {
                tempContext.addStateLockBone(coverState);
            }
        }
    }

    private void _animate(AnimationDef animation, long startTime) {
        KeyframeAnimator._animate(tempContext.currentRenderNode().model, animation, startTime, 0, 0.0625f, 0.0625f, 0.0625f, animation.looping(), animation.keepOnLastFrame(), 1);
    }

    @Override
    public void clearTracks(String... trackNames) {
        for (String trackName : trackNames) {
            Track<T> track = getTrack(trackName);
            if (track != null) {
                track.clear();
            }
        }
    }

    @Override
    public void clearTrack(String trackName) {
        Track<T> track = getTrack(trackName);
        if (track != null) {
            track.clear();
        }
    }
}
