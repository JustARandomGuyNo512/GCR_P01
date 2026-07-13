package com.sheridan.gcr.client.model.bulletShell;

import com.sheridan.gcr.client.model.BufferedBatchSingleMeshModel;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.ModularModel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * Only visible in first person view, using first person view GunRenderer uniforms
 * */
@OnlyIn(Dist.CLIENT)
public class BulletShellModel extends BufferedBatchSingleMeshModel {
    private static final Map<ResourceLocation, BulletShellModel> INSTANCE_MAP = new HashMap<>();

    private final ResourceLocation modelID;

    public BulletShellModel(MeshModelData root, ResourceLocation modelID) {
        super(root, modelID, 20);
        INSTANCE_MAP.put(modelID, this);
        this.modelID = modelID;
    }

    @Override
    public void render(boolean isFirstPerson) {
        if (!isFirstPerson) {
            return;
        }
        super.render(true);
    }

    @Override
    protected void afterUniformLoaded(ShaderInstance shader, boolean isFirstPerson, boolean isShadowPass) {
        if (isFirstPerson && !isShadowPass) {
            //ModularModel.uploadHeatMapTex(shader.getId(), true, null);
            ModularModel.uploadMuzzleFlashEffectUniforms(shader.getId());
        }
    }

    public ResourceLocation getModelID() {
        return modelID;
    }

    public static BulletShellModel getInstance(ResourceLocation modelID) {
        return INSTANCE_MAP.get(modelID);
    }

}
