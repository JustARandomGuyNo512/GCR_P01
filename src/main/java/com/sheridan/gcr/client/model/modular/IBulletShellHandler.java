package com.sheridan.gcr.client.model.modular;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.client.model.bulletShell.BulletShellModel;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.render.fx.bulletShell.BulletShellDisplay;
import com.sheridan.gcr.modularSys.modules.views.IStateView;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface IBulletShellHandler<T extends IStateView> {
    BulletShellDisplay getBulletShellDisplay();
    BulletShellModel getModel();
    PoseStack.Pose getOffsetPose();
    boolean shouldThrowBulletShell(T view, ReadOnlyTag states);
}
