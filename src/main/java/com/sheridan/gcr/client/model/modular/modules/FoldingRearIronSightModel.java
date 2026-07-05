package com.sheridan.gcr.client.model.modular.modules;

import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.FoldingIronSightVoxelHandler;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FoldingRearIronSightModel extends SightModel{
    private final Bone fold;
    private final float foldRot;

    public FoldingRearIronSightModel(MeshModelData root, ResourceLocation name, float foldRotDeg) {
        super(root, name);
        this.fold = getOrThrow("FOLD");
        this.foldRot = (float) Math.toRadians(foldRotDeg);
    }

    @Override
    public void updateBoneRenderStatus(ModuleRenderContext context) {
        int currentParam = context.getCurrentParam(FoldingIronSightVoxelHandler.FOLD_SIGHT_PARAM_KEY);
        if (currentParam == 1) {
            fold.xRot += foldRot;
        }
        super.updateBoneRenderStatus(context);
    }
}
