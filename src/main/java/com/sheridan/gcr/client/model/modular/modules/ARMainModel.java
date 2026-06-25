package com.sheridan.gcr.client.model.modular.modules;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.*;
import com.sheridan.gcr.client.model.modular.state.stateViewers.ARMainViewer;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.fx.bulletShell.BulletShellDisplay;
import com.sheridan.gcr.modularSys.modules.views.ARView;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ARMainModel extends ArmHandlerModel<ARView> implements IBulletShellHandlerModel<ARView>, ISightModel, IGunModel {
    private final AssaultRifleBulletShellHandler bulletShellHandler;
    private final Bone handHoldPivot;

    public ARMainModel(MeshModelData root, BulletShellDisplay display, ARMainViewer viewer) {
        super(root, viewer, GCR.RL("m4a1_main"));
        this.bulletShellHandler = new AssaultRifleBulletShellHandler(this, display);
        this.handHoldPivot = getOrThrow(DEFAULT_HAND_ROT_PIVOT_NAME);
    }

    @Override
    public IBulletShellHandler<?> getBulletShellHandler() {
        return bulletShellHandler;
    }

    @Override
    public String getFarthestSightZName(ModuleRenderContext context) {
        return DEFAULT_FARTHEST_SIGHT_Z_NAME;
    }

    @Override
    public Bone getHandRotPivot() {
        return handHoldPivot;
    }
}
