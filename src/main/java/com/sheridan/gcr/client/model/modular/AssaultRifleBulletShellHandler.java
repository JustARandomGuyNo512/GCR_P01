package com.sheridan.gcr.client.model.modular;

import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.render.fx.bulletShell.BulletShellDisplay;
import com.sheridan.gcr.modularSys.modules.views.IGunView;

public class AssaultRifleBulletShellHandler extends BulletShellHandler<IGunView>{
    public AssaultRifleBulletShellHandler(IModularModel model, BulletShellDisplay display) {
        super(model, display);
    }

    @Override
    public boolean shouldThrowBulletShell(IGunView view, ReadOnlyTag states) {
        return !view.stuck(states);
    }
}
