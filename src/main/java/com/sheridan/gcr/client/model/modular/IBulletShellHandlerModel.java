package com.sheridan.gcr.client.model.modular;

import com.sheridan.gcr.client.model.modular.state.IStateViewerModel;
import com.sheridan.gcr.modularSys.modules.views.IStateView;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface IBulletShellHandlerModel<T extends IStateView> extends IStateViewerModel<T> {
    IBulletShellHandler<?> getBulletShellHandler();
}
