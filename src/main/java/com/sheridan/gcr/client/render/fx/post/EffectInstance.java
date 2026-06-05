package com.sheridan.gcr.client.render.fx.post;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ChainedJsonException;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.GsonHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

@OnlyIn(Dist.CLIENT)
public class EffectInstance implements Effect, AutoCloseable {
    private static final String EFFECT_SHADER_PATH = "shaders/program/";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AbstractUniform DUMMY_UNIFORM = new AbstractUniform();
    private static final boolean ALWAYS_REAPPLY = true;
    private static EffectInstance lastAppliedEffect;
    private static int lastProgramId = -1;
    private final Map<String, IntSupplier> samplerMap = Maps.newHashMap();
    private final List<String> samplerNames = Lists.newArrayList();
    private final List<Integer> samplerLocations = Lists.newArrayList();
    private final List<Uniform> uniforms = Lists.newArrayList();
    private final List<Integer> uniformLocations = Lists.newArrayList();
    private final Map<String, Uniform> uniformMap = Maps.newHashMap();
    private final int programId;
    private final String name;
    private boolean dirty;
    private final BlendMode blend;
    private final List<Integer> attributes;
    private final List<String> attributeNames;
    private final EffectProgram vertexProgram;
    private final EffectProgram fragmentProgram;

