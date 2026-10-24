package com.sheridan.gcr.client.model.modular.state;

import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.animation.KeyframeAnimator;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class StaticState {
    public static final StaticState EMPTY = new StaticState("empty", null, null, null);
    protected Map<String, Vector3f> offsets;
    protected Map<String, Vector3f> rotations;
    protected Map<String, Vector3f> scales;
    public final String name;

    public static StaticState fromAnimation(String stateName, AnimationDef def, float scale, float progress) {
        float length = def.lengthInSeconds();
        progress = Mth.clamp(progress, 0, 1);
        float delta = progress * length;
        long startTime = System.currentTimeMillis() - (long)(delta * 1000);
        TransformSavingBone root = TransformSavingBone.root();
        KeyframeAnimator._animate(root, def, startTime, 0, scale, scale, scale, false, false, 1);
        Map<String, Vector3f> offsets = new Object2ObjectArrayMap<>();
        Map<String, Vector3f> rotations = new Object2ObjectArrayMap<>();
        Map<String, Vector3f> scales = new Object2ObjectArrayMap<>();
        root.travel(bone -> {
            String name = bone.name;
            if (bone.hasMove()) {
                offsets.put(name, bone.pos);
            }
            if (bone.hasRotate()) {
                rotations.put(name, bone.rot);
            }
            if (bone.hasScale()) {
                scales.put(name, bone.scale);
            }
        });
        return new StaticState(
                stateName,
                offsets.isEmpty() ? null : offsets,
                rotations.isEmpty() ? null : rotations,
                scales.isEmpty() ? null : scales);
    }

    protected StaticState(String name,
                          Map<String, Vector3f> offsets,
                          Map<String, Vector3f> rotations,
                          Map<String, Vector3f> scales) {
        this.offsets = offsets;
        this.rotations = rotations;
        this.scales = scales;
        this.name = name;
    }

    public void apply(IAnimated animated, ModuleRenderContext context) {
        if (offsets != null) {
            for (Map.Entry<String, Vector3f> entry : this.offsets.entrySet()) {
                String boneName = entry.getKey();
                if (context.isBoneStateLocked(boneName)) {
                    continue;
                }
                animated.findByName(boneName).ifPresent(bone -> bone.offsetPos(entry.getValue()));
            }
        }
        if (rotations != null) {
            for (Map.Entry<String, Vector3f> entry : this.rotations.entrySet()) {
                String boneName = entry.getKey();
                if (context.isBoneStateLocked(boneName)) {
                    continue;
                }
                animated.findByName(boneName).ifPresent(bone -> bone.offsetRotation(entry.getValue()));
            }
        }
        if (scales != null) {
            for (Map.Entry<String, Vector3f> entry : this.scales.entrySet()) {
                String boneName = entry.getKey();
                if (context.isBoneStateLocked(boneName)) {
                    continue;
                }
                animated.findByName(boneName).ifPresent(bone -> bone.offsetScale(entry.getValue()));
            }
        }
    }

    public static class Builder {
        private final String name;
        private final Map<String, Vector3f> offsets = new HashMap<>();
        private final Map<String, Vector3f> rotations = new HashMap<>();
        private final Map<String, Vector3f> scales = new HashMap<>();

        private static final float DEG_TO_RAD = (float) Math.PI / 180f;
        private static final float PIXEL_TO_BLOCK = 0.0625f;

        public static Builder of(String name) {
            return new Builder(name);
        }

        public static StaticState empty(String name) {
            return new StaticState(name, null, null, null);
        }

        public Builder(String name) {
            this.name = name;
        }

        public Builder setTranslation(String boneName, float x, float y, float z) {
            this.offsets.put(boneName, new Vector3f(x * PIXEL_TO_BLOCK, y * PIXEL_TO_BLOCK, z * PIXEL_TO_BLOCK));
            return this;
        }

        public Builder setRotation(String boneName, float xDeg, float yDeg, float zDeg) {
            this.rotations.put(boneName, new Vector3f(xDeg * DEG_TO_RAD, yDeg * DEG_TO_RAD, zDeg * DEG_TO_RAD));
            return this;
        }

        public Builder setScale(String boneName, float x, float y, float z) {
            this.scales.put(boneName, new Vector3f(x - 1, y - 1, z - 1));
            return this;
        }

        public Builder setScale(String boneName, float scale) {
            this.scales.put(boneName, new Vector3f(scale - 1, scale - 1, scale - 1));
            return this;
        }

        public StaticState build() {
            return new StaticState(
                    this.name,
                    this.offsets.isEmpty() ? null : new Object2ObjectArrayMap<>(this.offsets),
                    this.rotations.isEmpty() ? null : new Object2ObjectArrayMap<>(this.rotations),
                    this.scales.isEmpty() ? null : new Object2ObjectArrayMap<>(this.scales)
            );
        }
    }

    public static class TransformSavingBone implements IAnimated {
        public Vector3f pos;
        public Vector3f rot;
        public Vector3f scale;
        public String name;
        public Map<String, TransformSavingBone> children;

        public static TransformSavingBone root() {
            return new TransformSavingBone("root");
        }

        protected TransformSavingBone(String name) {
            pos = new Vector3f();
            rot = new Vector3f();
            scale = new Vector3f();
            children = new HashMap<>();
            this.name = name;
        }

        @Override
        public void offsetPos(Vector3f vector3f) {
            pos.add(vector3f);
        }

        @Override
        public void offsetRotation(Vector3f vector3f) {
            rot.add(vector3f);
        }

        @Override
        public void offsetScale(Vector3f vector3f) {
            scale.add(vector3f);
        }

        public boolean hasMove() {
            return Math.abs(pos.x) > 1e-5 ||
                Math.abs(pos.y) > 1e-5 ||
                Math.abs(pos.z) > 1e-5;
        }

        public boolean hasRotate() {
            return Math.abs(rot.x) > 1e-5 ||
                Math.abs(rot.y) > 1e-5 ||
                Math.abs(rot.z) > 1e-5;
        }

        public boolean hasScale() {
            return Math.abs(scale.x) > 1e-5 ||
                Math.abs(scale.y) > 1e-5 ||
                Math.abs(scale.z) > 1e-5;
        }

        private TransformSavingBone getOrCreate(String name) {
            TransformSavingBone stateSavingBone = children.get(name);
            if (stateSavingBone == null) {
                stateSavingBone = new TransformSavingBone(name);
                children.put(name, stateSavingBone);
            }
            return stateSavingBone;
        }

        public void travel(Consumer<TransformSavingBone> consumer) {
            consumer.accept(this);
            for (TransformSavingBone child : children.values()) {
                child.travel(consumer);
            }
        }

        @Override
        public Optional<IAnimated> findByName(String pName) {
            if (name.equals(pName)) {
                return Optional.of(this);
            } else {
                return Optional.of(getOrCreate(pName));
            }
        }
    }

}
