package com.sheridan.gcr.client.model.modular;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.client.model.bulletShell.BulletShellModel;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.render.fx.bulletShell.BulletShellDisplay;
import com.sheridan.gcr.modularSys.modules.views.IStateView;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BulletShellHandler<T extends IStateView> implements IBulletShellHandler<T>{
    private final IModularModel model;
    private final BulletShellDisplay display;
    private BulletShellModel bulletShellModel;

    public BulletShellHandler(IModularModel model, BulletShellDisplay display) {
        this.model = model;
        this.display = display;
        getBulletShellModel();
    }

    protected BulletShellModel getBulletShellModel() {
        if (bulletShellModel != null) {
            return bulletShellModel;
        }
        BulletShellModel instance = BulletShellModel.getInstance(display.modelID);
        if (instance != null) {
            bulletShellModel = instance;
        }
        return instance;
    }

    @Override
    public BulletShellDisplay getBulletShellDisplay() {
        return display;
    }

    @Override
    public BulletShellModel getModel() {
        return getBulletShellModel();
    }

    @Override
    public PoseStack.Pose getOffsetPose() {
        return model.getBonePose(display.bindBoneName);
    }

    @Override
    public boolean shouldThrowBulletShell(T view, ReadOnlyTag states) {
        return true;
    }
}
