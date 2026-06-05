package com.sheridan.gcr.modularSys.task.other;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.modularSys.fire.IFireMode;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.task.GunTask;
import com.sheridan.gcr.network.c2s.SyncGunStatusPacket;
import net.minecraft.world.item.ItemStack;

import java.util.Map;


public class FireModeSwitchTask extends GunTask<IGun> {
    public FireModeSwitchTask(ItemStack itemStack, IGun gun) {
        super(itemStack, gun, 1);
    }

    @Override
    public void start() {
        IFireMode<?> fireMode = gun.getFireMode(itemStack);
        gun.toNextFireMode(itemStack);
        IFireMode<?> afterMode = gun.getFireMode(itemStack);
        if (afterMode != fireMode) {
            SyncGunStatusPacket.toServer(gun, itemStack, IGun.FIRE_MODEL_ID);
            Client.getGunRenderer().dispatchAnimationEvent(
                    EventType.SWITCH_FIRE_MODE,
                    Map.of("before", fireMode.getName(), "after", afterMode.getName())
            );
        }

    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onComplete() {

    }

    @Override
    public boolean blockShoot() {
        return false;
    }

    @Override
    public TaskType getType() {
        return TaskType.SWITCH_FIRE_MODE;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public boolean blockSprinting() {
        return false;
    }
}
