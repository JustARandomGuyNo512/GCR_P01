package com.sheridan.gcr.modularSys.fire;


import com.sheridan.gcr.Client;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.client.SprintingHandler;
import com.sheridan.gcr.client.recoil.IRecoilUpdater;
import com.sheridan.gcr.client.recoil.RecoilHandler;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.task.GunTaskHandler;
import com.sheridan.gcr.network.c2s.GunFirePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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

    @Override
    @OnlyIn(Dist.CLIENT)
    public FireControl clientIntentToFire(Player player, ItemStack stack, T gun) {
        String identityID = gun.getIdentityID(stack);
        if (IGun.NONE.equals(identityID)) {
            player.sendSystemMessage(Component.literal("server data not synced"));
            return FireControl.EXIT_FIRE_STATE;
        }
        SprintingHandler.INSTANCE.exitSprinting(Utils.secondToTick(1.3f));
        if (SprintingHandler.INSTANCE.getSprintingProgress() != 0) {
            return FireControl.CANCEL_FIRE;
        }
        if (!GunTaskHandler.INSTANCE.blockShoot()) {
            return onClientIntentToFire(player, stack, gun);
        }
        return FireControl.EXIT_FIRE_STATE;
    }

    @OnlyIn(Dist.CLIENT)
    protected abstract FireControl onClientIntentToFire(Player player, ItemStack stack, T gun);

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
