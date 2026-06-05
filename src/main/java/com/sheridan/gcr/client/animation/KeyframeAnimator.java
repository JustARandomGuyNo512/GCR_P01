package com.sheridan.gcr.client.animation;

import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.sheridan.gcr.client.animation.AnimationChannel.Targets.POSITION;

@OnlyIn(Dist.CLIENT)
public class KeyframeAnimator {
    private static final Vector3f INTERPOLATION_RESULT_CACHE = new Vector3f(0,0,0);

    public static float dist(
            long startTime, long shift,
            boolean looping, boolean keepOnLastFrame,
            AnimationDef definition,
            float speed) {
        float timeDis = getTimeDis(startTime, shift) * speed;

        if (looping) {
            timeDis %= definition.lengthInSeconds();
        } else {
            if (keepOnLastFrame) {
                timeDis = Math.min(timeDis, definition.lengthInSeconds());
            } else {
                if (timeDis > definition.lengthInSeconds()) {
                    return Float.NaN;
                }
            }
        }
        return timeDis;
    }

    public static void animate(IAnimated root, AnimationDef definition, long startTime, boolean looping, boolean keepOnLastFrame, float speed) {
        _animate(root, definition, startTime, 0, 0.0625f, 0.0625f, 0.0625f, looping, keepOnLastFrame, speed);
    }

    public static void animate(IAnimated root, AnimationDef definition, long startTime) {
        _animate(root, definition, startTime, 0, 0.0625f, 0.0625f, 0.0625f, false, false, 1.0f);
    }

    public static void animate(IAnimated root, AnimationDef definition, long startTime, float speed) {
        _animate(root, definition, startTime, 0, 0.0625f, 0.0625f, 0.0625f, false, false, speed);
    }

    public static void _animate(
            IAnimated root,
            AnimationDef definition,
            long startTime, long shift,
            float scaleX, float scaleY, float scaleZ,
            boolean looping, boolean keepOnLastFrame,
            float speed) {
        float timeDist = dist(startTime, shift, looping, keepOnLastFrame, definition, speed);

        if (Float.isNaN(timeDist)) {
            return;
        }

        for (Map.Entry<String, List<AnimationChannel>> entry : definition.boneAnimations().entrySet()) {
            Optional<IAnimated> optional = root.findByName(entry.getKey());
            List<AnimationChannel> list = entry.getValue();
            optional.ifPresent((modelPart) -> list.forEach((channel) -> {
                Keyframe[] keyframes = channel.keyframes();
                applyFrame(keyframes, timeDist, channel, modelPart, scaleX, scaleY, scaleZ);
            }));
        }
    }

    private static void applyFrame(Keyframe[] keyframes, float timer, AnimationChannel channel, IAnimated bone, float scaleX, float scaleY, float scaleZ) {
        if (keyframes.length > 0) {
            int currentIndex = Math.max(0, Mth.binarySearch(0, keyframes.length, (index) -> timer <= keyframes[index].timestamp()) - 1);
            int nextIndex = Math.min(keyframes.length - 1, currentIndex + 1);
            Keyframe prevFrame = keyframes[currentIndex];
            Keyframe nextFrame = keyframes[nextIndex];
            float f1 = timer - prevFrame.timestamp();
            float f2;
            if (nextIndex != currentIndex) {
                f2 = Mth.clamp(f1 / (nextFrame.timestamp() - prevFrame.timestamp()), 0.0F, 1.0F);
            } else {
                f2 = 0.0F;
            }
            if (channel.target() == POSITION) {
                nextFrame.interpolation().apply(INTERPOLATION_RESULT_CACHE, f2, keyframes, currentIndex, nextIndex, -scaleX, -scaleY, scaleZ);
            } else if (channel.target() == AnimationChannel.Targets.ROTATION) {
                nextFrame.interpolation().apply(INTERPOLATION_RESULT_CACHE, f2, keyframes, currentIndex, nextIndex, -1, -1, 1);
            } else {
                nextFrame.interpolation().apply(INTERPOLATION_RESULT_CACHE, f2, keyframes, currentIndex, nextIndex, 1, 1, 1);
            }
            channel.target().apply(bone, INTERPOLATION_RESULT_CACHE);
        }
    }

    private static float getTimeDis(long startTime, long shift) {
        return (System.currentTimeMillis() - startTime - shift) * 0.001F;
    }

    public static Vector3f posVec(float pX, float pY, float pZ) {
        return new Vector3f(pX, -pY, pZ);
    }

    public static Vector3f degreeVec(float pXDegrees, float pYDegrees, float pZDegrees) {
        return new Vector3f(pXDegrees * ((float)Math.PI / 180F), pYDegrees * ((float)Math.PI / 180F), pZDegrees * ((float)Math.PI / 180F));
    }

    public static Vector3f scaleVec(double pXScale, double pYScale, double pZScale) {
        return new Vector3f((float)(pXScale - 1.0D), (float)(pYScale - 1.0D), (float)(pZScale - 1.0D));
    }

}
