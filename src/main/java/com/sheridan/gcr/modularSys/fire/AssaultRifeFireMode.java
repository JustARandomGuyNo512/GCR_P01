package com.sheridan.gcr.modularSys.fire;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.client.stuck.ClientGunStuckCache;
import com.sheridan.gcr.modularSys.modules.guns.SlottedGunMainPart;
import com.sheridan.gcr.modularSys.task.GunTaskHandler;
import com.sheridan.gcr.modularSys.task.IGunTask;
import com.sheridan.gcr.network.c2s.GunFirePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.awt.*;
import java.util.Map;

/**
 * 突击步枪（AR / AK 系）基础开火模式。
 *
 * <h2>卡壳判定放在客户端</h2>
 * 卡壳由客户端在开火那一刻掷骰子决定，并随 {@link GunFirePacket} 上报给服务端；服务端只做
 * 合法性校验后采信。这么做解决两个服务端判定解决不了的问题：
 * <ol>
 *   <li><b>动画命中</b>：卡壳必须在 {@code SHOOT} 事件派发之前就知道，动画控制器
 *       {@code view.stuck(states)} 才能选中 {@code shoot_stuck}。服务端下发状态时射击动画
 *       早就播完了。</li>
 *   <li><b>幻影射击</b>：服务端判定时，客户端会先按“打得出去”处理并连发多颗；服务端实际
 *       卡壳的那几发根本没打出来。客户端判定后双方共用同一个结果，弹药与卡壳认知一致。</li>
 * </ol>
 */
public abstract class AssaultRifeFireMode<T extends SlottedGunMainPart> extends FireMode<T> {
    public AssaultRifeFireMode(String name) {
        super(name);
    }


    @OnlyIn(Dist.CLIENT)
    @Override
    public FireControl onClientIntentToFire(Player player, ItemStack stack, T gun) {
        // 先把缓存里的卡壳投影回物品 states：容器同步可能把本地写入冲掉，
        // 而下面任务系统里 getTask(REMOVE_STUCK) 读的是 states，不是缓存。
        ClientGunStuckCache.get().projectToNbt(stack, gun);
        boolean stuck = ClientGunStuckCache.get().isStuck(stack, gun);
        if (stuck) {
            if (!GunTaskHandler.INSTANCE.hasTask()) {
                IGunTask<?> task = gun.getTask(stack, IGunTask.TaskType.REMOVE_STUCK, Map.of());
                if (task != null) {
                    GunTaskHandler.INSTANCE.setTask(task);
                }
            }

            return FireControl.EXIT_FIRE_STATE;
        }
        //removeStuckTaskSent = false;
        int ammoLeft = gun.getGunAmmoLeft(stack);
        return ammoLeft > 0 ? FireControl.ALLOW_FIRE : FireControl.EXIT_FIRE_STATE;
    }

    @Override
    public void triggerServerShoot(Player player, ItemStack stack, T gun, GunFirePacket packet) {
        boolean stuck = packet.stuck;
        if (useAmmo(player, stack, gun, stuck)) {
            gun.serverShoot(player, stack, packet.shootId, packet);
        } else if (player instanceof ServerPlayer serverPlayer) {
            // 这一发没打出去（服务端已经卡壳 / 膛内没弹）。必须回执，否则客户端的本地预测
            // 只能等超时自愈；回执同时把服务端真实的卡壳状态带回去，两边立刻收敛。
            gun.serverShootRejected(serverPlayer, stack, packet.shootId);
        }
    }

    /**
     * 当前这一发的最终卡壳概率：基础故障率随枪管热量在 {@code stuckRate ~ maxStuckRate} 之间插值，
     * 热量按 {@code heat_stuck_ratio} 放大后取三次方作为插值系数。
     */
    protected float stuckRate(Player player, ItemStack stack, T gun) {
        float stuckRate = gun.getStuckRate(stack);
        float maxStuckRate = gun.getMaxStuckRate(stack);
        float currHeat = gun.getCurrHeat(stack, player.level().getGameTime());
        float heatStuckRatio = gun.getHeatStuckRatio(stack);
        currHeat *= heatStuckRatio;
        currHeat = Mth.clamp(currHeat, 0, 1);
        currHeat = (float) Math.pow(currHeat, 3);
        return Mth.clamp(Mth.lerp(currHeat, stuckRate, maxStuckRate), 0f, 1f);
    }

    // =====================================================================
    // 客户端开火
    // =====================================================================

    @OnlyIn(Dist.CLIENT)
    protected enum ClientShot {
        /** 这一发根本没打出去（没有膛内弹等）。 */
        NONE,
        /** 正常击发。 */
        FIRED,
        /** 卡壳，射击循环应当停止。 */
        JAMMED
    }

