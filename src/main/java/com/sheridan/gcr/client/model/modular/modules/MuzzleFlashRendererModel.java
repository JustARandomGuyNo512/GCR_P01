package com.sheridan.gcr.client.model.modular.modules;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.GunEffect;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.IMuzzleFlashRenderer;
import com.sheridan.gcr.client.model.modular.IMuzzleFlashRendererModel;
import com.sheridan.gcr.client.model.modular.ModularModel;
import com.sheridan.gcr.client.model.modular.MuzzleFlashRenderer;
import com.sheridan.gcr.client.render.ModuleRenderContext;

public class MuzzleFlashRendererModel extends ModularModel implements IMuzzleFlashRendererModel {
    protected final MuzzleFlashRenderer muzzleFlashRenderer;

    public MuzzleFlashRendererModel(MeshModelData root, MuzzleFlashRenderer muzzleFlashRenderer) {
        super(root, GCR.RL(""));
        this.muzzleFlashRenderer = muzzleFlashRenderer;
    }

    @Override
    public void render(ModuleRenderContext context) {
        super.render(context);
        renderMuzzleFlash(context);
    }

    protected void renderMuzzleFlash(ModuleRenderContext context) {
        muzzleFlashRenderer.onRender(context, this, GunEffect.SHOOT, context.root.id);
    }

    @Override
    public void afterAllRendered(ModuleRenderContext context) {
        super.afterAllRendered(context);
        muzzleFlashRenderer.afterAllRendered(context);
    }

    @Override
    public IMuzzleFlashRenderer getRenderer() {
        return muzzleFlashRenderer;
    }

}
