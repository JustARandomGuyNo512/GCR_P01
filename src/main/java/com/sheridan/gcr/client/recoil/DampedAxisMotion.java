package com.sheridan.gcr.client.recoil;

/**
 * 通用单轴阻尼运动发生器（双通道融合系统）
 * 融合了【用于连射基线沉降的隐式阻尼半隐式欧拉积分弹簧】与【用于单发打击感的时间轴速度冲击函数】
 */
public class DampedAxisMotion {

    // 基线通道参数（物理分离：刚度与阻尼）
    private final float baseStiffness; // 弹簧刚度 (k)
    private final float baseDamping;   // 阻尼系数 (c)
    private double basePosition = 0.0;
    private double baseVelocity = 0.0;

    // 打击感通道参数
    private final float joltOmega0;
    private float joltTime = 0.0f;
    private float joltVelocityImpulse = 0.0f;
    private boolean isJolting = false;

    // 配置权重
    private final float baseWeight;
    private final float joltWeight;
    private final float joltZeta;

    public DampedAxisMotion(float baseStiffness, float baseDamping, float joltOmega0, float baseWeight, float joltWeight, float joltZeta) {
        this.baseStiffness = baseStiffness;
        this.baseDamping = baseDamping;
        this.joltOmega0 = joltOmega0;
        this.baseWeight = baseWeight;
        this.joltWeight = joltWeight;
        this.joltZeta = joltZeta;
    }

    /**
     * 每帧调用，步进整个轴向的物理状态
     * @param dt 帧间隔时间（秒）
     */
    public void update(float dt) {
        if (dt <= 0f) {
            return;
        }

        // 1. 步进基线状态机（隐式阻尼半隐式欧拉积分）
        if (Math.abs(basePosition) > 1e-4 || Math.abs(baseVelocity) > 1e-4) {
            // 隐式阻尼更新速度：v_{n+1} = (v_n - k * x_n * dt) / (1 + c * dt)
            // 彻底解决了高阻尼下数值爆炸/无法运动的问题
            baseVelocity = (baseVelocity - baseStiffness * basePosition * dt) / (1.0 + baseDamping * dt);

            // 使用新速度更新位置：x_{n+1} = x_n + v_{n+1} * dt
            basePosition += baseVelocity * dt;
        } else {
            basePosition = 0.0;
            baseVelocity = 0.0;
        }

        // 2. 步进打击感通道时间轴（自然衰减隐退）
        if (isJolting) {
            joltTime += dt;
            // 取绝对值判断衰减，防止负数位移导致关断失效
            if (Math.abs(calculateJolt()) < 1e-4 && joltTime > 0.1f) {
                isJolting = false;
                joltTime = 0.0f;
                joltVelocityImpulse = 0.0f;
            }
        }
    }

    /**
     * 当该轴受到冲量输入时调用（例如开火、受击）
     * @param impulse 冲量强度
     */
    public void applyImpulse(float impulse) {
        // 基线通道：累加速度冲量
        this.baseVelocity += impulse * baseWeight;

        // 打击感通道：重置时间轴
        this.joltTime = 0.0f;
        this.joltVelocityImpulse = impulse * joltWeight;
        this.isJolting = true;
    }

    /**
     * 强制重置当前轴物理状态
     */
    public void reset() {
        this.basePosition = 0.0;
        this.baseVelocity = 0.0;
        this.joltTime = 0.0f;
        this.joltVelocityImpulse = 0.0f;
        this.isJolting = false;
    }

    /**
     * 计算打击感通道的单发速度冲击位移
     */
    private double calculateJolt() {
        if (!isJolting || joltTime <= 0f) return 0.0;
        return joltVelocityImpulse * Math.exp(joltZeta * -joltOmega0 * joltTime) * Math.cos(joltOmega0 * joltTime - Math.PI / 2);
    }

    /**
     * 获取当前轴总物理位移输出
     */
    public float getValue() {
        return (float) (basePosition + calculateJolt());
    }

    /**
     * 仅获取纯单发打击感分量
     */
    public float getJoltValue() {
        return (float) calculateJolt();
    }
}