    /**
     * 客户端开火的公共流程：本地判定卡壳 → 扣弹 → 登记预测 → 发包 → 播放射击反馈。
     *
     * <p>服务端会用同一个卡壳结果执行同一套 {@link #useAmmo}，因此两边对弹药的处理完全一致。</p>
     */
    @OnlyIn(Dist.CLIENT)
    protected ClientShot clientShoot(Player player, ItemStack stack, T gun) {
        boolean rollStuck = rollStuckClient(player, stack, gun);
        if (!useAmmo(player, stack, gun, rollStuck)) {
            return ClientShot.NONE;
        }
        // 以物品 states 的最终结果为准：useMagAmmo 在“没装弹匣”等情况下可能不会真的置位卡壳，
        // 上报给服务端的必须是这一发真实发生的事，双方才会完全一致。
        boolean stuck = gun.isStuck(stack);
        // 最后一发的判定也在这里算好：它依赖的膛内弹数与弹匣信息在这一刻是确定值，
        // 交给渲染线程回读 states 会撞上同一个跨线程可见性问题。
        boolean lastRound = !stuck && gun.getGunAmmoLeft(stack) == 0 && gun.getMagAttachment(stack) != null;
        String gunId = gun.getIdentityID(stack);
        int shootId = nextShootId();
        if (stuck) {
            // 必须在发包之前登记预测：回执可能瞬间返回，先登记才能保证配对成功。
            ClientGunStuckCache.get().onLocalJam(gunId, shootId);
        }
        sendPacket(shootId, stuck, gunId);
        gun.clientShoot(player, stack);
        Client.WEAPON_STATUS.onShoot();
        // 卡壳与最后一发直接作为事件参数下发：开火线程与渲染线程不同，物品 NBT 是 HashMap，
        // 渲染线程回读可能延迟，导致 shoot_stuck / shoot_last 选不中。
        Client.getGunRenderer().dispatchAnimationEvent(EventType.SHOOT, Map.of(
                EventType.PARAM_STUCK, Boolean.toString(stuck),
                EventType.PARAM_LAST_ROUND, Boolean.toString(lastRound)
        ));
        Minecraft.getInstance().gui.setOverlayMessage(
                Component.translatable("gcr.overlay.stuck")
                        .setStyle(Style.EMPTY.withColor(Color.RED.getRGB())), false);
        return stuck ? ClientShot.JAMMED : ClientShot.FIRED;
    }

    /** 客户端这一发是否卡壳：概率为 1 必定卡壳、为 0 不可能卡壳，其余按概率掷骰子。 */
    @OnlyIn(Dist.CLIENT)
    protected boolean rollStuckClient(Player player, ItemStack stack, T gun) {
        float rate = stuckRate(player, stack, gun);
        if (rate <= 0f) {
            return false;
        }
        if (rate >= 1f) {
            return true;
        }
        return Math.random() < rate;
    }

    protected boolean useAmmo(Player player, ItemStack stack, T gun, boolean handleStuck) {
        boolean stuck = gun.isStuck(stack);
        if (stuck) {
            return false;
        }
        int ammoLeft = gun.getGunAmmoLeft(stack);
        if (ammoLeft > 0) {
            gun.setGunAmmoLeft(stack, 0);
            useMagAmmo(stack, gun, player, handleStuck);
            gun.notifyDataChanged(stack);
            return true;
        }
        return false;
    }

    protected abstract void useMagAmmo(ItemStack itemStack, T gun, Player player, boolean handleStuck);


    @OnlyIn(Dist.CLIENT)
    public void triggerClientShootSemi(Player player, ItemStack stack, T gun) {
        if (clientShoot(player, stack, gun) != ClientShot.NONE) {
            IFireMode.stopFire();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void triggerClientShootFullAuto(Player player, ItemStack stack, T gun) {
        ClientShot shot = clientShoot(player, stack, gun);
        if (shot == ClientShot.JAMMED) {
            IFireMode.stopFire();
        } else if (shot == ClientShot.FIRED) {
            Client.WEAPON_STATUS.fireCount++;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void triggerClientShootBurst(Player player, ItemStack stack, T gun, int burstCount) {
        ClientShot shot = clientShoot(player, stack, gun);
        if (shot == ClientShot.JAMMED) {
            IFireMode.stopFire();
        } else if (shot == ClientShot.FIRED) {
            Client.WEAPON_STATUS.fireCount++;
            if (Client.WEAPON_STATUS.fireCount >= burstCount) {
                IFireMode.stopFire();
            }
        }
    }
}
