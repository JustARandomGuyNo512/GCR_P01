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
            float realtimeDeltaTicks = event.getPartialTick().getRealtimeDeltaTicks();
            Stage.HIGHEST.handleTasks(realtimeDeltaTicks);
            Stage.HIGH.handleTasks(realtimeDeltaTicks);
            Stage.NORMAL.handleTasks(realtimeDeltaTicks);
            Stage.LOW.handleTasks(realtimeDeltaTicks);
            Stage.LOWEST.handleTasks(realtimeDeltaTicks);
        }
    }

}
