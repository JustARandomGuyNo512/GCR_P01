package com.sheridan.gcr.client.model.modular;

import com.sheridan.gcr.client.model.modular.animation.eventSys.IAnimationController;
import com.sheridan.gcr.client.render.FirstPersonRenderContext;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Optional;


@OnlyIn(Dist.CLIENT)
public interface IAnimationControllerModel {
    Optional<IAnimationController<?>> getController();

    void bindController(IAnimationController<?> controller);

    void callInitEventSubscriptions();

    void callInitAnimation();

    void callInitTrack();

    void applyFirstPersonAnimation(ModuleRenderContext context);

    void applyThirdPersonAnimation(ModuleRenderContext context);

    void applyCustomFirstPersonAnimation(FirstPersonRenderContext firstPersonRenderContext);
}
