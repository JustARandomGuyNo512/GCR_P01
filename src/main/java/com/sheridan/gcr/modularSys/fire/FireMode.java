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

    @Deprecated
    public boolean shouldStuck(ItemStack itemStack, T gun, float stuckRate, boolean isClientSide) {
        if (stuckRate <= 0.0F) {
            return false;
        }
        if (stuckRate >= 1.0F) {
            return true;
        }
        long hash = 1125899906842597L;

        hash = 31L * hash + gun.getIdentityID(itemStack).hashCode();
        hash = 31L * hash + gun.getStuckSeed(itemStack);
        hash = 31L * hash + gun.getAmmoLeft(itemStack);

        // SplitMix64 finalizer，分布质量较好
        hash ^= (hash >>> 30);
        hash *= 0xBF58476D1CE4E5B9L;
        hash ^= (hash >>> 27);
        hash *= 0x94D049BB133111EBL;
        hash ^= (hash >>> 31);

        double random = (double) (hash >>> 1) / (double) Long.MAX_VALUE;
        System.out.println("side: " + (isClientSide ? "client" : "server") + " " + gun.getIdentityID(itemStack).hashCode() + " " + gun.getStuckSeed(itemStack) + " " + gun.getAmmoLeft(itemStack) + " final res: " + (random < stuckRate));
        return random < stuckRate;
    }
}
