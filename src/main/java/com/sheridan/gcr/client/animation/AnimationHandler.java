package com.sheridan.gcr.client.animation;

import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class AnimationHandler {
    public static final String RELOAD = "reload";
    public static final String HAND_ACTION = "hand_action";
    public static final String INSPECT = "inspect";
    private final Deque<AnimationInstance> recoils = new ArrayDeque<>();
    public static final AnimationHandler INSTANCE = new AnimationHandler();
    private static final int MAX_KEYFRAME_ANIMATION_LEN = 12;
    private static final Map<String, IAnimationSequence> animations = new HashMap<>();

    protected AnimationHandler() {
    }

    /**
     * push keyframe animation recoil
     * */
    public void pushRecoil(AnimationDef recoilAnimation, long shootTime) {
        if (recoils.size() < MAX_KEYFRAME_ANIMATION_LEN) {
            recoils.add(new AnimationInstance(recoilAnimation, shootTime));
        } else {
            recoils.poll();
            recoils.add(new AnimationInstance(recoilAnimation, shootTime));
        }
    }

    public IAnimationSequence get(String name) {
        return animations.get(name);
    }

    public long getReloadStartTime() {
        return getStartTime(RELOAD);
    }

    public float getReloadLengthIfHas() {
        return getLengthIfHas(RELOAD);
    }

    public long getStartTime(String channel) {
        IAnimationSequence sequence = animations.get(channel);
        return sequence == null ? 0 : sequence.getStartTime();
    }

    public float getLengthIfHas(String channel) {
        IAnimationSequence sequence = animations.get(channel);
        return sequence == null ? Float.NaN : sequence.getStartTime();
    }

    public void apply(IAnimated root, String name, ModuleRenderContext context) {
        IAnimationSequence sequence = animations.get(name);
        if (sequence != null && root != null) {
            sequence.apply(root, context);
        }
    }


    public boolean has(String name) {
        IAnimationSequence sequence = animations.get(name);
        return sequence != null;
    }

    public void clearAnimation(String channel) {
        IAnimationSequence remove = animations.remove(channel);
        if (remove != null) {
            remove.removed();
        }
    }

    public void clearAllAnimation() {
        for (IAnimationSequence sequence : animations.values()) {
            sequence.removed();
        }
        animations.clear();
    }

    public void startReload(AnimationDef animationDefinition) {
        startAnimation(RELOAD, animationDefinition, true, true);
    }

    public void startReload(IAnimationSequence sequence) {
        startAnimation(RELOAD, sequence);
    }

    public void startHandAction(AnimationDef animationDefinition) {
        startAnimation(HAND_ACTION, animationDefinition, true, true);
    }

    public void startInspect(AnimationDef animationDefinition) {
        startAnimation(INSPECT, animationDefinition, true, false);
    }

    public void startAnimation(String channel, AnimationDef animationDefinition, boolean enableSound, boolean soundOnServer) {
        if (animationDefinition == null) {
            return;
        }
        SingleAnimationSequence sequence = new SingleAnimationSequence(animationDefinition.asInstance().soundOnServer(soundOnServer).enableSound(enableSound));
        IAnimationSequence old = get(channel);
        if (old != null) {
            old.removed();
        }
        animations.put(channel, sequence);
    }

    public void startAnimation(String channel, AnimationDef animationDefinition, Vector3f scales, boolean enableSound, boolean soundOnServer) {
        if (animationDefinition == null) {
            return;
        }
        SingleAnimationSequence sequence = new SingleAnimationSequence(
                animationDefinition.asInstance()
                        .soundOnServer(soundOnServer)
                        .enableSound(enableSound)
                        .setScales(scales)
        );
        IAnimationSequence old = get(channel);
        if (old != null) {
            old.removed();
        }
        animations.put(channel, sequence);
    }

    public void startAnimation(String channel, IAnimationSequence sequence) {
        if (sequence == null) {
            return;
        }
        IAnimationSequence old = get(channel);
        if (old != null) {
            old.removed();
        }
        animations.put(channel, sequence);
    }

    public void onClientTick() {
        if (animations.isEmpty()) {
            return;
        }
        Set<String> finished = new HashSet<>();
        for (Map.Entry<String, IAnimationSequence> entry : animations.entrySet()) {
            if (entry.getValue().tick()) {
                finished.add(entry.getKey());
            }
        }
        if (!finished.isEmpty()) {
            for (String id : finished)  {
                IAnimationSequence remove = animations.remove(id);
                remove.removed();
            }
        }
    }

}
