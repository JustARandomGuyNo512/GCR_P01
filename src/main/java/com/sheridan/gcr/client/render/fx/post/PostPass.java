package com.sheridan.gcr.client.render.fx.post;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

@OnlyIn(Dist.CLIENT)
public class PostPass implements AutoCloseable {
    private final EffectInstance effect;
    public final RenderTarget inTarget;
    public final RenderTarget outTarget;
    public final List<IntSupplier> auxAssets = Lists.newArrayList();
    public final List<String> auxNames = Lists.newArrayList();
    public final List<Integer> auxWidths = Lists.newArrayList();
    public final List<Integer> auxHeights = Lists.newArrayList();
    private Matrix4f shaderOrthoMatrix;
    private final int filterMode;

    public PostPass(ResourceProvider resourceProvider, String name, RenderTarget inTarget, RenderTarget outTarget, boolean useLinearFilter) throws IOException {
        this.effect = new EffectInstance(resourceProvider, name);
        this.inTarget = inTarget;
        this.outTarget = outTarget;
        this.filterMode = useLinearFilter ? 9729 : 9728;
    }

    public void close() {
        this.effect.close();
    }

    public final String getName() {
        return this.effect.getName();
    }

    public void addAuxAsset(String auxName, IntSupplier auxFramebuffer, int width, int height) {
        this.auxNames.add(this.auxNames.size(), auxName);
        this.auxAssets.add(this.auxAssets.size(), auxFramebuffer);
        this.auxWidths.add(this.auxWidths.size(), width);
        this.auxHeights.add(this.auxHeights.size(), height);
    }

    public void setOrthoMatrix(Matrix4f shaderOrthoMatrix) {
        this.shaderOrthoMatrix = shaderOrthoMatrix;
    }

    public void process(float partialTicks, Consumer<PostPass> pConsumer) {
        this.inTarget.unbindWrite();
        float f = (float)this.outTarget.width;
        float f1 = (float)this.outTarget.height;
        RenderSystem.viewport(0, 0, (int)f, (int)f1);
        Objects.requireNonNull(this.inTarget);
        this.effect.setSampler("DiffuseSampler", this.inTarget::getColorTextureId);
        pConsumer.accept(this);
        for(int i = 0; i < this.auxAssets.size(); ++i) {
            this.effect.setSampler(this.auxNames.get(i), this.auxAssets.get(i));
            this.effect.safeGetUniform("AuxSize" + i).set((float) this.auxWidths.get(i), (float) this.auxHeights.get(i));
        }

        this.effect.safeGetUniform("ProjMat").set(this.shaderOrthoMatrix);
        this.effect.safeGetUniform("InSize").set((float)this.inTarget.width, (float)this.inTarget.height);
        this.effect.safeGetUniform("OutSize").set(f, f1);
        this.effect.safeGetUniform("Time").set(partialTicks);
        Minecraft minecraft = Minecraft.getInstance();
        this.effect.safeGetUniform("ScreenSize").set((float)minecraft.getWindow().getWidth(), (float)minecraft.getWindow().getHeight());
        this.effect.apply();
        this.outTarget.bindWrite(false);
        RenderSystem.disableDepthTest();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferbuilder.addVertex(0.0F, 0.0F, 0);
        bufferbuilder.addVertex(f, 0.0F, 0);
        bufferbuilder.addVertex(f, f1, 0);
        bufferbuilder.addVertex(0.0F, f1, 0);
        BufferUploader.draw(bufferbuilder.buildOrThrow());
        RenderSystem.disableDepthTest();
        this.effect.clear();
        this.outTarget.unbindWrite();
        this.inTarget.unbindRead();

        for(Object object : this.auxAssets) {
            if (object instanceof RenderTarget) {
                ((RenderTarget)object).unbindRead();
            }
        }

    }

    public EffectInstance getEffect() {
        return this.effect;
    }

    public int getFilterMode() {
        return this.filterMode;
    }
}