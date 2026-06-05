package com.sheridan.gcr.modularSys.task.other;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.task.GunTask;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class RemoveStuckTask<T extends IGun> extends GunTask<T> {
    public RemoveStuckTask(ItemStack itemStack, T gun) {
        super(itemStack, gun, 1);
    }

    @Override
    public void start() {
        Client.getGunRenderer().dispatchAnimationEvent(EventType.REMOVE_STUCK, Map.of());
    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onComplete() {

    }

    @Override
    public TaskType getType() {
        return TaskType.REMOVE_STUCK;
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
