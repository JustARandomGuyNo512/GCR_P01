package com.sheridan.gcr.client.model.modular.modules;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.GunEffect;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.ArmHandlerModel;
import com.sheridan.gcr.client.model.modular.IMuzzleFlashRenderer;
import com.sheridan.gcr.client.model.modular.IMuzzleFlashRendererModel;
import com.sheridan.gcr.client.model.modular.MuzzleFlashRenderer;
import com.sheridan.gcr.client.model.modular.state.stateViewers.GP25Viewer;
import com.sheridan.gcr.client.model.modular.state.stateViewers.M203Viewer;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.views.IGP25View;
import com.sheridan.gcr.modularSys.modules.views.IM203View;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GP25Model extends ArmHandlerModel<IGP25View> implements IMuzzleFlashRendererModel {
    private final MuzzleFlashRenderer renderer;
    public GP25Model(MeshModelData root, GP25Viewer viewer, MuzzleFlashRenderer muzzleFlashRenderer) {
        super(root, viewer, GCR.RL(""));
        this.renderer = muzzleFlashRenderer;
    }

    @Override
    public void render(ModuleRenderContext context) {
        super.render(context);
        renderer.onRender(context, this, GunEffect.SHOOT, context.currentRenderNode().id);
    }

    @Override
    public void afterAllRendered(ModuleRenderContext context) {
        super.afterAllRendered(context);
        renderer.onAfterAllRendered(context);
    }

    @Override
    public IMuzzleFlashRenderer getRenderer() {
        return renderer;
    }
}
