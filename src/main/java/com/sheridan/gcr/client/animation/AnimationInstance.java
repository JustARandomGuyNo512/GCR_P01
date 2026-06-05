package com.sheridan.gcr.client.animation;

import com.sheridan.gcr.Utils;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class AnimationInstance {
    private static final Vector3f DEFAULT_SCALE = new Vector3f(1, 1, 1);
    public AnimationDef animation;
    public long timeStamp;
    public Vector3f scales = new Vector3f(DEFAULT_SCALE);
    private int tick = 0;
    private int prevSoundIndex = -1;
    private boolean enableSound = false;
    public boolean soundOnServer = false;
    public boolean keepOnLastFrame = false;
    public int loopTimes = 0;
    public int looped = 0;
    private float speed = 1;

    private boolean coverState = false;
    public Consumer<Float> onPlaying;
    private Set<String> excludeCoverBones;

    public AnimationInstance(AnimationDef animation, long timeStamp, Vector3f scales) {
        this.animation = animation;
        this.timeStamp = timeStamp;
        this.scales = scales;
    }

    public AnimationInstance(AnimationDef animation, long timeStamp) {
        this.animation = animation;
        this.timeStamp = timeStamp;
    }

    public AnimationInstance(AnimationDef animation) {
        this.animation = animation;
        this.timeStamp = 0L;
    }

    public AnimationInstance(AnimationDef animation, Vector3f scales) {
        this.animation = animation;
        this.timeStamp = 0L;
        this.scales = scales;
    }

    public AnimationInstance setScales(Vector3f scales) {
        this.scales.set(scales);
        return this;
    }

    public AnimationInstance setScales(float x, float y, float z) {
        this.scales.x = x;
        this.scales.y = y;
        this.scales.z = z;
        return this;
    }



    public AnimationInstance setScale(float scale) {
        this.scales.x = scale;
        this.scales.y = scale;
        this.scales.z = scale;
        return this;
    }


    public AnimationInstance enableSound(boolean enableSound) {
        this.enableSound = enableSound;
        return this;
    }

    public boolean isCoverState() {
        return coverState;
    }

    public AnimationInstance coverState() {
        this.coverState = true;
        return this;
    }

    public AnimationInstance setSpeed(float speed) {
        this.speed = speed;
        return this;
    }

    public AnimationInstance keepOnLastFrame() {
        this.keepOnLastFrame = true;
        return this;
    }

    public float getSpeed() {
        return speed;
    }

    public boolean hasExcludeCoverBones() {
        return excludeCoverBones != null;
    }

    public boolean wasBoneExcludeCovered(String boneName) {
        return excludeCoverBones != null && excludeCoverBones.contains(boneName);
    }

    public AnimationInstance coverStateExclude(String excludeBone) {
        this.coverState = true;
        if (excludeCoverBones == null) {
            excludeCoverBones = new HashSet<>();
        }
        excludeCoverBones.add(excludeBone);
        return this;
    }

    public AnimationInstance coverStateExclude(String... excludeBones) {
        this.coverState = true;
        if (excludeCoverBones == null) {
            excludeCoverBones = new HashSet<>();
        }
        excludeCoverBones.addAll(Arrays.asList(excludeBones));
        return this;
    }

    public AnimationInstance setLoopTimes(int loopTimes) {
        if (!animation.looping() && loopTimes > 1) {
            throw new IllegalArgumentException("Cannot loop times when animation is not looping");
        }
        this.loopTimes = loopTimes;
        return this;
    }

    public int getTick() {
        return tick;
    }

    public AnimationInstance soundOnServer(boolean soundOnServer) {
        this.soundOnServer = soundOnServer;
        return this;
    }

    public long length() {
        return (long) (animation.lengthInSeconds() * 1000);
    }

    public float lengthInSec() {
        return animation.lengthInSeconds();
    }

    public boolean loop() {
        return animation.looping() && loopTimes != 0;
    }

    public boolean shouldLoop() {
        return loop() && looped < loopTimes;
    }

    public void reset() {
        prevSoundIndex = -1;
        tick = 0;
    }

    public void onLooped() {
        reset();
        looped ++;
    }

    public void onClientTick() {
        if (tick <= Utils.secondToTick(animation.lengthInSeconds()) + 1) {
            if (enableSound) {
                List<AnimationDef.SoundPoint> soundPoints = animation.getSoundPoints();
                if (soundPoints == null || soundPoints.isEmpty()) {
                    return;
                }
                int soundIndex = Math.max(0, Mth.binarySearch(
                        0, soundPoints.size(), (index) -> tick < soundPoints.get(index).tick) - 1);
                if (soundIndex != prevSoundIndex) {
                    soundPoints.get(soundIndex).playSound(soundOnServer);
                    prevSoundIndex = soundIndex;
                }
            }
        }
        tick = Math.min(tick + 1, Integer.MAX_VALUE - 1);
    }

    public AnimationInstance setOnPlaying(Consumer<Float> onPlaying) {
        this.onPlaying = onPlaying;
        return this;
    }
}
