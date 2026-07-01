package com.sheridan.gcr.client.render.fx.particles.explosion;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.gcr.client.render.fx.particles.explosion.FragmentOption;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class FragmentParticle extends TextureSheetParticle {

    // ================== 静态全局向量池 ==================
    private static final int POOL_SIZE = 1024; // 必须是2的幂，方便位运算取模
    private static final int POOL_MASK = POOL_SIZE - 1;
    private static final float[] DIR_X = new float[POOL_SIZE];
    private static final float[] DIR_Y = new float[POOL_SIZE];
    private static final float[] DIR_Z = new float[POOL_SIZE];

    // 类加载时预计算斐波那契均匀球面分布
    static {
        float goldenRatio = (1.0F + Mth.sqrt(5.0F)) / 2.0F;
        float angleIncrement = (float) Math.PI * 2.0F * goldenRatio;
        for (int i = 0; i < POOL_SIZE; i++) {
            float t = (float) i / POOL_SIZE;
            float inclination = (float) Math.acos(1.0F - 2.0F * t);
            float azimuth = angleIncrement * i;
            DIR_X[i] = Mth.sin(inclination) * Mth.cos(azimuth);
            DIR_Y[i] = Mth.sin(inclination) * Mth.sin(azimuth);
            DIR_Z[i] = Mth.cos(inclination);
        }
    }

    private final int count;
    private final float radius;
    private final float speed;

    // 替代实例数组的变量：只存基础随机种子、起始索引和步长
    private final int startIndex;
    private final int stride;

    protected FragmentParticle(ClientLevel level, double x, double y, double z, FragmentOption options, SpriteSet sprites) {
        super(level, x, y, z);
        this.count = options.count;
        this.radius = options.radius;
        this.speed = options.speed;

        this.lifetime = 3;
        this.gravity = 0.0F;
        this.hasPhysics = false;

        this.rCol = options.r;
        this.gCol = options.g;
        this.bCol = options.b;

        // 生成这个粒子的唯一特征
        Random rand = new Random((long) (x * y * z * 1000));
        this.startIndex = rand.nextInt(POOL_SIZE);
        // 保证步长是奇数，与 1024 互质，这样遍历池子时绝不会短循环重复，能均匀且乱序地抓取向量
        this.stride = rand.nextInt(200) * 2 + 1;

        this.setSpriteFromAge(sprites);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        float ageProgress = ((float)this.age + partialTicks) / (float)this.lifetime;
        this.alpha = 1.0F - ageProgress;
        if (this.alpha <= 0.0F) return;

        net.minecraft.world.phys.Vec3 cameraPos = camera.getPosition();
        float renderX = (float)(Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float renderY = (float)(Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float renderZ = (float)(Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        Quaternionf quaternion = new Quaternionf();
        this.getFacingCameraMode().setRotation(quaternion, camera, partialTicks);
        if (this.roll != 0.0F) {
            quaternion.rotateZ(Mth.lerp(partialTicks, this.oRoll, this.roll));
        }

        int packedLight = LightTexture.FULL_BRIGHT;
        Vector3f vertexPos = new Vector3f();

        float f = 1.0F - ageProgress;
        float easeOut = 1.0F - f * f * f;

        for (int i = 0; i < this.count; i++) {
            int dirIdx = (this.startIndex + i * this.stride) & POOL_MASK;
            float dirX = DIR_X[dirIdx];
            float dirY = DIR_Y[dirIdx];
            float dirZ = DIR_Z[dirIdx];

            float maxRadiusForThisParticle = this.radius * this.speed;
            float currentExpand = maxRadiusForThisParticle * easeOut;

            float pX = renderX + dirX * currentExpand;
            float pY = renderY + dirY * currentExpand;
            float pZ = renderZ + dirZ * currentExpand;

            float pSize = 0.25F * (1.0F - ageProgress * 0.5F);

            // 4. 写入顶点
            vertexPos.set(1.0F, -1.0F, 0.0F).rotate(quaternion).mul(pSize).add(pX, pY, pZ);
            buffer.addVertex(vertexPos.x(), vertexPos.y(), vertexPos.z()).setUv(u1, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(packedLight);

            vertexPos.set(1.0F, 1.0F, 0.0F).rotate(quaternion).mul(pSize).add(pX, pY, pZ);
            buffer.addVertex(vertexPos.x(), vertexPos.y(), vertexPos.z()).setUv(u1, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(packedLight);

            vertexPos.set(-1.0F, 1.0F, 0.0F).rotate(quaternion).mul(pSize).add(pX, pY, pZ);
            buffer.addVertex(vertexPos.x(), vertexPos.y(), vertexPos.z()).setUv(u0, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(packedLight);

            vertexPos.set(-1.0F, -1.0F, 0.0F).rotate(quaternion).mul(pSize).add(pX, pY, pZ);
            buffer.addVertex(vertexPos.x(), vertexPos.y(), vertexPos.z()).setUv(u0, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(packedLight);
        }
    }

    public static class Provider implements ParticleProvider<FragmentOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull FragmentOption type, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new FragmentParticle(level, x, y, z, type, this.sprites);
        }
    }
}