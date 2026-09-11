package com.sheridan.gcr.client.model.modular.state.stateViewers;

import com.sheridan.gcr.modularSys.modules.views.IGP25View;

/**
 * GP25 的状态视图：只有 {@code base} 与 {@code empty} 两种状态，
 * 弹膛检查直接复用 {@link GrenadeLauncherViewer} 的公共逻辑。
 */
public class GP25Viewer extends GrenadeLauncherViewer<IGP25View> {

    public GP25Viewer(IGP25View view) {
        super(view);
    }

    @Override
    public void onRegisterStateMapping() {
        addStateMapping(BASE_STATE, "gcr:gp25_base", DEFAULT_SCALE, 0);
        addStateMapping(EMPTY_STATE, "gcr:gp25_empty", DEFAULT_SCALE, 0);
    }
}
