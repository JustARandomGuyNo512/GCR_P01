package com.sheridan.gcr.client.model.modular.modules;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.GunEffect;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.*;
import com.sheridan.gcr.client.model.modular.state.stateViewers.TestM203Viewer;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.fx.muzzleFlash.CommonMuzzleFlashes;
import com.sheridan.gcr.modularSys.modules.views.IM203View;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class M203Model extends ArmHandlerModel<IM203View> implements IMuzzleFlashRendererModel {
    private final MuzzleFlashRenderer renderer;
    public M203Model(MeshModelData root, TestM203Viewer viewer) {
        super(root, viewer, GCR.RL(""));
        this.renderer = new MuzzleFlashRenderer(
                new MuzzleEntry("no1", "MUZZLE_FLASH", "", 1.8f, CommonMuzzleFlashes.SUPPRESSOR_COMMON)
        );
    }

    @Override
    public void render(ModuleRenderContext context) {
        super.render(context);
        renderer.render(context, this, GunEffect.SHOOT, context.currentRenderNode().id);
    }

    @Override
    public void afterAllRendered(ModuleRenderContext context) {
        super.afterAllRendered(context);
        renderer.afterAllRendered(context);
    }

    @Override
    public IMuzzleFlashRenderer getRenderer() {
        return renderer;
    }
}
