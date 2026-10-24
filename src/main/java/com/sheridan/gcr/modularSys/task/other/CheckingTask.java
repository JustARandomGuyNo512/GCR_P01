package com.sheridan.gcr.modularSys.task.other;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.task.GunTask;
import com.sheridan.gcr.modularSys.task.IGunTask;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Objects;


public class CheckingTask extends GunTask<IGun> {
    public static final String CHECK_MAG = "check_mag";
    public static final String CHECK_CHAMBER = "check_chamber";
    public static final String CHECK_SUB_WEAPON = "check_sub_weapon";

    private final String type;
    private Map<String, String> params;

    public CheckingTask(ItemStack itemStack, IGun gun, String type) {
        super(itemStack, gun, 20);
        this.type = type;
    }

    public CheckingTask(ItemStack itemStack, IGun gun, String type, Map<String, String> params) {
        super(itemStack, gun, 20);
        this.type = type;
        this.params = params;
    }

    @Override
    public boolean equals(IGunTask<?> other) {
        if (!(other instanceof CheckingTask otherTask) || !Objects.equals(otherTask.params, this.params)) {
            return false;
        }
        return super.equals(other);
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
                Client.getGunRenderer().dispatchAnimationEvent(EventType.CHECK_MAG, params);
            }
        } else if (CHECK_CHAMBER.equals(type)) {
            Client.getGunRenderer().dispatchAnimationEvent(EventType.CHECK_CHAMBER, params);
        } else if (CHECK_SUB_WEAPON.equals(type)) {
            Client.getGunRenderer().dispatchAnimationEvent(EventType.CHECK_SUB_WEAPON, params);
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
