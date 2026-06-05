package com.sheridan.gcr.client.animation;

import com.sheridan.gcr.client.animation.command.Command;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class AnimationSequence implements IAnimationSequence {

    private final List<AnimationInstance> animations = new ArrayList<>();

    private long startTime;
    private long length;

    private boolean finished;
    private AnimationInstance current;

    private Consumer<IAnimationSequence> onRemoved;

    private float speed = 1.0f;

    public AnimationSequence() {}

    public AnimationSequence(AnimationInstance... instance) {
        this.animations.addAll(List.of(instance));
        this.finishBuild();
    }

    public float getSpeed() {
        return speed;
    }

    public AnimationSequence setSpeed(float speed) {
        this.speed = Math.max(0f, speed);
        return this;
    }

    @Override
    public IAnimationSequence append(AnimationInstance instance) {
        if (finished) return this;
        animations.add(instance);
        return this;
    }

    // =========================
    // 构建（含 keepOnLastFrame 校验）
    // =========================

    public AnimationSequence finishBuild() {
        startTime = System.currentTimeMillis();
        length = 0;

        for (int i = 0; i < animations.size(); i++) {
            AnimationInstance instance = animations.get(i);

            // ⭐ 校验：只能最后一个能 keepOnLastFrame
            if (instance.keepOnLastFrame && i != animations.size() - 1) {
                throw new IllegalStateException("keepOnLastFrame instance must be the last in AnimationSequence");
            }

            instance.timeStamp = startTime + length;

            long realLength = (long) (
                    instance.length()
                            * (instance.loop() ? instance.loopTimes : 1)
                            / Math.max(1e-6f, instance.getSpeed())
            );

            length += realLength;
        }

        finished = true;
        return this;
    }

    public AnimationSequence prepare() {
        startTime = System.currentTimeMillis();
        for (AnimationInstance instance : animations) {
            instance.reset();
            instance.looped = 0;
        }
        return finishBuild();
    }

    // =========================
    // 播放
    // =========================

    @Override
    public void apply(IAnimated root, ModuleRenderContext context) {
        AnimationInstance inst = animations.get(getIndex());
        if (inst != null) {

            float finalSpeed = this.speed * inst.getSpeed();

            KeyframeAnimator._animate(
                    root,
                    inst.animation,
                    inst.timeStamp,
                    0L,
                    inst.scales.x,
                    inst.scales.y,
                    inst.scales.z,
                    inst.loop(),
                    inst.keepOnLastFrame, // ⭐ 关键
                    finalSpeed
            );

            current = inst;

            if (inst.onPlaying != null) {
                inst.onPlaying.accept(getCurrentAnimatingProgress());
            }

            List<Command> commands = inst.animation.getCommands();
            if (!commands.isEmpty()) {
                for (Command command : commands) {
                    command.onFrame(root, this, context);
                }
            }
        }
    }

    @Override
    public boolean applying() {
        AnimationInstance inst = animations.get(getIndex());
        if (inst != null) {

            if (inst.keepOnLastFrame) {
                return true; // ⭐ 永远在播放
            }

            float res = KeyframeAnimator.dist(
                    inst.timeStamp,
                    0L,
                    inst.loop(),
                    false,
                    inst.animation,
                    this.speed * inst.getSpeed()
            );
            return !Float.isNaN(res);
        }
        return false;
    }

    @Override
    public boolean tick() {
        int index = getIndex();
        AnimationInstance inst = animations.get(index);

        long now = System.currentTimeMillis();

        long dist = (long) ((now - inst.timeStamp) * this.speed);

        long instanceLength = (long) (
                inst.length() / Math.max(1e-6f, inst.getSpeed())
        );

        if (inst.shouldLoop()) {
            if (dist > instanceLength * (inst.looped + 1)) {
                inst.onLooped();
            }
            inst.onClientTick();
        } else {
            if (dist > instanceLength) {

                // ⭐ 如果是最后一个且 keepOnLastFrame → 不结束
                if (inst == animations.getLast() && inst.keepOnLastFrame) {
                    inst.onClientTick();
                    return false; // 永不结束
                }

                if (inst == animations.getLast()) {
                    return true;
                }

                inst = animations.get(index + 1);
            }
            inst.onClientTick();
        }

        // ⭐ 全局结束判断（排除 keepOnLastFrame）
        if (animations.getLast().keepOnLastFrame) {
            return false;
        }

        return (long) ((now - startTime) * speed) > length;
    }

    // =========================
    // 时间
    // =========================

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public float getLength() {
        return length / 1000f;
    }

    @Override
    public float getProgress() {
        long now = System.currentTimeMillis();
        float elapsed = (now - startTime) * speed;
        return elapsed / (float) length;
    }

    @Override
    public float getCurrentAnimatingProgress() {
        if (current == null) return 0;

        long now = System.currentTimeMillis();

        long dist = (long) (
                (now - current.timeStamp) * speed
        ) - (long) (
                current.looped * (current.length() / Math.max(1e-6f, current.getSpeed()))
        );

        return dist / (float) (
                current.length() / Math.max(1e-6f, current.getSpeed())
        );
    }

    // =========================
    // 索引
    // =========================

    private int getIndex() {
        long virtualNow = startTime + (long) ((System.currentTimeMillis() - startTime) * speed);

        int index = Mth.binarySearch(
                0,
                animations.size(),
                (i) -> animations.get(i).timeStamp >= virtualNow
        ) - 1;

        return Math.max(0, Math.min(index, animations.size() - 1));
    }

    // =========================
    // 访问
    // =========================

    @Override
    public AnimationInstance getCurrentAnimating() {
        return current;
    }

    @Override
    public AnimationInstance get(int index) {
        return animations.get(index);
    }

    @Override
    public int size() {
        return animations.size();
    }

    // =========================
    // 生命周期
    // =========================

    @Override
    public void onRemoved(Consumer<IAnimationSequence> onRemoved) {
        this.onRemoved = onRemoved;
    }

    @Override
    public void removed() {
        if (onRemoved != null) {
            onRemoved.accept(this);
        }
    }
}

