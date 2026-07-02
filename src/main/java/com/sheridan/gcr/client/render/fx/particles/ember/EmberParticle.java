package com.sheridan.gcr.client.render.fx.particles.ember;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class EmberParticle extends TextureSheetParticle {

    private final int gridSize;
    private final EmberOption.EasingType easing;
    private final TextureAtlasSprite chosenSprite;
    private final List<EmberOption.Entry> clusterEntries;
    private final boolean shouldSort;

    private int currentFrame = 0;

    protected EmberParticle(ClientLevel level, double x, double y, double z, EmberOption options, SpriteSet sprites) {
        super(level, x, y, z);
        this.gridSize = Math.max(1, options.gridSize);
        this.easing = options.easing;
        this.lifetime = options.lifetime;
        this.quadSize = options.scale;
        this.shouldSort = options.shouldSort;
        this.clusterEntries = new ArrayList<>(options.entries);

        this.gravity = 0.0F;
        this.hasPhysics = false;

        this.chosenSprite = sprites.get(level.getRandom());
        this.updateFrameProgress();
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
        } else {
            this.updateFrameProgress();
        }
    }


    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        float ageProgress = ((float) this.age + partialTicks) / (float) this.lifetime;
        this.alpha = 1.0F - ageProgress; // 渐隐
        if (this.alpha <= 0.0F) return;


        net.minecraft.world.phys.Vec3 cameraPos = camera.getPosition();
        float renderX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float renderY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float renderZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());


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

        if (this.shouldSort && !this.clusterEntries.isEmpty()) {
            this.clusterEntries.sort((e1, e2) -> {
                float d1_X = renderX + e1.dx(), d1_Y = renderY + e1.dy(), d1_Z = renderZ + e1.dz();
                float d2_X = renderX + e2.dx(), d2_Y = renderY + e2.dy(), d2_Z = renderZ + e2.dz();
                float distSq1 = d1_X * d1_X + d1_Y * d1_Y + d1_Z * d1_Z;
                float distSq2 = d2_X * d2_X + d2_Y * d2_Y + d2_Z * d2_Z;
                return Float.compare(distSq2, distSq1);
            });
        }

        if (this.clusterEntries.isEmpty()) {
            drawSubQuad(buffer, quaternion, renderX, renderY, renderZ, this.quadSize, u0, u1, v0, v1, packedLight);
        } else {
            for (EmberOption.Entry entry : this.clusterEntries) {
                float pX = renderX + entry.dx();
                float pY = renderY + entry.dy();
                float pZ = renderZ + entry.dz();
                drawSubQuad(buffer, quaternion, pX, pY, pZ, this.quadSize, u0, u1, v0, v1, packedLight);
            }
        }
    }

    private void drawSubQuad(VertexConsumer buffer, Quaternionf quaternion, float x, float y, float z, float size, float u0, float u1, float v0, float v1, int light) {
        Vector3f vertexPos = new Vector3f();

        vertexPos.set(1.0F, -1.0F, 0.0F).rotate(quaternion).mul(size).add(x, y, z);
        buffer.addVertex(vertexPos.x(), vertexPos.y(), vertexPos.z()).setUv(u1, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);

        vertexPos.set(1.0F, 1.0F, 0.0F).rotate(quaternion).mul(size).add(x, y, z);
        buffer.addVertex(vertexPos.x(), vertexPos.y(), vertexPos.z()).setUv(u1, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);

        vertexPos.set(-1.0F, 1.0F, 0.0F).rotate(quaternion).mul(size).add(x, y, z);
        buffer.addVertex(vertexPos.x(), vertexPos.y(), vertexPos.z()).setUv(u0, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);

        vertexPos.set(-1.0F, -1.0F, 0.0F).rotate(quaternion).mul(size).add(x, y, z);
        buffer.addVertex(vertexPos.x(), vertexPos.y(), vertexPos.z()).setUv(u0, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
    }

    private void updateFrameProgress() {
        float t = (float) this.age / (float) this.lifetime;
        t = Mth.clamp(t, 0.0F, 1.0F);

        float easedProgress = switch (this.easing) {
            case SQR -> 1.0F - (1.0F - t) * (1.0F - t);
            case CUBIC -> 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
            default -> t;
        };

        int totalFrames = this.gridSize * this.gridSize;
        this.currentFrame = Mth.clamp((int) (easedProgress * totalFrames), 0, totalFrames - 1);
    }

    @Override protected float getU0() { int col = this.currentFrame % this.gridSize; return Mth.lerp((float) col / this.gridSize, this.chosenSprite.getU0(), this.chosenSprite.getU1()); }
    @Override protected float getU1() { int col = this.currentFrame % this.gridSize; return Mth.lerp((float) (col + 1) / this.gridSize, this.chosenSprite.getU0(), this.chosenSprite.getU1()); }
    @Override protected float getV0() { int row = this.currentFrame / this.gridSize; return Mth.lerp((float) row / this.gridSize, this.chosenSprite.getV0(), this.chosenSprite.getV1()); }
    @Override protected float getV1() { int row = this.currentFrame / this.gridSize; return Mth.lerp((float) (row + 1) / this.gridSize, this.chosenSprite.getV0(), this.chosenSprite.getV1()); }

    public static class Provider implements ParticleProvider<EmberOption> {
        protected final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(@NotNull EmberOption type, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new EmberParticle(level, x, y, z, type, this.sprites);
        }
    }
}