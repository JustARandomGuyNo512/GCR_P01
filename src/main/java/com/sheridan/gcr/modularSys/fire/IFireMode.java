package com.sheridan.gcr.modularSys.fire;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.c2s.GunFirePacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface IFireMode<T extends IGun> {

    /**
     * 理论射速 → 每一发的精确间隔（纳秒）。
     *
     * <p>开火时间轴直接按纳秒计时：{@code 60_000_000_000 / rpm}。旧的 {@link #rpmToDelay(int)}
     * 会把间隔量化成整数个 5ms 步进（700 RPM 只能取 85ms 或 90ms），持续射击的实际射速
     * 永远对不上理论值；纳秒间隔配合「下一发 = 上一发计划时刻 + 间隔」就没有这个问题。</p>
     *
     * @return 间隔纳秒；{@code rpm <= 0} 时返回 0（调用方应视为不可用）
     */
    static long rpmToIntervalNanos(int rpm) {
        if (rpm <= 0) {
            return 0L;
        }
        return 60_000_000_000L / rpm;
    }

    /**
     * @deprecated 把射速量化成整数个 5ms 步进的旧算法，只保留给还在读「冷却步数」的旧调用方；
     *             开火计时请用 {@link #rpmToIntervalNanos(int)}。
     */
    @Deprecated
    static int rpmToDelay(int rpm) {
        return 60000 / rpm / 5;
    }

    enum FireControl {
        CANCEL_FIRE,
        EXIT_FIRE_STATE,
        ALLOW_FIRE
    }

    /**
     * 根据枪械的基础RPM，计算并返回此模式下的最终RPM。
     * @param baseRpm 从枪械模块（枪管等）计算出的基础射速
     * @return 该开火模式下的有效射速
     */
    int modifyRpm(int baseRpm);

    @OnlyIn(Dist.CLIENT)
    FireControl clientIntentToFire(Player player, ItemStack stack, T gun);

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
