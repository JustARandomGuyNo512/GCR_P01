package com.sheridan.gcr.client.render.fx.post;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ChainedJsonException;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.GsonHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class PostChain implements AutoCloseable {
    private static final String MAIN_RENDER_TARGET = "minecraft:main";
    private final RenderTarget screenTarget;
    private final ResourceProvider resourceProvider;
    private final String name;
    public final List<PostPass> passes = Lists.newArrayList();
    private final Map<String, RenderTarget> customRenderTargets = Maps.newHashMap();
    private final List<RenderTarget> fullSizedTargets = Lists.newArrayList();
    private Matrix4f shaderOrthoMatrix;
    private int screenWidth;
    private int screenHeight;
    private float time;
    private float lastStamp;

    public PostChain(TextureManager textureManager, ResourceProvider resourceProvider, RenderTarget screenTarget, ResourceLocation resourceLocation) throws IOException, JsonSyntaxException {
        this.resourceProvider = resourceProvider;
        this.screenTarget = screenTarget;
        this.time = 0.0F;
        this.lastStamp = 0.0F;
        this.screenWidth = screenTarget.viewWidth;
        this.screenHeight = screenTarget.viewHeight;
        this.name = resourceLocation.toString();
        this.updateOrthoMatrix();
        this.load(textureManager, resourceLocation);
    }

    private void load(TextureManager textureManager, ResourceLocation resourceLocation) throws IOException, JsonSyntaxException {
        Resource resource = this.resourceProvider.getResourceOrThrow(resourceLocation);

        try {
            try (Reader reader = resource.openAsReader()) {
                JsonObject jsonobject = GsonHelper.parse(reader);
                if (GsonHelper.isArrayNode(jsonobject, "targets")) {
                    JsonArray jsonarray = jsonobject.getAsJsonArray("targets");
                    int i = 0;

                    for(JsonElement jsonelement : jsonarray) {
                        try {
                            this.parseTargetNode(jsonelement);
                        } catch (Exception exception1) {
                            ChainedJsonException chainedjsonexception1 = ChainedJsonException.forException(exception1);
                            chainedjsonexception1.prependJsonKey("targets[" + i + "]");
                            throw chainedjsonexception1;
                        }

                        ++i;
                    }
                }

                if (GsonHelper.isArrayNode(jsonobject, "passes")) {
                    JsonArray jsonarray1 = jsonobject.getAsJsonArray("passes");
                    int j = 0;

                    for(JsonElement jsonelement1 : jsonarray1) {
                        try {
                            this.parsePassNode(textureManager, jsonelement1);
                        } catch (Exception exception) {
                            ChainedJsonException chainedjsonexception2 = ChainedJsonException.forException(exception);
                            chainedjsonexception2.prependJsonKey("passes[" + j + "]");
                            throw chainedjsonexception2;
                        }

                        ++j;
                    }
                }
            }

        } catch (Exception exception2) {
            ChainedJsonException chainedjsonexception = ChainedJsonException.forException(exception2);
            String var10001 = resourceLocation.getPath();
            chainedjsonexception.setFilenameAndFlush(var10001 + " (" + resource.sourcePackId() + ")");
            throw chainedjsonexception;
        }
    }

    private void parseTargetNode(JsonElement json) throws ChainedJsonException {
        if (GsonHelper.isStringValue(json)) {
            this.addTempTarget(json.getAsString(), this.screenWidth, this.screenHeight);
        } else {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(json, "target");
            String s = GsonHelper.getAsString(jsonobject, "name");
            int i = GsonHelper.getAsInt(jsonobject, "width", this.screenWidth);
            int j = GsonHelper.getAsInt(jsonobject, "height", this.screenHeight);
            if (this.customRenderTargets.containsKey(s)) {
                throw new ChainedJsonException(s + " is already defined");
            }

            this.addTempTarget(s, i, j);
        }

    }

    private void parsePassNode(TextureManager textureManager, JsonElement json) throws IOException {
        JsonObject jsonobject = GsonHelper.convertToJsonObject(json, "pass");
        String s = GsonHelper.getAsString(jsonobject, "name");
        String s1 = GsonHelper.getAsString(jsonobject, "intarget");
        String s2 = GsonHelper.getAsString(jsonobject, "outtarget");
        RenderTarget rendertarget = this.getRenderTarget(s1);
        RenderTarget rendertarget1 = this.getRenderTarget(s2);
        boolean flag = GsonHelper.getAsBoolean(jsonobject, "use_linear_filter", false);
        if (rendertarget == null) {
            throw new ChainedJsonException("Input target '" + s1 + "' does not exist");
        } else if (rendertarget1 == null) {
            throw new ChainedJsonException("Output target '" + s2 + "' does not exist");
        } else {
            PostPass postpass = this.addPass(s, rendertarget, rendertarget1, flag);
            JsonArray jsonarray = GsonHelper.getAsJsonArray(jsonobject, "auxtargets", (JsonArray)null);
            if (jsonarray != null) {
                int i = 0;

                for(JsonElement jsonelement : jsonarray) {
                    try {
                        JsonObject jsonobject1 = GsonHelper.convertToJsonObject(jsonelement, "auxtarget");
                        String s5 = GsonHelper.getAsString(jsonobject1, "name");
                        String s3 = GsonHelper.getAsString(jsonobject1, "id");
                        boolean flag1;
                        String s4;
                        if (s3.endsWith(":depth")) {
                            flag1 = true;
                            s4 = s3.substring(0, s3.lastIndexOf(58));
                        } else {
                            flag1 = false;
                            s4 = s3;
                        }

                        RenderTarget rendertarget2 = this.getRenderTarget(s4);
                        if (rendertarget2 == null) {
                            if (flag1) {
                                throw new ChainedJsonException("Render target '" + s4 + "' can't be used as depth buffer");
                            }

                            ResourceLocation rl = ResourceLocation.tryParse(s4);
                            ResourceLocation resourcelocation = ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), "textures/effect/" + rl.getPath() + ".png");
                            this.resourceProvider.getResource(resourcelocation).orElseThrow(() -> new ChainedJsonException("Render target or texture '" + s4 + "' does not exist"));
                            RenderSystem.setShaderTexture(0, resourcelocation);
                            textureManager.bindForSetup(resourcelocation);
                            AbstractTexture abstracttexture = textureManager.getTexture(resourcelocation);
                            int j = GsonHelper.getAsInt(jsonobject1, "width");
                            int k = GsonHelper.getAsInt(jsonobject1, "height");
                            boolean flag2 = GsonHelper.getAsBoolean(jsonobject1, "bilinear");
                            if (flag2) {
                                RenderSystem.texParameter(3553, 10241, 9729);
                                RenderSystem.texParameter(3553, 10240, 9729);
                            } else {
                                RenderSystem.texParameter(3553, 10241, 9728);
                                RenderSystem.texParameter(3553, 10240, 9728);
                            }

                            Objects.requireNonNull(abstracttexture);
                            postpass.addAuxAsset(s5, abstracttexture::getId, j, k);
                        } else if (flag1) {
                            Objects.requireNonNull(rendertarget2);
                            postpass.addAuxAsset(s5, rendertarget2::getDepthTextureId, rendertarget2.width, rendertarget2.height);
                        } else {
                            Objects.requireNonNull(rendertarget2);
                            postpass.addAuxAsset(s5, rendertarget2::getColorTextureId, rendertarget2.width, rendertarget2.height);
                        }
                    } catch (Exception exception1) {
                        ChainedJsonException chainedjsonexception = ChainedJsonException.forException(exception1);
                        chainedjsonexception.prependJsonKey("auxtargets[" + i + "]");
                        throw chainedjsonexception;
                    }

                    ++i;
                }
            }

            JsonArray jsonarray1 = GsonHelper.getAsJsonArray(jsonobject, "uniforms", (JsonArray)null);
            if (jsonarray1 != null) {
                int l = 0;

                for(JsonElement jsonelement1 : jsonarray1) {
                    try {
                        this.parseUniformNode(jsonelement1);
                    } catch (Exception exception) {
                        ChainedJsonException chainedjsonexception1 = ChainedJsonException.forException(exception);
                        chainedjsonexception1.prependJsonKey("uniforms[" + l + "]");
                        throw chainedjsonexception1;
                    }

                    ++l;
                }
            }

        }
    }

    private void parseUniformNode(JsonElement json) throws ChainedJsonException {
        JsonObject jsonobject = GsonHelper.convertToJsonObject(json, "uniform");
        String s = GsonHelper.getAsString(jsonobject, "name");
        Uniform uniform = ((PostPass)this.passes.get(this.passes.size() - 1)).getEffect().getUniform(s);
        if (uniform == null) {
            throw new ChainedJsonException("Uniform '" + s + "' does not exist");
        } else {
            float[] afloat = new float[4];
            int i = 0;

            for(JsonElement jsonelement : GsonHelper.getAsJsonArray(jsonobject, "values")) {
                try {
                    afloat[i] = GsonHelper.convertToFloat(jsonelement, "value");
                } catch (Exception exception) {
                    ChainedJsonException chainedjsonexception = ChainedJsonException.forException(exception);
                    chainedjsonexception.prependJsonKey("values[" + i + "]");
                    throw chainedjsonexception;
                }

                ++i;
            }

            switch (i) {
                case 0:
                default:
                    break;
                case 1:
                    uniform.set(afloat[0]);
                    break;
                case 2:
                    uniform.set(afloat[0], afloat[1]);
                    break;
                case 3:
                    uniform.set(afloat[0], afloat[1], afloat[2]);
                    break;
                case 4:
                    uniform.set(afloat[0], afloat[1], afloat[2], afloat[3]);
            }

        }
    }

    public RenderTarget getTempTarget(String attributeName) {
        return (RenderTarget)this.customRenderTargets.get(attributeName);
    }

    public void addTempTarget(String name, int width, int height) {
        RenderTarget rendertarget = new TextureTarget(width, height, true, Minecraft.ON_OSX);
        rendertarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        if (this.screenTarget.isStencilEnabled()) {
            rendertarget.enableStencil();
        }

        this.customRenderTargets.put(name, rendertarget);
        if (width == this.screenWidth && height == this.screenHeight) {
            this.fullSizedTargets.add(rendertarget);
        }

    }

    public void close() {
        for(RenderTarget rendertarget : this.customRenderTargets.values()) {
            rendertarget.destroyBuffers();
        }

        for(PostPass postpass : this.passes) {
            postpass.close();
        }

        this.passes.clear();
    }

    public PostPass addPass(String name, RenderTarget inTarget, RenderTarget outTarget, boolean useLinearFilter) throws IOException {
        PostPass postpass = new PostPass(this.resourceProvider, name, inTarget, outTarget, useLinearFilter);
        this.passes.add(this.passes.size(), postpass);
        return postpass;
    }

    private void updateOrthoMatrix() {
        this.shaderOrthoMatrix = (new Matrix4f()).setOrtho(0.0F, (float)this.screenTarget.width, 0.0F, (float)this.screenTarget.height, 0.1F, 1000.0F);
    }

    public void resize(int width, int height) {
        this.screenWidth = this.screenTarget.width;
        this.screenHeight = this.screenTarget.height;
        this.updateOrthoMatrix();

        for(PostPass postpass : this.passes) {
            postpass.setOrthoMatrix(this.shaderOrthoMatrix);
        }

        for(RenderTarget rendertarget : this.fullSizedTargets) {
            rendertarget.resize(width, height, Minecraft.ON_OSX);
        }

    }

    private void setFilterMode(int filterMode) {
        this.screenTarget.setFilterMode(filterMode);

        for(RenderTarget rendertarget : this.customRenderTargets.values()) {
            rendertarget.setFilterMode(filterMode);
        }

    }

    public void process(float partialTicks, Consumer<PostPass>pConsumer) {
        int i = 9728;

        for(PostPass postpass : this.passes) {
            int j = postpass.getFilterMode();
            if (i != j) {
                this.setFilterMode(j);
                i = j;
            }

            postpass.process(this.time / 20.0F, pConsumer);
        }

        this.setFilterMode(9728);
    }

    public void setUniform(String name, float backgroundBlurriness) {
        for(PostPass postpass : this.passes) {
            postpass.getEffect().safeGetUniform(name).set(backgroundBlurriness);
        }

    }

    public final String getName() {
        return this.name;
    }

    @Nullable
    private RenderTarget getRenderTarget(@Nullable String target) {
        if (target == null) {
            return null;
        } else {
            return target.equals("minecraft:main") ? this.screenTarget : (RenderTarget)this.customRenderTargets.get(target);
        }
    }
}

