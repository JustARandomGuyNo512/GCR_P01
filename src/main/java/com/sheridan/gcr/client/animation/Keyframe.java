//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.sheridan.gcr.client.animation;


import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public record Keyframe(float timestamp, Vector3f target, AnimationChannel.Interpolation interpolation) {
    public float timestamp() {
        return this.timestamp;
    }

    public Vector3f target() {
        return this.target;
    }

    public AnimationChannel.Interpolation interpolation() {
        return this.interpolation;
    }
}
