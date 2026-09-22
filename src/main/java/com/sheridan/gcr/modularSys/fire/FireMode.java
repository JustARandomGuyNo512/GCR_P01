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

    /**
     * 取下一个开火 id。与发包分离是为了让调用方能在发包之前先把本地卡壳预测登记好，
     * 否则服务端极快地回执时会出现“回执先到、预测后登记”的竞态。
     */
    @OnlyIn(Dist.CLIENT)
    protected static int nextShootId() {
        return Client.CLIENT_SHOOT_ID.incrementAndGet();
    }

    /**
     * 把这一发发给服务端。
     *
     * @param stuck 客户端在开火那一刻判定的卡壳结果，服务端校验后直接采信
     * @param gunId 开火时手持枪的 identityID，服务端据此确认这一发属于哪把枪
     */
    @OnlyIn(Dist.CLIENT)
    protected void sendPacket(int shootId, boolean stuck, String gunId) {
        IRecoilUpdater recoilUpdater = RecoilHandler.INSTANCE.getRecoilUpdater();
        float gunKickPitch = 0;
        float gunKickYaw = 0;
        if (recoilUpdater != null) {
            gunKickPitch = recoilUpdater.getGunKickPitch();
            gunKickYaw = recoilUpdater.getGunKickYaw();
        }
        PacketDistributor.sendToServer(new GunFirePacket(shootId, gunId, stuck, gunKickPitch, gunKickYaw));
    }

}
