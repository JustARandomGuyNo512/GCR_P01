package com.sheridan.gcr.modularSys.fire;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.c2s.GunFirePacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface IFireMode<T extends IGun> {

    static int rpmToDelay(int rpm) {
        return 60000 / rpm / 5;
    }

    /**
     * 根据枪械的基础RPM，计算并返回此模式下的最终RPM。
     * @param baseRpm 从枪械模块（枪管等）计算出的基础射速
     * @return 该开火模式下的有效射速
     */
    int modifyRpm(int baseRpm);

    @OnlyIn(Dist.CLIENT)
    boolean clientIntentToFire(Player player, ItemStack stack, T gun);

    @OnlyIn(Dist.CLIENT)
    void triggerClientShoot(Player player, ItemStack stack, T gun);

    void triggerServerShoot(Player player, ItemStack stack, T gun, GunFirePacket packet);

    String getName();

    Class<T> getGunClass();

    static void stopFire() {
        Client.WEAPON_STATUS.fireCount = 0;
        Client.LEFT_BUTTON_PRESSED.set(false);
    }

}
