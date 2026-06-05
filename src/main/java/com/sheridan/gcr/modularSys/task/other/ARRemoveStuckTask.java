package com.sheridan.gcr.modularSys.task.other;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.AnimationRegister;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.modularSys.modules.IAmmoSource;
import com.sheridan.gcr.modularSys.modules.guns.ar.AR;
import com.sheridan.gcr.network.c2s.RemoveStuckPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

public class ARRemoveStuckTask extends RemoveStuckTask<AR>{
    public String animationName;
    public int sendPacketDelay;

    public ARRemoveStuckTask(ItemStack itemStack, AR gun) {
        super(itemStack, gun);
        CompoundTag magStates = gun.getAmmoSourceTag(itemStack);
        IAmmoSource mag = gun.getMagAttachment(itemStack);
        if (mag == null) {
            animationName = "remove_stuck_empty";
        } else {
            int ammoLeft = mag.getAmmoLeft(magStates);
            if (ammoLeft >= 1) {
                animationName = "remove_stuck";
            } else {
                animationName = "remove_stuck_empty";
            }
        }
        String sendPacketDelayKey = animationName + "_length";
        sendPacketDelay = Utils.secondToTick(gun.baseProperties.taskTimers.getOrDefault(sendPacketDelayKey, 1.0f));
        AnimationDef animationDef = AnimationRegister.get(GCR.RL("m4a1_" + animationName));
        if (animationDef != null) {
            this.length = Math.max(Utils.secondToTick(animationDef.lengthInSeconds() - 0.1f), sendPacketDelay);
        }
    }

    @Override
    public void onTick(Player player) {
        super.onTick(player);
        if (tick == sendPacketDelay) {
            PacketDistributor.sendToServer(new RemoveStuckPacket());
        }
    }

    @Override
    public void start() {
        if (animationName != null) {
            Client.getGunRenderer().dispatchAnimationEvent(EventType.REMOVE_STUCK, Map.of("name", animationName));
        }
    }
}
