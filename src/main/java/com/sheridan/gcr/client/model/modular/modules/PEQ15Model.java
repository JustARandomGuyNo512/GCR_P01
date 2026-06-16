package com.sheridan.gcr.client.model.modular.modules;

import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.IFlashLightHandlerModel;
import com.sheridan.gcr.client.model.modular.ILaserSightModel;
import com.sheridan.gcr.client.model.modular.LaserSighRenderer;
import com.sheridan.gcr.client.model.modular.ModularModel;
import com.sheridan.gcr.client.model.modular.state.IStateViewerModel;
import com.sheridan.gcr.client.model.modular.state.stateViewers.FlashLightStatesViewer;
import com.sheridan.gcr.client.render.FirstPersonRenderContext;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import com.sheridan.gcr.modularSys.modules.views.IFlashLightView;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.awt.*;

@OnlyIn(Dist.CLIENT)
public class PEQ15Model extends ModularModel implements IFlashLightHandlerModel, IStateViewerModel<IFlashLightView>, ILaserSightModel {
    private final FlashLightStatesViewer viewer;
    private final Bone mLokBone;
    private final Bone main;
    private final Bone laserSource;
    private final LaserSighRenderer renderer;
    public PEQ15Model(MeshModelData root, ResourceLocation name, FlashLightStatesViewer viewer) {
        super(root, name);
        this.viewer = viewer;
        mLokBone = getOrThrow("M_LOK");
        main = getOrThrow("MAIN");
        laserSource = getOrThrow("LASER");
        renderer = new LaserSighRenderer(this, Color.GREEN.getRGB());
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


    @Override
    public void preFirstPersonRender(FirstPersonRenderContext context) {
        super.preFirstPersonRender(context);
        IFlashLightView stateView = viewer.getStateView();
        if(stateView.isOn(context.currentRenderNode().getStates())) {
            handleFlashLightEffect(context, stateView.getLuminance(), stateView.getRange(), stateView.getAngle(), context.partialTicks);
        }
    }

    @Override
    public void render(ModuleRenderContext context) {
        super.render(context);
        if (context.isFirstPerson()) {
            renderer.renderFirstPerson(context);
        } else if (context.isThirdPerson()) {
            renderer.renderGeneric(context);
        }
    }

    @Override
    public Bone getLightDirPoseBone() {
        return getRootBone();
    }


    @Override
    public FlashLightStatesViewer getViewer() {
        return viewer;
    }

    @Override
    public Bone getLaserPoseBone() {
        return laserSource;
    }

    @Override
    public LaserSighRenderer getRenderer() {
        return renderer;
    }
}
