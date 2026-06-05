package com.sheridan.gcr.client.model.modular.modules;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.ArmHandlerModel;
import com.sheridan.gcr.client.model.modular.state.IStateViewer;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import com.sheridan.gcr.modularSys.modules.views.IStateView;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MLokFitGripModel extends ArmHandlerModel<IStateView> {
    private final Bone mLokBone;
    private final Bone main;

    public MLokFitGripModel(MeshModelData root) {
        super(root, IStateViewer.EMPTY, GCR.RL(""));
        mLokBone = getBone("M_LOK");
        main = getBone("MAIN");
        if (mLokBone == null) {
            throw new RuntimeException("Can't find M_LOK bone");
        }
        if (main == null) {
            throw new RuntimeException("Can't find MAIN bone");
        }
    }

    @Override
    public void updateBoneRenderStatus(ModuleRenderContext context) {
        int currentParam = context.getCurrentParam(IVoxelHandler.VOXEL_INDEX_PARAM_KEY);
        if (currentParam == 1) {
            main.y -= mLokBone.y;
        }
        super.updateBoneRenderStatus(context);
        if (currentParam != 1) {
            mLokBone.renderStatus.visible = false;
        }
    }
}
