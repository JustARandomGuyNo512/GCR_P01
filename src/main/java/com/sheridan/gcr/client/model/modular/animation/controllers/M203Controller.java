package com.sheridan.gcr.client.model.modular.animation.controllers;

import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.modules.TestM203Model;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class M203Controller extends AnimationController<TestM203Model> {
    @Override
    public void firstPersonSubscriptions(TestM203Model model) {

    }

    @Override
    public void initAnimation(TestM203Model model) {

    }

    @Override
    public void initTrack(TestM203Model moduleModel) {

    }

    @Override
    public void thirdPersonAnimation(TestM203Model model, ModuleRenderContext context) {

    }

    @Override
    public boolean assertCompatible(IModularModel model) {
        return model instanceof TestM203Model;
    }

}
