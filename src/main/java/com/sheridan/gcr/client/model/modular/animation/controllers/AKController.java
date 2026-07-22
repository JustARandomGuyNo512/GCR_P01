package com.sheridan.gcr.client.model.modular.animation.controllers;

import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.modules.AKModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AKController extends AnimationController<AKModel> {
    @Override
    public void initAnimation(AKModel model) {

    }

    @Override
    public void initTrack(AKModel moduleModel) {

    }

    @Override
    public boolean assertCompatible(IModularModel model) {
        return model instanceof AKModel;
    }
}
