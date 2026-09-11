package com.sheridan.gcr.client.model.modular.animation.controllers;

import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.modules.M203Model;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * M203 的动画控制器。动画资源与 GP25 不同，其余行为继承
 * {@link GrenadeLauncherController}。
 */
@OnlyIn(Dist.CLIENT)
public class M203Controller extends GrenadeLauncherController<M203Model> {

    @Override
    protected String getCheckAnimationPath() {
        return "gcr:m4a1_check_grenade_m203.g";
    }

    @Override
    protected String getReloadAnimationPath() {
        return "gcr:m4a1_reload_grenade_m203.g";
    }

    @Override
    public boolean assertCompatible(IModularModel model) {
        return model instanceof M203Model;
    }
}
