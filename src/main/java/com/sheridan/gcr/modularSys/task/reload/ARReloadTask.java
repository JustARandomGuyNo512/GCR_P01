package com.sheridan.gcr.modularSys.task.reload;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.AnimationRegister;
import com.sheridan.gcr.client.animation.command.MaskMagAmmo;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.modularSys.modules.IAmmoSource;
import com.sheridan.gcr.modularSys.modules.guns.ar.AR;
import com.sheridan.gcr.modularSys.task.GunTask;
import com.sheridan.gcr.modularSys.task.IGunTask;
import com.sheridan.gcr.network.c2s.GunReloadPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class ARReloadTask extends GunTask<AR> {
    private final String animationName;
    private final IAmmoSource magAttachment;
    public int sendPacketDelay;

    public ARReloadTask(ItemStack itemStack, AR gun) {
        super(itemStack, gun);
        CompoundTag states = gun.rootNodeTag(itemStack);
        boolean boltLocked = gun.boltLocked(states);
        boolean hasMagAttachment = gun.hasMagAttachment(states);
        int ammoLeft = gun.getGunAmmoLeft(itemStack);
        if (hasMagAttachment) {
            if (ammoLeft > 0) {
                animationName = "mag_reload";
            } else {
                animationName = boltLocked ? "mag_reload_empty" : "mag_reload_charge";
            }
        } else {
            animationName = boltLocked ? "chamber_reload_empty" : "chamber_reload";
        }
        Map<String, Float> taskTimers = gun.baseProperties.taskTimers;
        String sendPacketDelayKey = animationName + "_length";
        sendPacketDelay = Utils.secondToTick(taskTimers.getOrDefault(sendPacketDelayKey, 1.0f));
        magAttachment = gun.getMagAttachment(itemStack);
        //TODO:将task长度写入属性中而不是根据动画名字推算
        AnimationDef animationDef = AnimationRegister.get(GCR.RL("m4a1_" + animationName));
        if (animationDef != null) {
            this.length = Math.max(Utils.secondToTick(animationDef.lengthInSeconds() - 0.1f), sendPacketDelay);
        }
    }

    @Override
    public boolean equals(IGunTask<?> other) {
        if (!(other instanceof ARReloadTask otherTask) || !Objects.equals(otherTask.animationName, this.animationName)) {
            return false;
        }
        return super.equals(other);
    }

    @Override
    public void start() {
        if (animationName != null) {
            Client.getGunRenderer().dispatchAnimationEvent(EventType.RELOAD, Map.of("animation_name", animationName));
        }
    }

    @Override
    public void onTick(Player player) {
        super.onTick(player);
        if (tick == sendPacketDelay) {
            PacketDistributor.sendToServer(new GunReloadPacket());
        }
    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onComplete() {

    }


    @Override
    public TaskType getType() {
        return TaskType.RELOAD;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    protected int getPredictedAmmoCount() {
        return magAttachment == null ? 1 : magAttachment.getMaxCapacity();
    }

    @Override
    public int getCustomVariable(String variableName) {
        if (MaskMagAmmo.VARIABLE_KEY.equals(variableName)) {
            return getPredictedAmmoCount();
        }
        return -1;
    }
}
