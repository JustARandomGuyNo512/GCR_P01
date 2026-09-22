package com.sheridan.gcr.modularSys.task.other;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.AnimationRegister;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.client.stuck.ClientGunStuckCache;
import com.sheridan.gcr.modularSys.modules.guns.ak.AK;
import com.sheridan.gcr.modularSys.task.IGunTask;
import com.sheridan.gcr.network.c2s.RemoveStuckPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class AKRemoveStuckTask extends RemoveStuckTask<AK>{
    public String animationName;
    public int sendPacketDelay;

    public AKRemoveStuckTask(ItemStack itemStack, AK gun) {
        super(itemStack, gun);
        animationName = "remove_stuck";
        int ammoLeft = gun.getAmmoLeft(itemStack);
        if (ammoLeft <= 0) {
            animationName += "_empty";
        }
        String sendPacketDelayKey = animationName + "_length";
        sendPacketDelay = Utils.secondToTick(gun.baseProperties.taskTimers.getOrDefault(sendPacketDelayKey, 1.0f));
        AnimationDef animationDef = AnimationRegister.get(GCR.RL("ak74m_" + animationName));
        if (animationDef != null) {
            this.length = Math.max(Utils.secondToTick(animationDef.lengthInSeconds() - 0.05f), sendPacketDelay);
        }
    }

    @Override
    public boolean equals(IGunTask<?> other) {
        if (!(other instanceof AKRemoveStuckTask otherTask) || !Objects.equals(otherTask.animationName, this.animationName)) {
            return false;
        }
        return super.equals(other);
    }

    @Override
    public void onTick(Player player) {
        super.onTick(player);
        if (tick == sendPacketDelay) {
            String gunId = gun.getIdentityID(itemStack);
            PacketDistributor.sendToServer(new RemoveStuckPacket(gunId));
            // 清障请求已经发出：本地预测（服务端从未确认过的卡壳）可以立刻作废；
            // 已确认的卡壳等服务端回执，避免“刚清完又卡上”的抖动。
            ClientGunStuckCache.get().onLocalClearRequested(gunId);
        }
    }

    @Override
    public void start() {
        if (animationName != null) {
            Client.getGunRenderer().dispatchAnimationEvent(EventType.REMOVE_STUCK, Map.of("name", animationName));
        }
    }
}
