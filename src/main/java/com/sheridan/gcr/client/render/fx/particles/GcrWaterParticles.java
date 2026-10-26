package com.sheridan.gcr.client.render.fx.particles;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.level.material.FogType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The three TaCZ Additions water effect types using Minecraft's own particle
 * atlas sprites, without copying textures or depending on TaCZ Additions.
 */
@OnlyIn(Dist.CLIENT)
public final class GcrWaterParticles {
    private GcrWaterParticles() {}

    /**
     * TaCZ Additions' optional above-water bubble pass: the water layer
     * renders after normal particles and otherwise hides underwater trails.
     * We disable depth ONLY for bubbles in this pass; water puffs and drops
     * still use depth-tested translucent particle rendering.
     */
    private static final ParticleRenderType THROUGH_WATER = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder buffer, TextureManager textures) {
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tessellator) {
            tessellator.end();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "gcr:water_bubble_through_water";
        }
    };

    private static final class Bubble extends TextureSheetParticle {
        Bubble(ClientLevel level, double x, double y, double z,
               double vx, double vy, double vz, SpriteSet sprites) {
            super(level, x, y, z);
            xd = vx * 0.2 + (random.nextDouble() - 0.5) * 0.04;
            yd = vy * 0.2 + (random.nextDouble() - 0.5) * 0.04;
            zd = vz * 0.2 + (random.nextDouble() - 0.5) * 0.04;
            setSize(0.02f, 0.02f);
            quadSize *= 0.50f + random.nextFloat() * 0.60f;
            lifetime = 12 + random.nextInt(32);
            hasPhysics = false;
            pickSprite(sprites);
        }

        @Override
        public void tick() {
            xo = x; yo = y; zo = z;
            if (age++ >= lifetime) { remove(); return; }
            yd += 0.002;
            move(xd, yd, zd);
            xd *= 0.85; yd *= 0.85; zd *= 0.85;
            if (!level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER)) remove();
        }

        @Override
        public ParticleRenderType getRenderType() {
            return Minecraft.getInstance().gameRenderer.getMainCamera().getFluidInCamera() == FogType.WATER
                    ? ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
                    : THROUGH_WATER;
        }
    }

    private static final class Puff extends TextureSheetParticle {
        private final SpriteSet sprites;

        Puff(ClientLevel level, double x, double y, double z,
             double vx, double vy, double vz, SpriteSet sprites) {
            super(level, x, y, z);
            this.sprites = sprites;
            xd = vx; yd = vy; zd = vz;
            setSize(0.06f, 0.02f);
            quadSize = 0.05f + random.nextFloat() * 0.06f;
            lifetime = 7 + random.nextInt(6);
            hasPhysics = false;
            alpha = 0.68f;
            setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            xd *= 0.74; yd *= 0.74; zd *= 0.74;
            alpha = Math.max(0f, 0.68f * (1f - age / (float) lifetime));
            setSpriteFromAge(sprites);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private static final class Drop extends TextureSheetParticle {
        private final SpriteSet sprites;

        Drop(ClientLevel level, double x, double y, double z,
             double vx, double vy, double vz, SpriteSet sprites) {
            super(level, x, y, z);
            this.sprites = sprites;
            xd = vx; yd = vy; zd = vz;
            gravity = 0.13f;
            friction = 0.91f;
            hasPhysics = true;
            lifetime = 10 + random.nextInt(11);
            quadSize *= 0.32f;
            setSize(0.012f, 0.012f);
            setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            if (onGround) remove();
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public static final class BubbleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public BubbleProvider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new Bubble(world, x, y, z, vx, vy, vz, sprites);
        }
    }

    public static final class PuffProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public PuffProvider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new Puff(world, x, y, z, vx, vy, vz, sprites);
        }
    }

    public static final class DropProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public DropProvider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new Drop(world, x, y, z, vx, vy, vz, sprites);
        }
    }
}
