//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.sheridan.gcr.client.animation;

import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

public record AnimationChannel(Target target, Keyframe... keyframes) {

    public Target target() {
        return this.target;
    }

    public Keyframe[] keyframes() {
        return this.keyframes;
    }

    @OnlyIn(Dist.CLIENT)
    public interface Target {
        void apply(IAnimated var1, Vector3f var2);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Targets {
        public static final Target POSITION = IAnimated::offsetPos;
        public static final Target ROTATION = IAnimated::offsetRotation;
        public static final Target SCALE = IAnimated::offsetScale;

        public Targets() {}
    }

    @OnlyIn(Dist.CLIENT)
    public static class Interpolations {
        public static final Interpolation LINEAR = (res, progress, twoFrames, currentIndex, nextIndex, scaleX, scaleY, scaleZ) -> {
            Vector3f vector3f = twoFrames[currentIndex].target();
            Vector3f vector3f1 = twoFrames[nextIndex].target();
            return vector3f.lerp(vector3f1, progress, res).mul(scaleX, scaleY, scaleZ);
        };
        public static final Interpolation CATMULL_ROM = (res, progress, twoFrames, currentIndex, nextIndex, scaleX, scaleY, scaleZ) -> {
            Vector3f vector3f = twoFrames[Math.max(0, currentIndex - 1)].target();
            Vector3f vector3f1 = twoFrames[currentIndex].target();
            Vector3f vector3f2 = twoFrames[nextIndex].target();
            Vector3f vector3f3 = twoFrames[Math.min(twoFrames.length - 1, nextIndex + 1)].target();
            res.set(Mth.catmullrom(progress, vector3f.x(), vector3f1.x(), vector3f2.x(), vector3f3.x()) * scaleX,
                    Mth.catmullrom(progress, vector3f.y(), vector3f1.y(), vector3f2.y(), vector3f3.y()) * scaleY,
                    Mth.catmullrom(progress, vector3f.z(), vector3f1.z(), vector3f2.z(), vector3f3.z()) * scaleZ);
            return res;
        };

        public Interpolations() {}
    }

    @OnlyIn(Dist.CLIENT)
    public interface Interpolation {
        Vector3f apply(Vector3f res, float progress, Keyframe[] twoFrames, int currentIndex, int nextIndex, float scaleX, float scaleY, float scaleZ);
    }

}
