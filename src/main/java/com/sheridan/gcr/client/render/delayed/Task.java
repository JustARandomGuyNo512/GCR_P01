package com.sheridan.gcr.client.render.delayed;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class Task {
    private int continueFrameCount;
    public Consumer<Float> task;

    public Task(Consumer<Float> task, int continueFrameCount) {
        this.continueFrameCount = Math.max(1, continueFrameCount);
        this.task = task;
    }

    public Task(Consumer<Float> task) {
        this(task, 1);
    }

    public Task forever() {
        continueFrameCount = -1;
        return this;
    }

    public void run(float partialTicks) {
        task.accept(partialTicks);
        if (continueFrameCount != -1) {
            continueFrameCount--;
        }
    }

    public boolean isDone() {
        return continueFrameCount == 0;
    }
}
