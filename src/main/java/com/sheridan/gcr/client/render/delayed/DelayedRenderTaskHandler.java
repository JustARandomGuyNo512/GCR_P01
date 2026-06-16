package com.sheridan.gcr.client.render.delayed;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;


@OnlyIn(Dist.CLIENT)
public class DelayedRenderTaskHandler {

    @SubscribeEvent
    public static void onRenderLevelLast(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            Stage.HIGHEST.handleTasks(event);
            Stage.HIGH.handleTasks(event);
            Stage.NORMAL.handleTasks(event);
            Stage.LOW.handleTasks(event);
            Stage.LOWEST.handleTasks(event);
        }
    }

}
