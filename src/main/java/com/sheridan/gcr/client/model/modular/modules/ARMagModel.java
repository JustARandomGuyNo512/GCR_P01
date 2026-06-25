package com.sheridan.gcr.client.model.modular.modules;

import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.ModularModel;
import com.sheridan.gcr.client.model.modular.state.IStateViewer;
import com.sheridan.gcr.client.model.modular.state.IStateViewerModel;
import com.sheridan.gcr.client.model.modular.state.stateViewers.ARMagViewer;
import com.sheridan.gcr.modularSys.modules.views.IAmmoSourceView;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ARMagModel extends ModularModel implements IStateViewerModel<IAmmoSourceView> {
    protected ARMagViewer viewer;

    public ARMagModel(MeshModelData root, ResourceLocation name, ARMagViewer viewer) {
        super(root, name);
        this.viewer = viewer;
    }

    @Override
    public IStateViewer<IAmmoSourceView> getViewer() {
        return viewer;
    }
}
