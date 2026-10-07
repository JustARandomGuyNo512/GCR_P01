package com.sheridan.gcr.client.render.fx.muzzleSmoke.slow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.gcr.client.render.RenderTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 一次射击产生的一个 slow smoke。
 * <p>
 * 持有 {@link SlowSmokeEntry}（fadeSeconds / localScale / controller）、
 * {@link SlowSmokeTextureSet}（可用贴图集合）、射击那一刻的矩阵位置副本以及一个随机种子。
 * <p>
 * 烟雾自身的变化（位置偏移 / 大小 / 透明度 / 是否取消）由 entry 上的 {@link ISlowSmokeController}
 * 按 progress + seed 算出来，复写进 task 持有的 {@link TransformRes}（每帧复用同一个对象，不重复创建）；
 * 镜头与全局坐标的换算保持不变：射击那一刻的世界位置 + controller 给的局部位置偏移 → 再换算回当前相机局部坐标。
 * <p>
 * 同一个 task 每帧只会渲染集合里的一张贴图，而且只渲染一次；
 * 具体是哪一张由随机种子决定（见 {@link #textureIndex()}），整个生命周期固定不变，避免逐帧闪烁。
 */
@OnlyIn(Dist.CLIENT)
public class SlowSmokeTask {
    private static final float TAU = (float) (Math.PI * 2.0);
    private static final Quaternionf SCRATCH_ROTATION = new Quaternionf();
    private static final Vector3f SCRATCH_OFFSET = new Vector3f();
    private static final Vector3f SCRATCH_SORT = new Vector3f();
    /** 视角变换用的临时量：当前相机朝向 / 朝向差 */
    private static final Quaternionf SCRATCH_CAM_ROT = new Quaternionf();
    private static final Quaternionf SCRATCH_VIEW_DELTA = new Quaternionf();
    private static final Vector3f SCRATCH_CAM_POS = new Vector3f();

    /** 射击那一刻的矩阵位置副本 */
    private final PoseStack.Pose pose;
    private final long shootTime;
    private final SlowSmokeEntry entry;
    private final SlowSmokeTextureSet textureSet;
    private final int seed;
    private final int textureIndex;
    private final float rotationAngle;
    private final int light;
    /** 射击那一刻的相机朝向（相机 → 世界）与它的逆 */
    private final Quaternionf spawnCameraRotation;
    private final Quaternionf spawnCameraRotationInverse;
    /** 射击那一刻的相机位置 */
    private final Vec3 spawnCameraPos;
    /** 射击那一刻烟雾在世界里的位置（枪口局部位置转到世界方向后加上相机位置） */
    private final Vector3f spawnWorldPosition;
    /** controller 的输出：每帧复写，不重新创建 */
    private final TransformRes transformRes = new TransformRes();
    /** controller 主动取消（{@link TransformRes#isCancel()}）后这一条就算结束 */
    private boolean cancelled;
    /** 这一帧实际写入的渲染类型，供渲染器立即 endBatch，保证绘制顺序 = 排序顺序 */
    @Nullable
    private RenderType lastRenderType;

    public SlowSmokeTask(PoseStack.Pose pose, long shootTime, SlowSmokeEntry entry, SlowSmokeTextureSet textureSet) {
        this(pose, shootTime, entry, textureSet, LightTexture.FULL_BRIGHT);
    }

    public SlowSmokeTask(PoseStack.Pose pose, long shootTime, SlowSmokeEntry entry, SlowSmokeTextureSet textureSet, int light) {
        this(pose, shootTime, entry, textureSet, light, (int) (Math.random() * Integer.MAX_VALUE));
    }

    public SlowSmokeTask(PoseStack.Pose pose, long shootTime, SlowSmokeEntry entry, SlowSmokeTextureSet textureSet, int light, int seed) {
        this.pose = pose.copy();
        this.shootTime = shootTime;
        this.entry = entry;
        this.textureSet = textureSet;
        this.light = light;
        this.seed = seed;

        // 随机 index：每帧都用它取贴图，保证只画一张且不闪烁
        this.textureIndex = textureSet == null ? 0 : textureSet.indexBySeed(seed);
        this.rotationAngle = (random01(seed, 49979687) - 0.5f) * TAU;

        // 记下射击那一刻的视角姿态：之后靠它把烟雾的位置固定在世界坐标里
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        this.spawnCameraRotation = new Quaternionf(camera.rotation());
        this.spawnCameraRotationInverse = new Quaternionf(camera.rotation()).conjugate();
        this.spawnCameraPos = camera.getPosition();

        // 枪口局部位置 → 世界坐标
        Vector3f localPosition = new Vector3f();
        this.pose.pose().getTranslation(localPosition);
        localPosition.rotate(spawnCameraRotation);
        this.spawnWorldPosition = localPosition.add(
                (float) spawnCameraPos.x,
                (float) spawnCameraPos.y,
                (float) spawnCameraPos.z
        );
    }

    public PoseStack.Pose getPose() {
        return pose;
    }

    public long getShootTime() {
        return shootTime;
    }

    public SlowSmokeEntry getEntry() {
        return entry;
    }

    public SlowSmokeTextureSet getTextureSet() {
        return textureSet;
    }

    public int getSeed() {
        return seed;
    }

    /** 本 task 随机选中的贴图 index */
    public int textureIndex() {
        return textureIndex;
    }

    public int getLight() {
        return light;
    }

    /** controller 每帧复写的输出（位置偏移 / alpha / scale / cancel） */
    public TransformRes transformRes() {
        return transformRes;
    }

    /** 生命进度 0 → 1，1 代表已经淡出到看不见 */
    public float progress() {
        return progress(System.currentTimeMillis());
    }

    public float progress(long now) {
        return Mth.clamp((now - shootTime) / (float) entry.fadeDurationMillis(), 0f, 1f);
    }

    public float ageSeconds() {
        return (System.currentTimeMillis() - shootTime) * 0.001f;
    }

    public boolean isFinished() {
        return isFinished(System.currentTimeMillis());
    }

    public boolean isFinished(long now) {
        return cancelled || now - shootTime >= entry.fadeDurationMillis();
    }

    /**
     * 渲染这一帧的这一张贴图，返回 true 表示已经结束
     * <p>
     * 每帧只会取集合里的一张贴图（随机 index），并且只渲染一次；
     * 每帧只跑一次 controller，结果复写进 {@link #transformRes}。
     */
    public boolean handleRender(MultiBufferSource bufferSource) {
        lastRenderType = null;
        if (isFinished()) {
            return true;
        }
        SmokeTexture texture = textureSet == null ? null : textureSet.pick(textureIndex);
        if (texture == null) {
            return true;
        }

        float progress = progress();
        updateTransform(progress);
        if (transformRes.isCancel()) {
            cancelled = true;
            return true;
        }

        float alpha = transformRes.alpha() * (texture.baseAlpha());
        float size = texture.baseSize() * entry.localScale() * transformRes.scale();
        if (alpha <= 0f || size <= 0f) {
            return isFinished();
        }
        RenderType renderType = RenderTypes.entityTranslucent(texture.location());
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        render(vertexConsumer, texture, alpha, size);
        lastRenderType = renderType;
        return false;
    }

    /** 这一帧实际写入的渲染类型，可能为 null（这一帧没画） */
    @Nullable
    public RenderType lastRenderType() {
        return lastRenderType;
    }

    /** 跑一遍 entry 的 controller，把这一帧烟雾自身的变换复写到复用的 transformRes 上 */
    private void updateTransform(float progress) {
        entry.controller().update(progress, seed, transformRes);
    }

    /** 渲染当前帧的单个 quad（每帧每 task 只调用一次） */
    public void render(VertexConsumer vertexConsumer, SmokeTexture texture, float alpha, float size) {
        PoseStack.Pose renderPose = pose.copy();
        // 镜头 / 世界坐标变换：位置取 controller 这一帧给的位置偏移
        applyViewTransform(renderPose);
        if (entry.randomRotate()) {
            SCRATCH_ROTATION.identity().rotateZ(rotationAngle);
            renderPose.pose().rotate(SCRATCH_ROTATION);
        }
        renderPose.pose().scale(size, size, 1f);
        draw(renderPose.pose(), vertexConsumer, alpha, texture);
    }

    /**
     * 当前这一帧烟雾的世界坐标位 = 射击那一刻的世界位置 + controller 给的位置偏移（局部空间 → 世界方向）
     */
    private Vector3f currentWorldPosition(Vector3f dest) {
        if (entry.followFlyDir()) {
            dest.set(transformRes.positionOffset());
        } else {
            dest.set(0f, 0f, 0f);
        }
        dest.rotate(spawnCameraRotation);
        return dest.add(spawnWorldPosition);
    }

    /** 世界坐标位 → 相对当前相机的局部坐标（方块单位） */
    private void currentViewPosition(Camera camera, Vector3f dest) {
        // 获取烟雾的世界坐标
        currentWorldPosition(dest);

        // 世界坐标 -> 相对相机的坐标
        Vec3 cameraPos = camera.getPosition();
        dest.x -= (float) cameraPos.x;
        dest.y -= (float) cameraPos.y;
        dest.z -= (float) cameraPos.z;

        // 世界坐标 -> 当前相机局部坐标
        SCRATCH_CAM_ROT.set(camera.rotation()).conjugate();
        dest.rotate(SCRATCH_CAM_ROT);
    }

    /**
     * 镜头 / 全局坐标变换：把"世界坐标位"换算成当前相机局部坐标，写回姿态平移。
     */
    private void applyViewTransform(PoseStack.Pose renderPose) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Matrix4f matrix = renderPose.pose();

        // 1) 朝向：用当前相机旋转替换掉姿态里射击那一刻的相机旋转（只影响旋转，位置下面会被覆盖）
        //SCRATCH_VIEW_DELTA.set(camera.rotation()).mul(spawnCameraRotationInverse);
        //matrix.rotateLocal(SCRATCH_VIEW_DELTA);

        // 2) 位置：世界坐标位 → 当前相机对齐的局部坐标
        currentViewPosition(camera, SCRATCH_OFFSET);
        matrix.setTranslation(SCRATCH_OFFSET.x, SCRATCH_OFFSET.y, SCRATCH_OFFSET.z);
    }

    /**
     * 供 {@code MuzzleFlashRenderer} 做 z 轴半透明排序用的深度值。
     * <p>
     * 统一约定：{@code key = -pose 平移的 z}，数值越大 = 离相机越远（这个姿态空间里 -Z 是枪口前方）。
     * 渲染器按 key 降序排序，也就是由远到近画，标准的半透明后向前绘制顺序。
     * <p>
     * 位置取的是和 {@link #render} 完全一致的"世界坐标位换算回当前相机局部"的位置。
     */
    public float sortDepth() {
        // 排序和绘制用同一套 controller 结果（同一个对象，后一次调用直接覆盖）
        updateTransform(progress());
        currentViewPosition(Minecraft.getInstance().gameRenderer.getMainCamera(), SCRATCH_SORT);
        return -SCRATCH_SORT.z;
    }

    /**
     * slow smoke 的每张图都不是序列图，直接整张当做一个 quad 渲染
     */
    protected void draw(Matrix4f matrix, VertexConsumer vertexConsumer, float alpha, SmokeTexture texture) {
        vertexConsumer.addVertex(matrix, -0.5f, 0.5f, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(0f, 0f).setNormal(1,1,1).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY);
        vertexConsumer.addVertex(matrix, 0.5f, 0.5f, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(1f, 0f).setNormal(1,1,1).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY);
        vertexConsumer.addVertex(matrix, 0.5f, -0.5f, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(1f, 1f).setNormal(1,1,1).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY);
        vertexConsumer.addVertex(matrix, -0.5f, -0.5f, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(0f, 1f).setNormal(1,1,1).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY);
    }

    /** 由种子推导 0..1 的稳定随机值 */
    private static float random01(int seed, int salt) {
        int h = seed * 0x9E3779B9 + salt * 0x85EBCA6B;
        h ^= h >>> 15;
        h *= 0x2545F491;
        h ^= h >>> 13;
        return (h >>> 8) / (float) 0x1000000;
    }
}
