package com.sheridan.gcr.client.model.modular.modules;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.GunEffect;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.ArmHandlerModel;
import com.sheridan.gcr.client.model.modular.IMuzzleFlashRenderer;
import com.sheridan.gcr.client.model.modular.IMuzzleFlashRendererModel;
import com.sheridan.gcr.client.model.modular.MuzzleFlashRenderer;
import com.sheridan.gcr.client.model.modular.state.IStateViewer;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.views.IGrenadeLauncherView;

/**
 * 榴弹发射器模型的公共实现。
 *
 * <p>M203 与 GP25 的模型只差一个状态视图与炮口焰参数，渲染流程完全一致：
 * 走 {@link ArmHandlerModel} 的手臂姿态，再叠加开火炮口焰。</p>
 */
public class GrenadeLauncherModel<T extends IGrenadeLauncherView> extends ArmHandlerModel<T> implements IMuzzleFlashRendererModel {

    private final MuzzleFlashRenderer muzzleFlashRenderer;

    public GrenadeLauncherModel(MeshModelData root, IStateViewer<T> viewer, MuzzleFlashRenderer muzzleFlashRenderer) {
        super(root, viewer, GCR.RL(""));
        this.muzzleFlashRenderer = muzzleFlashRenderer;
    }

    @Override
    public void render(ModuleRenderContext context) {
        super.render(context);
        muzzleFlashRenderer.onRender(context, this, GunEffect.SHOOT, context.currentRenderNode().id);
    }

    @Override
    public void afterAllRendered(ModuleRenderContext context) {
        super.afterAllRendered(context);
        muzzleFlashRenderer.onAfterAllRendered(context);
    }

    @Override
    public IMuzzleFlashRenderer getRenderer() {
        return muzzleFlashRenderer;
    }
}
