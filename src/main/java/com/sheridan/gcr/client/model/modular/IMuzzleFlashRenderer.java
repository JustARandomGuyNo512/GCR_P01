package com.sheridan.gcr.client.model.modular;

import com.sheridan.gcr.client.GunEffect;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public interface IMuzzleFlashRenderer {
    @NotNull
    List<MuzzleEntry> getMuzzleFlashEntries();

    @Nullable
    MuzzleEntry getByName(String name);

    void onRender(ModuleRenderContext context, IMuzzleFlashRendererModel model, GunEffect effectListener, String effectModuleId);

    void onAfterAllRendered(ModuleRenderContext context);
}
