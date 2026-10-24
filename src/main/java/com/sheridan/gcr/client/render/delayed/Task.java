package com.sheridan.gcr.client.render.delayed;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class Task {
    private int continueFrameCount;
    public Consumer<RenderLevelStageEvent> task;

    public Task(Consumer<RenderLevelStageEvent> task, int continueFrameCount) {
        this.continueFrameCount = Math.max(1, continueFrameCount);
        this.task = task;
    }

    public Task(Consumer<RenderLevelStageEvent> task) {
        this(task, 1);
    }

    public Task forever() {
        continueFrameCount = -1;
        return this;
    }

    public void run(RenderLevelStageEvent event) {
        task.accept(event);
        if (continueFrameCount != -1) {
            continueFrameCount--;
        }
    }

    public boolean isDone() {
        return continueFrameCount == 0;
    }
}
