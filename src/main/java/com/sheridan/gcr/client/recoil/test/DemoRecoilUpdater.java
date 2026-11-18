package com.sheridan.gcr.client.recoil.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.client.model.modular.IGunModel;
import com.sheridan.gcr.client.recoil.IRecoilUpdater;
import com.sheridan.gcr.client.recoil.RecoilData;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;

/**
 * Demo 级 RecoilUpdater：由 {@link NoiseRecoilData} 的随机参数驱动两层噪声弹簧。
 *
 * <p>核心随机层委托给 {@link NoiseRecoilHandler}，这个类只做 {@link IRecoilUpdater}
 * 的接口适配和绝对时间记录。开发环境里可以临时把 {@code RecoilHandler} 里的
 * {@code new RecoilUpdater()} 换成 {@code new DemoRecoilUpdater()} 做手感对比。
 * {@link #setRecoilData(RecoilData)} 只保存引用，不读取任何 impulse / controller / visualMix
 * 参数；实际运动完全由构造时传入的 {@link NoiseRecoilData} 决定。</p>
 *
 * <p>这一版只做随机旋转层：没有确定性抬枪、后移、roll，也没有在 {@link #applyTransformPost}
 * 里改 pose。随机偏移通过 {@link #getPitch()} / {@link #getYaw()} 和
 * {@link #getCameraSpeed()} 暴露，真正的相机 / 枪模合成等接入时再决定。</p>
 *
 * <p>注意：{@code getCameraSpeed()} 和 {@code getGunKickPitch/Yaw} 目前只是兼容旧链路的输出；
 * 相机和服务端弹道的最终口径（是否重复叠加）还没定，接正式版本时要统一。符号约定
 * （尤其是 Minecraft 的 pitch 正方向）也还没在真实相机上校准，接入时先确认是否需要取反。</p>
 *
 * <p>不是线程安全的；和旧 updater 一样，{@link #update} / {@link #onShoot} 应在
 * {@code ClientWeaponLooper} 的武器线程上串行调用。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class DemoRecoilUpdater implements IRecoilUpdater {
    private final NoiseRecoilData data;
    private final NoiseRecoilHandler handler;
    private final Vector2f cameraSpeed = new Vector2f();

    /** 旧 RecoilData：只保留引用给 getRecoilData()，运动完全不用它。 */
    private RecoilData legacyData;

    private double time;
    private double lastShootTime;
    private boolean hasShot;

    public DemoRecoilUpdater() {
        this(NoiseRecoilData.defaults());
    }

    public DemoRecoilUpdater(NoiseRecoilData data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }

        this.data = data;
        this.handler = NoiseRecoilHandler.fromData(data);
    }

    @Override
    public void update(double timeDist) {
        float dt = (float) timeDist;
        if (!(dt > 0f) || !Float.isFinite(dt)) {
            return;
        }

        time += dt;
        handler.update(dt);

        // 兼容旧 RecoilCameraHandler：它把 getCameraSpeed() 当“每次 update 的增量”累加，
        // 所以这里返回 velocity * dt，而不是 velocity 本身。
        cameraSpeed.set(handler.getPitchVelocity() * dt, handler.getYawVelocity() * dt);
    }

    @Override
    public void onShoot(Player player) {
        onShoot();
    }

    /** Demo / 测试用：不依赖 Player 的开火入口。 */
    public void onShoot() {
        handler.onShoot();
        lastShootTime = time;
        hasShot = true;
    }

    @Override
    public void applyImpulse(float impulseZ, float pitch, float yaw,
                             float globalPitch, float globalYaw, float shakeRoll) {
        // Demo：新模型是目标驱动，不再使用旧冲量。
    }

    @Override
    public void applyTransformPost(PoseStack poseStack, boolean aiming,
                                   float particleTicks, IGunModel model) {
        // Demo：不在渲染层改 pose。随机旋转通过 getPitch()/getYaw() 和 getCameraSpeed() 暴露；
        // 真正的相机 / 枪模合成等接入时再决定。
    }

    @Override
    public float getGunKickPitch() {
        return getPitch();
    }

    @Override
    public float getGunKickYaw() {
        return getYaw();
    }

    /**
     * Demo：完全忽略旧 RecoilData 的 impulse / controller / visualMix。
     * 保留引用只是为了 {@link #getRecoilData()} 能返回原对象；运动只由
     * {@link NoiseRecoilData} 决定。
     */
    @Override
    public void setRecoilData(RecoilData data) {
        this.legacyData = data;
    }

    @Override
    public RecoilData getRecoilData() {
        return legacyData;
    }

    /** Demo 用 activity 代替旧的 heat，范围同样是 0~1。 */
    @Override
    public float getRecoilHeat() {
        return handler.getActivity();
    }

    @Override
    public float getCamShakeZ() {
        return 0f;
    }

    @Override
    public float distFromLastShoot() {
        return hasShot ? (float) (time - lastShootTime) : 0f;
    }

    /**
     * 兼容旧相机层：RecoilCameraHandler 把该值当作“每次 update 的增量”累加，
     * 所以这里返回 velocity * dt，而不是 velocity 本身。
     * 新代码如果要绝对偏移，直接用 {@link #getPitch()} / {@link #getYaw()}。
     */
    @Override
    public Vector2f getCameraSpeed() {
        return cameraSpeed;
    }

    /** 两层叠加后的 pitch（度）。 */
    public float getPitch() {
        return handler.getPitch();
    }

    /** 两层叠加后的 yaw（度）。 */
    public float getYaw() {
        return handler.getYaw();
    }

    public float getPitchVelocity() {
        return handler.getPitchVelocity();
    }

    public float getYawVelocity() {
        return handler.getYawVelocity();
    }

    public float getGlobalPitch() {
        return handler.getGlobalPitch();
    }

    public float getGlobalYaw() {
        return handler.getGlobalYaw();
    }

    public float getLocalPitch() {
        return handler.getLocalPitch();
    }

    public float getLocalYaw() {
        return handler.getLocalYaw();
    }

    public float getActivity() {
        return handler.getActivity();
    }

    public NoiseRecoilData getData() {
        return data;
    }
}
