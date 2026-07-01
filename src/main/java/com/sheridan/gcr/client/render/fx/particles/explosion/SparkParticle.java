package com.sheridan.gcr.client.render.fx.particles.explosion;


import com.mojang.blaze3d.vertex.VertexConsumer;
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

import static com.sheridan.gcr.client.render.fx.particles.explosion.FragmentParticle.*;


@OnlyIn(Dist.CLIENT)
public class SparkParticle extends TextureSheetParticle {

    private final int count;
    private final float radius;
    private final float speed;
    private final int baseSeed;
    private final int startColor;
    private final int fadeColor;
    protected SparkParticle(ClientLevel level, double x, double y, double z, SparkOption options, SpriteSet sprites) {
        super(level, x, y, z);
        this.count = options.count;
        this.radius = options.radius;
        this.speed = options.speed;

        // 飘落需要更长的生命周期，这里固定为 25 帧（可根据需要改为 options 传入）
        this.lifetime = 25;
        this.gravity = 0.0F;
        this.hasPhysics = false;

        this.startColor = options.startColor;
        this.fadeColor = options.fadeColor;

        Random rand = new Random((long) (x * y * z * 5000));
        this.baseSeed = rand.nextInt();
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

        float alphaFactor = Mth.clamp((1.0F - ageProgress) * 1.5F, 0.0F, 1.0F);
        if (alphaFactor <= 0.0F) return;

        int sA = (this.startColor >> 24) & 0xFF;
        int sR = (this.startColor >> 16) & 0xFF;
        int sG = (this.startColor >> 8) & 0xFF;
        int sB = this.startColor & 0xFF;


        int fA = (this.fadeColor >> 24) & 0xFF;
        int fR = (this.fadeColor >> 16) & 0xFF;
        int fG = (this.fadeColor >> 8) & 0xFF;
        int fB = this.fadeColor & 0xFF;

        int curA = (int) (Mth.lerp(ageProgress, sA, fA) * alphaFactor);
        int curR = (int) Mth.lerp(ageProgress, sR, fR);
        int curG = (int) Mth.lerp(ageProgress, sG, fG);
        int curB = (int) Mth.lerp(ageProgress, sB, fB);

        int packedColor = (curA << 24) | (curR << 16) | (curG << 8) | curB;

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

        // 火花物理运动曲线
        float radialEase = (1.0F - (float) Math.exp(-ageProgress * 8.0F)) / (1.0F - (float) Math.exp(-8.0F));
        float fallEase = ageProgress * ageProgress;

        for (int i = 0; i < this.count; i++) {
            int seed = this.baseSeed + i;
            seed ^= seed << 13;
            seed ^= seed >>> 17;
            seed ^= seed << 5;

            int dirIdx = seed & POOL_MASK;
            float dirX = DIR_X[dirIdx];
            float dirY = DIR_Y[dirIdx];
            float dirZ = DIR_Z[dirIdx];

            float randSpeed = ((seed >>> 5) & 0x7FFFFFFF) / (float) Integer.MAX_VALUE;
            float randFall = ((seed >>> 11) & 0x7FFFFFFF) / (float) Integer.MAX_VALUE;
            float randSize = ((seed >>> 16) & 0x7FFFFFFF) / (float) Integer.MAX_VALUE;

            float mult = 0.4F + randSpeed * 1.2F;
            float maxRadius = this.radius * this.speed * mult;
            float currentExpand = maxRadius * radialEase;

            float maxFallDistance = 1.5F * (1 + randFall);
            float currentFall = maxFallDistance * fallEase;

            float pX = renderX + dirX * currentExpand;
            float pY = renderY + dirY * currentExpand - currentFall;
            float pZ = renderZ + dirZ * currentExpand;

            float sizeMul = 0.7F + randSize * 0.6F;
            float pSize = 0.25F * (1.0F + ageProgress) * sizeMul;


            vertexPos.set(1.0F, -1.0F, 0.0F).rotate(quaternion).mul(pSize).add(pX, pY, pZ);
            buffer.addVertex(vertexPos.x(), vertexPos.y(), vertexPos.z()).setUv(u1, v1).setColor(packedColor).setLight(packedLight);

            vertexPos.set(1.0F, 1.0F, 0.0F).rotate(quaternion).mul(pSize).add(pX, pY, pZ);
            buffer.addVertex(vertexPos.x(), vertexPos.y(), vertexPos.z()).setUv(u1, v0).setColor(packedColor).setLight(packedLight);

            vertexPos.set(-1.0F, 1.0F, 0.0F).rotate(quaternion).mul(pSize).add(pX, pY, pZ);
            buffer.addVertex(vertexPos.x(), vertexPos.y(), vertexPos.z()).setUv(u0, v0).setColor(packedColor).setLight(packedLight);

            vertexPos.set(-1.0F, -1.0F, 0.0F).rotate(quaternion).mul(pSize).add(pX, pY, pZ);
            buffer.addVertex(vertexPos.x(), vertexPos.y(), vertexPos.z()).setUv(u0, v1).setColor(packedColor).setLight(packedLight);
        }
    }

    public static class Provider implements ParticleProvider<SparkOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SparkOption type, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new SparkParticle(level, x, y, z, type, this.sprites);
        }
    }
}