    public EffectInstance(ResourceProvider resourceProvider, String name) throws IOException {
        ResourceLocation rl = ResourceLocation.tryParse(name);
        ResourceLocation resourcelocation = ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), "shaders/program/" + rl.getPath() + ".json");
        this.name = name;
        Resource resource = resourceProvider.getResourceOrThrow(resourcelocation);

        try (Reader reader = resource.openAsReader()) {
            JsonObject jsonobject = GsonHelper.parse(reader);
            String s = GsonHelper.getAsString(jsonobject, "vertex");
            String s1 = GsonHelper.getAsString(jsonobject, "fragment");
            JsonArray jsonarray = GsonHelper.getAsJsonArray(jsonobject, "samplers", (JsonArray)null);
            if (jsonarray != null) {
                int i = 0;

                for(JsonElement jsonelement : jsonarray) {
                    try {
                        this.parseSamplerNode(jsonelement);
                    } catch (Exception exception2) {
                        ChainedJsonException chainedjsonexception1 = ChainedJsonException.forException(exception2);
                        chainedjsonexception1.prependJsonKey("samplers[" + i + "]");
                        throw chainedjsonexception1;
                    }

                    ++i;
                }
            }

            JsonArray jsonarray1 = GsonHelper.getAsJsonArray(jsonobject, "attributes", (JsonArray)null);
            if (jsonarray1 != null) {
                int j = 0;
                this.attributes = Lists.newArrayListWithCapacity(jsonarray1.size());
                this.attributeNames = Lists.newArrayListWithCapacity(jsonarray1.size());

                for(JsonElement jsonelement1 : jsonarray1) {
                    try {
                        this.attributeNames.add(GsonHelper.convertToString(jsonelement1, "attribute"));
                    } catch (Exception exception1) {
                        ChainedJsonException chainedjsonexception2 = ChainedJsonException.forException(exception1);
                        chainedjsonexception2.prependJsonKey("attributes[" + j + "]");
                        throw chainedjsonexception2;
                    }

                    ++j;
                }
            } else {
                this.attributes = null;
                this.attributeNames = null;
            }

            JsonArray jsonarray2 = GsonHelper.getAsJsonArray(jsonobject, "uniforms", (JsonArray)null);
            if (jsonarray2 != null) {
                int k = 0;

                for(JsonElement jsonelement2 : jsonarray2) {
                    try {
                        this.parseUniformNode(jsonelement2);
                    } catch (Exception exception) {
                        ChainedJsonException chainedjsonexception3 = ChainedJsonException.forException(exception);
                        chainedjsonexception3.prependJsonKey("uniforms[" + k + "]");
                        throw chainedjsonexception3;
                    }

                    ++k;
                }
            }

            this.blend = parseBlendNode(GsonHelper.getAsJsonObject(jsonobject, "blend", (JsonObject)null));
            this.vertexProgram = getOrCreate(resourceProvider, Program.Type.VERTEX, s);
            this.fragmentProgram = getOrCreate(resourceProvider, Program.Type.FRAGMENT, s1);
            this.programId = ProgramManager.createProgram();
            ProgramManager.linkShader(this);
            this.updateLocations();
            if (this.attributeNames != null) {
                for(String s2 : this.attributeNames) {
                    int l = Uniform.glGetAttribLocation(this.programId, s2);
                    this.attributes.add(l);
                }
            }
        } catch (Exception exception3) {
            ChainedJsonException chainedjsonexception = ChainedJsonException.forException(exception3);
            String var10001 = resourcelocation.getPath();
            chainedjsonexception.setFilenameAndFlush(var10001 + " (" + resource.sourcePackId() + ")");
            throw chainedjsonexception;
        }

        this.markDirty();
    }

    public static EffectProgram getOrCreate(ResourceProvider resourceProvider, Program.Type type, String name) throws IOException {
        Program program = (Program)type.getPrograms().get(name);
        if (program != null && !(program instanceof EffectProgram)) {
            throw new InvalidClassException("Program is not of type EffectProgram");
        } else {
            EffectProgram effectprogram;
            if (program == null) {
                ResourceLocation rl = ResourceLocation.tryParse(name);
                String var10000 = rl.getNamespace();
                String var10001 = rl.getPath();
                ResourceLocation resourcelocation = ResourceLocation.fromNamespaceAndPath(var10000, "shaders/program/" + var10001 + type.getExtension());
                Resource resource = resourceProvider.getResourceOrThrow(resourcelocation);

                try (InputStream inputstream = resource.open()) {
                    effectprogram = EffectProgram.compileShader(type, name, inputstream, resource.sourcePackId());
                }
            } else {
                effectprogram = (EffectProgram)program;
            }

            return effectprogram;
        }
    }

    public static BlendMode parseBlendNode(@Nullable JsonObject json) {
        if (json == null) {
            return new BlendMode();
        } else {
            int i = 32774;
            int j = 1;
            int k = 0;
            int l = 1;
            int i1 = 0;
            boolean flag = true;
            boolean flag1 = false;
            if (GsonHelper.isStringValue(json, "func")) {
                i = BlendMode.stringToBlendFunc(json.get("func").getAsString());
                if (i != 32774) {
                    flag = false;
                }
            }

            if (GsonHelper.isStringValue(json, "srcrgb")) {
                j = BlendMode.stringToBlendFactor(json.get("srcrgb").getAsString());
                if (j != 1) {
                    flag = false;
                }
            }

            if (GsonHelper.isStringValue(json, "dstrgb")) {
                k = BlendMode.stringToBlendFactor(json.get("dstrgb").getAsString());
                if (k != 0) {
                    flag = false;
                }
            }

            if (GsonHelper.isStringValue(json, "srcalpha")) {
                l = BlendMode.stringToBlendFactor(json.get("srcalpha").getAsString());
                if (l != 1) {
                    flag = false;
                }

                flag1 = true;
            }

            if (GsonHelper.isStringValue(json, "dstalpha")) {
                i1 = BlendMode.stringToBlendFactor(json.get("dstalpha").getAsString());
                if (i1 != 0) {
                    flag = false;
                }

                flag1 = true;
            }

            if (flag) {
                return new BlendMode();
            } else {
                return flag1 ? new BlendMode(j, k, l, i1, i) : new BlendMode(j, k, i);
            }
        }
    }

    public void close() {
        for(Uniform uniform : this.uniforms) {
            uniform.close();
        }

        ProgramManager.releaseProgram(this);
    }

    public void clear() {
        RenderSystem.assertOnRenderThread();
        ProgramManager.glUseProgram(0);
        lastProgramId = -1;
        lastAppliedEffect = null;

        for(int i = 0; i < this.samplerLocations.size(); ++i) {
            if (this.samplerMap.get(this.samplerNames.get(i)) != null) {
                GlStateManager._activeTexture('蓀' + i);
                GlStateManager._bindTexture(0);
            }
        }

    }

    public void apply() {
        this.dirty = false;
        lastAppliedEffect = this;
        this.blend.apply();
        if (this.programId != lastProgramId) {
            ProgramManager.glUseProgram(this.programId);
            lastProgramId = this.programId;
        }

        for(int i = 0; i < this.samplerLocations.size(); ++i) {
            String s = (String)this.samplerNames.get(i);
            IntSupplier intsupplier = (IntSupplier)this.samplerMap.get(s);
            if (intsupplier != null) {
                RenderSystem.activeTexture('蓀' + i);
                int j = intsupplier.getAsInt();
                if (j != -1) {
                    RenderSystem.bindTexture(j);
                    Uniform.uploadInteger((Integer)this.samplerLocations.get(i), i);
                }
            }
        }

        for(Uniform uniform : this.uniforms) {
            uniform.upload();
        }

    }

    public void markDirty() {
        this.dirty = true;
    }

    @Nullable
    public Uniform getUniform(String name) {
        RenderSystem.assertOnRenderThread();
        return (Uniform)this.uniformMap.get(name);
    }

    public AbstractUniform safeGetUniform(String name) {
        Uniform uniform = this.getUniform(name);
        return (AbstractUniform)(uniform == null ? DUMMY_UNIFORM : uniform);
    }

    private void updateLocations() {
        RenderSystem.assertOnRenderThread();
        IntList intlist = new IntArrayList();

        for(int i = 0; i < this.samplerNames.size(); ++i) {
            String s = (String)this.samplerNames.get(i);
            int j = Uniform.glGetUniformLocation(this.programId, s);
            if (j == -1) {
                LOGGER.warn("Shader {} could not find sampler named {} in the specified shader program.", this.name, s);
                this.samplerMap.remove(s);
                intlist.add(i);
            } else {
                this.samplerLocations.add(j);
            }
        }

        for(int l = intlist.size() - 1; l >= 0; --l) {
            this.samplerNames.remove(intlist.getInt(l));
        }

        for(Uniform uniform : this.uniforms) {
            String s1 = uniform.getName();
            int k = Uniform.glGetUniformLocation(this.programId, s1);
            if (k == -1) {
                LOGGER.warn("Shader {} could not find uniform named {} in the specified shader program.", this.name, s1);
            } else {
                this.uniformLocations.add(k);
                uniform.setLocation(k);
                this.uniformMap.put(s1, uniform);
            }
        }

    }

    private void parseSamplerNode(JsonElement json) {
        JsonObject jsonobject = GsonHelper.convertToJsonObject(json, "sampler");
        String s = GsonHelper.getAsString(jsonobject, "name");
        if (!GsonHelper.isStringValue(jsonobject, "file")) {
            this.samplerMap.put(s, (IntSupplier) null);
            this.samplerNames.add(s);
        } else {
            this.samplerNames.add(s);
        }

    }

    public void setSampler(String name, IntSupplier textureId) {
        if (this.samplerMap.containsKey(name)) {
            this.samplerMap.remove(name);
        }

        this.samplerMap.put(name, textureId);
        this.markDirty();
    }

    private void parseUniformNode(JsonElement json) throws ChainedJsonException {
        JsonObject jsonobject = GsonHelper.convertToJsonObject(json, "uniform");
        String s = GsonHelper.getAsString(jsonobject, "name");
        int i = Uniform.getTypeFromString(GsonHelper.getAsString(jsonobject, "type"));
        int j = GsonHelper.getAsInt(jsonobject, "count");
        float[] afloat = new float[Math.max(j, 16)];
        JsonArray jsonarray = GsonHelper.getAsJsonArray(jsonobject, "values");
        if (jsonarray.size() != j && jsonarray.size() > 1) {
            throw new ChainedJsonException("Invalid amount of values specified (expected " + j + ", found " + jsonarray.size() + ")");
        } else {
            int k = 0;

            for(JsonElement jsonelement : jsonarray) {
                try {
                    afloat[k] = GsonHelper.convertToFloat(jsonelement, "value");
                } catch (Exception exception) {
                    ChainedJsonException chainedjsonexception = ChainedJsonException.forException(exception);
                    chainedjsonexception.prependJsonKey("values[" + k + "]");
                    throw chainedjsonexception;
                }

                ++k;
            }

            if (j > 1 && jsonarray.size() == 1) {
                while(k < j) {
                    afloat[k] = afloat[0];
                    ++k;
                }
            }

            int l = j > 1 && j <= 4 && i < 8 ? j - 1 : 0;
            Uniform uniform = new Uniform(s, i + l, j, this);
            if (i <= 3) {
                uniform.setSafe((int)afloat[0], (int)afloat[1], (int)afloat[2], (int)afloat[3]);
            } else if (i <= 7) {
                uniform.setSafe(afloat[0], afloat[1], afloat[2], afloat[3]);
            } else {
                uniform.set(afloat);
            }

            this.uniforms.add(uniform);
        }
    }

    public Program getVertexProgram() {
        return this.vertexProgram;
    }

    public Program getFragmentProgram() {
        return this.fragmentProgram;
    }

    public void attachToProgram() {
        this.fragmentProgram.attachToEffect(this);
        this.vertexProgram.attachToEffect(this);
    }

    public String getName() {
        return this.name;
    }

    public int getId() {
        return this.programId;
    }
}
