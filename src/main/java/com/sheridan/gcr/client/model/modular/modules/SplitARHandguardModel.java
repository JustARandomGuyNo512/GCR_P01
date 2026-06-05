package com.sheridan.gcr.client.model.modular.modules;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.ArmHandlerModel;
import com.sheridan.gcr.client.model.modular.state.IStateViewer;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.impl.SplitARHandguard;
import com.sheridan.gcr.modularSys.modules.views.IStateView;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SplitARHandguardModel extends ArmHandlerModel<IStateView> {

    public SplitARHandguardModel(MeshModelData root) {
        super(root, IStateViewer.EMPTY, GCR.RL(""));
    }

    @Override
    public void updateBoneRenderStatus(ModuleRenderContext context) {
        super.updateBoneRenderStatus(context);
        int currentParam = context.getCurrentParam(SplitARHandguard.HIDE_LOWER_PART_PARAM_KEY);
        getBone("lower").renderStatus.visible = currentParam != 1;
    }
}
