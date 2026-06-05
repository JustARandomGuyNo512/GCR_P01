package com.sheridan.gcr.modularSys.task.other;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.task.GunTask;
import net.minecraft.world.item.ItemStack;


public class CheckingTask extends GunTask<IGun> {
    public static final String CHECK_MAG = "check_mag";
    public static final String CHECK_CHAMBER = "check_chamber";

    private final String type;

    public CheckingTask(ItemStack itemStack, IGun gun, String type) {
        super(itemStack, gun, 1);
        this.type = type;
    }

    @Override
    public void start() {
        if (Client.isAiming()) {
            this.tick = this.length;
            return;
        }
        if (CHECK_MAG.equals(type)) {
            boolean hasMagAttachment = gun.hasMagAttachment(gun.rootNodeTag(itemStack));
            if (hasMagAttachment) {
                Client.getGunRenderer().dispatchAnimationEvent(EventType.CHECK_MAG);
            }
        } else if (CHECK_CHAMBER.equals(type)) {
            Client.getGunRenderer().dispatchAnimationEvent(EventType.CHECK_CHAMBER);
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
        return TaskType.CHECKING;
    }

    @Override
    public int getPriority() {
        return -1;
    }
}
