package com.sheridan.gcr.client.model.modular.state;

import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.modularSys.modules.views.IStateView;

public interface IStateViewerModel<T extends IStateView> extends IModularModel {
    IStateViewer<T> getViewer();

    default T getView() {
        return getViewer().getStateView();
    }

}
