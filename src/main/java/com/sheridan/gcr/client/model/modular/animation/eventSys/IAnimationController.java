package com.sheridan.gcr.client.model.modular.animation.eventSys;

import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.render.FirstPersonRenderContext;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public interface IAnimationController<T extends IModularModel> {

    void firstPersonSubscriptions(T model);

    default void customThirdPersonAnimation(T model, ModuleRenderContext context) {}

    default void customFirstPersonAnimation(T model, FirstPersonRenderContext context) {}

    void initAnimation(T model);

    void initTrack(T moduleModel);

    void subscribe(EventType eventType, int priority, Callback callback);

    AnimationDef registerAnimation(String simpleName, ResourceLocation path);

    Track<T> defineTrack(String name);

    void onUsingNode(String id);

    void clearNode();

    List<EventRegistry> getAllSubscriptions();

    Track<T> getTrack(String name);

    void clearTrack(String trackName);

    void clearTracks(String... trackNames);

    List<Track<T>> getAllTracks();

    boolean assertCompatible(IModularModel model);

    void onUsingContext(ModuleRenderContext context);

    boolean isTrackClear(String trackName);

    boolean areTracksClear(String... trackNames);
}
