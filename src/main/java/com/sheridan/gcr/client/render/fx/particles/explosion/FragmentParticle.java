package com.sheridan.gcr.client.render.fx.particles.explosion;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class FragmentParticle extends TextureSheetParticle {

    public static final int POOL_SIZE = 1024; // 必须是2的幂
    public static final int POOL_MASK = POOL_SIZE - 1;
    public static final float[] DIR_X = new float[POOL_SIZE];
    public static final float[] DIR_Y = new float[POOL_SIZE];
    public static final float[] DIR_Z = new float[POOL_SIZE];

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
    private final float irregularity;
    private final int color;
    private final int baseSeed;

    protected FragmentParticle(ClientLevel level, double x, double y, double z, FragmentOption options, SpriteSet sprites) {
        super(level, x, y, z);
        this.count = options.count;
        this.radius = options.radius;
        this.speed = options.speed;

        this.irregularity = 0.3F;

        this.lifetime = 4;
        this.gravity = 0.0F;
        this.hasPhysics = false;

        // 仅记录一个基于位置的初始种子
        Random rand = new Random((long) (x * y * z * 1000));
        this.baseSeed = rand.nextInt();
        this.color = options.color;
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
        if (this.alpha <= 0.0F){
            return;
        }

        Vec3 cameraPos = camera.getPosition();
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

            int seed = this.baseSeed + i;
            seed ^= seed << 13;
            seed ^= seed >>> 17;
            seed ^= seed << 5;


            int dirIdx = seed & POOL_MASK;
            float dirX = DIR_X[dirIdx];
            float dirY = DIR_Y[dirIdx];
            float dirZ = DIR_Z[dirIdx];

            float currentExpand = getCurrentExpand(seed, easeOut);

            float pX = renderX + dirX * currentExpand;
            float pY = renderY + dirY * currentExpand;
            float pZ = renderZ + dirZ * currentExpand;

            float pSize = 0.25F * (1.0F - ageProgress);

            vertexPos.set(1.0F, -1.0F, 0.0F).rotate(quaternion).mul(pSize).add(pX, pY, pZ);
            buffer.addVertex(vertexPos.x(), vertexPos.y(), vertexPos.z()).setUv(u1, v1).setColor(this.color).setLight(packedLight);

            vertexPos.set(1.0F, 1.0F, 0.0F).rotate(quaternion).mul(pSize).add(pX, pY, pZ);
            buffer.addVertex(vertexPos.x(), vertexPos.y(), vertexPos.z()).setUv(u1, v0).setColor(this.color).setLight(packedLight);

            vertexPos.set(-1.0F, 1.0F, 0.0F).rotate(quaternion).mul(pSize).add(pX, pY, pZ);
            buffer.addVertex(vertexPos.x(), vertexPos.y(), vertexPos.z()).setUv(u0, v0).setColor(this.color).setLight(packedLight);

            vertexPos.set(-1.0F, -1.0F, 0.0F).rotate(quaternion).mul(pSize).add(pX, pY, pZ);
            buffer.addVertex(vertexPos.x(), vertexPos.y(), vertexPos.z()).setUv(u0, v1).setColor(this.color).setLight(packedLight);
        }
    }

    private float getCurrentExpand(int seed, float easeOut) {
        float noise = ((seed >>> 7) & 0x7FFFFFFF) / (float) Integer.MAX_VALUE;

        float mul;
        if (noise < this.irregularity) {
            mul = 0.2F + noise * 1.8F * this.irregularity;
        } else {
            mul = 0.8F + noise * 0.4F;
        }

        float maxRadiusForThisParticle = this.radius * this.speed * mul;
        return maxRadiusForThisParticle * easeOut;
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