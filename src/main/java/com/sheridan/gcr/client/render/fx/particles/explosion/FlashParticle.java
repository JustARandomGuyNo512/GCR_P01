package com.sheridan.gcr.client.render.fx.particles.explosion;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class FlashParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected FlashParticle(ClientLevel level, double x, double y, double z,
                            double dx, double dy, double dz,
                            FlashOption options, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        this.quadSize = options.scale;
        this.lifetime = 1;

        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
    }

//    public @NotNull AABB getRenderBoundingBox(float partialTicks) {
//        float size = this.getQuadSize(partialTicks) * 64;
//        return new AABB(this.x - (double)size, this.y - (double)size, this.z - (double)size, this.x + (double)size, this.y + (double)size, this.z + (double)size);
//    }


    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera renderInfo, float partialTicks) {
        super.render(buffer, renderInfo, partialTicks);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public SpriteSet getSprites() {
        return sprites;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<FlashOption> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(@NotNull FlashOption options, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new FlashParticle(level, x, y, z, dx, dy, dz, options, sprites);
        }
    }
}
