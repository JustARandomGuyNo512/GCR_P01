package com.sheridan.gcr.client.render.fx.muzzleSmoke.fast;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.gcr.client.render.RenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;

@OnlyIn(Dist.CLIENT)
public class FastMuzzleSmoke {
    public final int length;
    public final float size;
    public final float spread;
    public final Vector2f alphas;
    public final ResourceLocation texture;
    public final int columnNum;
    private boolean randomRotate = false;

    public FastMuzzleSmoke(int length, float size, float spread, Vector2f alphaLerp, ResourceLocation texture, int columnNum)  {
        this.length = length;
        this.size = size;
        this.spread = Math.max(1, spread) * this.size;
        this.alphas = alphaLerp;
        this.texture = texture;
        this.columnNum = Mth.clamp(columnNum, 1, 4);
    }

    public FastMuzzleSmoke randomRotate() {
        this.randomRotate = true;
        return this;
    }

    public boolean isRandomRotate() {
        return randomRotate;
    }

    public void render(long lastShoot, PoseStack.Pose pose, MultiBufferSource bufferSource, int randomSeed, int light) {
        long timeDist = System.currentTimeMillis() - lastShoot;
        if (timeDist < length) {

            VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderTypes.getMuzzleFlash(texture));
                    //bufferSource.getBuffer(RenderTypes.getMuzzleFlashNotWriteDepth(texture));
            float progress = (float) timeDist / length;
            float size = Mth.lerp(progress, this.size, this.spread) * (0.833333333333333333f + randomSeed % 50 / 300f);
            float alpha = Mth.lerp(progress, alphas.x, alphas.y);
            int column = randomSeed % columnNum;
            int index = (int) (progress * 4);
            pose.pose().translate(0, 0, - progress * 0.05f);
            if (randomRotate) {
                float angle = (randomSeed % 360) * 0.017453292519943295f;
                pose.pose().rotate(new Quaternionf().rotateZ(angle));
            }
            pose.pose().scale(size, size, 1);
            draw(pose.pose(), vertexConsumer, alpha, column, index, light);
        }
    }

    protected void draw(Matrix4f matrix, VertexConsumer vertexConsumer, float alpha, int column, int index, int light) {
        float u1 = index * 0.25f;
        float u2 = u1 + 0.25f;
        float v1 = column * 0.25f;
        float v2 = v1 + 0.25f;
        vertexConsumer.addVertex(matrix, -0.5f, 0.5f, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(u1, v1).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY);
        vertexConsumer.addVertex(matrix, 0.5f, 0.5f, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(u2, v1).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY);
        vertexConsumer.addVertex(matrix, 0.5f, -0.5f, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(u2, v2).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY);
        vertexConsumer.addVertex(matrix, -0.5f, -0.5f, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(u1, v2).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY);
    }
}
