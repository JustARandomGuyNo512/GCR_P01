package com.sheridan.gcr.client.particles.impl;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class BulletHoleParticle extends TextureSheetParticle {
    private final Direction direction;

    protected BulletHoleParticle(ClientLevel level, double x, double y, double z, double directionIndex, double unused1, double unused2) {
        super(level, x, y, z);
        // 从服务器传过来的参数中获取命中的朝向
        this.direction = Direction.from3DDataValue((int) directionIndex);

        // 关键：持续 5 秒消失 (5秒 * 20 ticks = 100)
        this.lifetime = 100;

        // 子弹孔不需要重力和速度
        this.gravity = 0.0F;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;

        this.quadSize = 0.1F; // 弹孔大小，可自行调整

        // 让弹孔稍微往外凸出一点点（如 0.01 米），防止与方块表面发生标准深度冲突（Z-Fighting）
        this.x += direction.getStepX() * 0.01;
        this.y += direction.getStepY() * 0.01;
        this.z += direction.getStepZ() * 0.01;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        // 使用 TERRAIN_SHEET 或者是自定义的，由于你指定了固定图片，可以用 PARTICLE_SHEET_OPAQUE 或 Custom
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, Camera camera, float partialTicks) {
        // 重写渲染，使粒子不总是面向玩家（Billboard），而是平贴在方向面上
        System.out.println("render");

        Vec3 camPos = camera.getPosition();
        float fx = (float) (this.x - camPos.x());
        float fy = (float) (this.y - camPos.y());
        float fz = (float) (this.z - camPos.z());

        // 根据命中的面旋转渲染正方形
        Quaternionf quaternion = new Quaternionf();
        switch (this.direction) {
            case Direction.DOWN -> quaternion.rotationX((float) Math.toRadians(90));
            case Direction.UP -> quaternion.rotationX((float) Math.toRadians(-90));
            case Direction.NORTH -> quaternion.rotationY((float) Math.toRadians(180));
            case Direction.SOUTH -> quaternion.rotationY((float) Math.toRadians(0));
            case Direction.WEST -> quaternion.rotationY((float) Math.toRadians(90));
            case Direction.EAST -> quaternion.rotationY((float) Math.toRadians(-90));
        }

        Vector3f[] vertices = new Vector3f[]{
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)
        };

        for (int i = 0; i < 4; ++i) {
            Vector3f vertex = vertices[i];
            vertex.rotate(quaternion);
            vertex.mul(this.quadSize);
            vertex.add(fx, fy, fz);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);

        buffer.addVertex(vertices[0].x(), vertices[0].y(), vertices[0].z()).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u1, v1).setLight(light);
        buffer.addVertex(vertices[1].x(), vertices[1].y(), vertices[1].z()).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u1, v0).setLight(light);
        buffer.addVertex(vertices[2].x(), vertices[2].y(), vertices[2].z()).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u0, v0).setLight(light);
        buffer.addVertex(vertices[3].x(), vertices[3].y(), vertices[3].z()).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u0, v1).setLight(light);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            BulletHoleParticle particle = new BulletHoleParticle(level, x, y, z, dx, dy, dz);
            particle.pickSprite(this.spriteSet);
            return particle;
        }
    }
}
