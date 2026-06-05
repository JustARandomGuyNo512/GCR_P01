package com.sheridan.gcr.modularSys.task.other;

import com.sheridan.gcr.modularSys.modules.guns.SlottedGunMainPart;
import com.sheridan.gcr.modularSys.task.GunTask;
import com.sheridan.gcr.network.c2s.SwitchUsingSightPacket;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;


public class SwitchUsingSightTask extends GunTask<SlottedGunMainPart> {
    public SwitchUsingSightTask(ItemStack itemStack, SlottedGunMainPart gun) {
        super(itemStack, gun, 1);
    }

    @Override
    public void start() {
        gun.switchUsingSight(itemStack);
        PacketDistributor.sendToServer(new SwitchUsingSightPacket());
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
        return TaskType.SWITCH_USING_SIGHT;
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