//package com.sheridan.gcr.client.animation;
//
//import com.sheridan.gcr.client.animation.command.Command;
//import com.sheridan.gcr.client.render.ModuleRenderContext;
//import net.minecraft.util.Mth;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.api.distmarker.OnlyIn;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.function.Consumer;
//
//@OnlyIn(Dist.CLIENT)
//public class AnimationSequence implements IAnimationSequence {
//
//    private final List<AnimationInstance> animations = new ArrayList<>();
//
//    private long startTime;
//    private long length; // 注意：这是“虚拟时间长度”（已经考虑 instance.speed）
//
//    private boolean finished;
//    private AnimationInstance current;
//
//    private Consumer<IAnimationSequence> onRemoved;
//
//    /** ⭐ 全局速度（默认 1） */
//    private float speed = 1.0f;
//
//    public AnimationSequence() {}
//
//    public AnimationSequence(AnimationInstance... instance) {
//        this.animations.addAll(List.of(instance));
//        this.finishBuild();
//    }
//
//
//    public float getSpeed() {
//        return speed;
//    }
//
//    public AnimationSequence setSpeed(float speed) {
//        this.speed = Math.max(0f, speed); // 不支持倒放
//        return this;
//    }
//
//
//    @Override
//    public IAnimationSequence append(AnimationInstance instance) {
//        if (finished) {
//            return this;
//        }
//        animations.add(instance);
//        return this;
//    }
//
//    public AnimationSequence finishBuild() {
//        startTime = System.currentTimeMillis();
//        length = 0;
//
//        for (AnimationInstance instance : animations) {
//            instance.timeStamp = startTime + length;
//
//            long realLength = (long) (
//                    instance.length()
//                            * (instance.loop() ? instance.loopTimes : 1)
//                            / Math.max(1e-6f, instance.getSpeed())
//            );
//
//            length += realLength;
//        }
//
//        finished = true;
//        return this;
//    }
//
//    public AnimationSequence prepare() {
//        startTime = System.currentTimeMillis();
//        for (AnimationInstance instance : animations) {
//            instance.reset();
//            instance.looped = 0;
//        }
//        return finishBuild();
//    }
//
//
//    @Override
//    public void apply(IAnimated root, ModuleRenderContext context) {
//        AnimationInstance first = animations.get(getIndex());
//        if (first != null) {
//
//            float finalSpeed = this.speed * first.getSpeed();
//
//            KeyframeAnimator._animate(
//                    root,
//                    first.animation,
//                    first.timeStamp,
//                    0L,
//                    first.scales.x,
//                    first.scales.y,
//                    first.scales.z,
//                    first.loop(),
//                    false,
//                    finalSpeed
//            );
//
//            current = first;
//
//            if (first.onPlaying != null) {
//                first.onPlaying.accept(getCurrentAnimatingProgress());
//            }
//
//            List<Command> commands = first.animation.getCommands();
//            if (!commands.isEmpty()) {
//                for (Command command : commands) {
//                    command.onFrame(root, this, context);
//                }
//            }
//        }
//    }
//
//    @Override
//    public boolean applying() {
//        AnimationInstance first = animations.get(getIndex());
//        if (first != null) {
//            float res = KeyframeAnimator.dist(
//                    first.timeStamp,
//                    0L,
//                    first.loop(),
//                    false,
//                    first.animation,
//                    this.speed * first.getSpeed()
//            );
//            return !Float.isNaN(res);
//        }
//        return false;
//    }
//
//    @Override
//    public boolean tick() {
//        int index = getIndex();
//        AnimationInstance current = animations.get(index);
//
//        long now = System.currentTimeMillis();
//
//        long dist = (long) ((now - current.timeStamp) * this.speed);
//
//        long instanceLength = (long) (
//                current.length() / Math.max(1e-6f, current.getSpeed())
//        );
//
//        if (current.shouldLoop()) {
//            if (dist > instanceLength * (current.looped + 1)) {
//                current.onLooped();
//            }
//            current.onClientTick();
//        } else {
//            if (dist > instanceLength) {
//                if (current == animations.getLast()) {
//                    return true;
//                }
//                current = animations.get(index + 1);
//            }
//            current.onClientTick();
//        }
//
//        return (long) ((now - startTime) * speed) > length;
//    }
//
//
//    @Override
//    public long getStartTime() {
//        return startTime;
//    }
//
//    @Override
//    public float getLength() {
//        return length / 1000f;
//    }
//
//    @Override
//    public float getProgress() {
//        long now = System.currentTimeMillis();
//        float elapsed = (now - startTime) * speed;
//        return elapsed / (float) length;
//    }
//
//    @Override
//    public float getCurrentAnimatingProgress() {
//        if (current == null) {
//            return 0;
//        }
//
//        long now = System.currentTimeMillis();
//
//        long dist = (long) (
//                (now - current.timeStamp) * speed
//        ) - (long) (
//                current.looped * (current.length() / Math.max(1e-6f, current.getSpeed()))
//        );
//
//        return dist / (float) (
//                current.length() / Math.max(1e-6f, current.getSpeed())
//        );
//    }
//
//
//    private int getIndex() {
//        long virtualNow = startTime + (long) ((System.currentTimeMillis() - startTime) * speed);
//
//        int index = Mth.binarySearch(
//                0,
//                animations.size(),
//                (i) -> animations.get(i).timeStamp >= virtualNow
//        ) - 1;
//
//        return Math.max(0, Math.min(index, animations.size() - 1));
//    }
//
//
//    @Override
//    public AnimationInstance getCurrentAnimating() {
//        return current;
//    }
//
//    @Override
//    public AnimationInstance get(int index) {
//        return animations.get(index);
//    }
//
//    @Override
//    public int size() {
//        return animations.size();
//    }
//
//
//    @Override
//    public void onRemoved(Consumer<IAnimationSequence> onRemoved) {
//        this.onRemoved = onRemoved;
//    }
//
//    @Override
//    public void removed() {
//        if (onRemoved != null) {
//            onRemoved.accept(this);
//        }
//    }
//}