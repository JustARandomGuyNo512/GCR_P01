package com.sheridan.gcr.modularSys.task.reload;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.task.GunTask;
import com.sheridan.gcr.network.c2s.SubWeaponReloadPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

public class M203ReloadTask extends GunTask<IGun> {
    private final int sendPacketDelay;
    private final String nodeId;
    private final String gunId;
    public M203ReloadTask(ItemStack itemStack, IGun gun, int lengthInTicks, int sendPacketDelay,String nodeId, String gunId) {
        super(itemStack, gun, lengthInTicks);
        this.sendPacketDelay = sendPacketDelay;
        this.nodeId = nodeId;
        this.gunId = gunId;
    }

    @Override
    public void start() {
        Client.getGunRenderer().dispatchAnimationEvent(EventType.RELOAD_SUB_WEAPON, Map.of("animation_name", "reload_grenade"));
    }

    @Override
    public void onTick(Player player) {
        super.onTick(player);
        if (tick == sendPacketDelay) {
            PacketDistributor.sendToServer(new SubWeaponReloadPacket(gunId, nodeId));
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

    public int getSendPacketDelay() {
        return sendPacketDelay;
    }
}
