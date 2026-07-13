package com.sheridan.gcr.modularSys.fire;


import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.recoil.IRecoilUpdater;
import com.sheridan.gcr.client.recoil.RecoilHandler;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.c2s.GunFirePacket;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public abstract class FireMode<T extends IGun> implements IFireMode<T>{
    private final String name;

    public FireMode(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    protected void sendPacket() {
        IRecoilUpdater recoilUpdater = RecoilHandler.INSTANCE.getRecoilUpdater();
        float gunKickPitch = 0;
        float gunKickYaw = 0;
        if (recoilUpdater != null) {
            gunKickPitch = recoilUpdater.getGunKickPitch();
            gunKickYaw = recoilUpdater.getGunKickYaw();
        }
        PacketDistributor.sendToServer(new GunFirePacket(Client.CLIENT_SHOOT_ID.incrementAndGet(), gunKickPitch, gunKickYaw));
    }

}
