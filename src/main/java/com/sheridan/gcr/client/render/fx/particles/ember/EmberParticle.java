package com.sheridan.gcr.client.render.fx.particles.ember;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class EmberParticle extends TextureSheetParticle {

    private final int gridSize;
    private final EmberOption.EasingType easing;
    private final TextureAtlasSprite chosenSprite;

    private int currentFrame = 0;

    protected EmberParticle(ClientLevel level, double x, double y, double z, EmberOption options, SpriteSet sprites) {
        super(level, x, y, z);
        this.gridSize = Math.max(1, options.gridSize);
        this.easing = options.easing;
        this.lifetime = options.lifetime;
        this.quadSize = options.scale;

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


    private void updateFrameProgress() {
        float t = (float) this.age / (float) this.lifetime;
        t = Mth.clamp(t, 0.0F, 1.0F);


        float easedProgress = switch (this.easing) {
            case SQR -> 1.0F - (1.0F - t) * (1.0F - t);
            case CUBIC -> 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
            default -> t;
        };

        int totalFrames = this.gridSize * this.gridSize;
        // 计算当前落在哪一帧
        this.currentFrame = Mth.clamp((int) (easedProgress * totalFrames), 0, totalFrames - 1);
    }

    @Override
    protected float getU0() {
        int col = this.currentFrame % this.gridSize;
        return Mth.lerp((float) col / this.gridSize, this.chosenSprite.getU0(), this.chosenSprite.getU1());
    }

    @Override
    protected float getU1() {
        int col = this.currentFrame % this.gridSize;
        return Mth.lerp((float) (col + 1) / this.gridSize, this.chosenSprite.getU0(), this.chosenSprite.getU1());
    }

    @Override
    protected float getV0() {
        int row = this.currentFrame / this.gridSize;
        return Mth.lerp((float) row / this.gridSize, this.chosenSprite.getV0(), this.chosenSprite.getV1());
    }

    @Override
    protected float getV1() {
        int row = this.currentFrame / this.gridSize;
        return Mth.lerp((float) (row + 1) / this.gridSize, this.chosenSprite.getV0(), this.chosenSprite.getV1());
    }

    public static class Provider implements ParticleProvider<EmberOption> {
        protected final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull EmberOption type, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new EmberParticle(level, x, y, z, type, this.sprites);
        }
    }
}