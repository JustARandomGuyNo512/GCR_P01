package com.sheridan.gcr.client.render.delayed;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public enum Stage {
    HIGHEST(new ArrayList<>()),
    HIGH(new ArrayList<>()),
    NORMAL(new ArrayList<>()),
    LOW(new ArrayList<>()),
    LOWEST(new ArrayList<>());

    private final List<Task> tasks;

    Stage(List<Task> tasks) {
        this.tasks = tasks;
    }

    void handleTasks(float partialTicks) {
        for (int i = 0; i < tasks.size(); ) {
            Task task = tasks.get(i);
            task.run(partialTicks);
            if (task.isDone()) {
                tasks.remove(i);
            } else {
                i++;
            }
        }
    }

    public void addTask(Task task) {
        this.tasks.add(task);
    }

    public boolean remove(Task task) {
        return tasks.remove(task);
    }
}
