package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.fire.IFireMode;
import com.sheridan.gcr.modularSys.fire.AftermatchWallTuck;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.IPacket;
import com.sheridan.gcr.network.s2c.GunFireAckPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.sheridan.gcr.network.GCRNetwork;
import com.sheridan.gcr.network.PacketContext;
import org.jetbrains.annotations.NotNull;

public class GunFirePacket implements IPacket<GunFirePacket> {
    public static final ResourceLocation ID = GCR.RL("gun_fire");


    public int shootId;
    /**
     * 开火时客户端手持枪的 identityID。
     *
     * <p>服务端必须用它校验这一发到底属于哪把枪：玩家快速切枪时，包可能在切枪之后才被处理，
     * 只认「当前主手是不是枪」会把子弹、弹药消耗甚至卡壳记到另一把枪上。</p>
     */
    public String gunId;
    /**
     * 客户端在开火那一刻判定的卡壳结果，服务端只做合法性校验后直接采信。
     *
     * <p>卡壳必须由客户端判定：只有客户端能在 {@code SHOOT} 动画事件派发之前知道结果，
     * 等服务端下发状态时动画早就播完了。而且双方共用同一个判定结果后，对「这一发有没有
     * 打出去、弹药怎么扣」的认知完全一致，不会再出现客户端连发、服务端其实卡壳的幻影射击。</p>
     */
    public boolean stuck;
    public float gunKickPitch;
    public float gunKickYaw;
    /** Additional aim direction derived from first-person model inertia. */
    public float motionPitch;
    public float motionYaw;
    // Strict server-side upper bounds: this packet cannot override arbitrary aim.
    public static final float MAX_MOTION_PITCH = 10f;
    public static final float MAX_MOTION_YAW = 20f;

    public GunFirePacket(int shootId, String gunId, boolean stuck,
                         float gunKickPitch, float gunKickYaw, float motionPitch, float motionYaw) {
        this.shootId = shootId;
        this.gunId = gunId;
        this.stuck = stuck;
        this.gunKickPitch = gunKickPitch;
        this.gunKickYaw = gunKickYaw;
        this.motionPitch = motionPitch;
        this.motionYaw = motionYaw;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(shootId);
        buf.writeUtf(gunId);
        buf.writeBoolean(stuck);
        buf.writeFloat(gunKickPitch);
        buf.writeFloat(gunKickYaw);
        buf.writeFloat(motionPitch);
        buf.writeFloat(motionYaw);
    }

    public static GunFirePacket decode(FriendlyByteBuf buf) {
        return new GunFirePacket(
                buf.readInt(),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    @Override
    public void onClient(GunFirePacket packet, PacketContext context) {

    }

    @Override
    public void onServer(GunFirePacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ItemStack heldItem = player.getMainHandItem();
            if (!(heldItem.getItem() instanceof GunItem gunItem)) {
                reject(player, packet, false);
                return;
            }
            IGun gunModule = gunItem.getGun();
            if (!packet.gunId.equals(gunModule.getIdentityID(heldItem))) {
                // 陈旧的一发：客户端已经换成了别的枪（或这一发根本不属于手上这把）。
                // 绝不能拿它去驱动当前主手武器——否则会凭空消耗另一把枪的弹药、甚至给它挂上卡壳。
                reject(player, packet, false);
                return;
            }
            IFireMode<?> fireMode = gunModule.getFireMode(heldItem);
            if (fireMode == null || !fireMode.getGunClass().isInstance(gunModule)) {
                // 这一发确实属于手上这把枪，但没法处理它：回执必须带上这把枪真实的卡壳状态，
                // 否则客户端会把物品里的卡壳位错误地写成 false。
                reject(player, packet, gunModule.isStuck(heldItem));
                return;
            }
            // Additional client-reported visual inertia is accepted only
            // within the renderer's small physically possible angle envelope.
            if (!Float.isFinite(packet.motionPitch) || !Float.isFinite(packet.motionYaw)
                    || Math.abs(packet.motionPitch) > MAX_MOTION_PITCH
                    || Math.abs(packet.motionYaw) > MAX_MOTION_YAW) {
                reject(player, packet, gunModule.isStuck(heldItem));
                return;
            }
            // Reject BEFORE AssaultRifeFireMode.useAmmo: a wall-blocked shot must not cost a round.
            // Always recalculate on the server; the client cannot bypass the wall with a packet.
            if (AftermatchWallTuck.check(player, 1f).fireBlocked()) {
                reject(player, packet, gunModule.isStuck(heldItem));
                return;
            }
            invokeServerShoot(fireMode, player, heldItem, gunModule, GunFirePacket.this);
        });
    }

    /**
     * 回一个「没有开火」的回执。
     *
     * <p>客户端的本地卡壳预测靠回执配对解除：只要服务端收到过这一发（哪怕最后丢弃），
     * 就必须回执，否则客户端只能等预测超时才敢继续射击，表现为一次莫名其妙的小卡顿。</p>
     *
     * @param stuck 服务端眼中这把枪当前的卡壳状态；身份不匹配时客户端手里不是这把枪，
     *              这个值不会被写进物品数据，给 false 即可（同时也要解除客户端对它的预测）。
     */
    private static void reject(ServerPlayer player, GunFirePacket packet, boolean stuck) {
        GCRNetwork.sendToPlayer(
                player,
                new GunFireAckPacket(packet.gunId, -1, packet.shootId, stuck, false)
        );
    }

    @SuppressWarnings("unchecked")
    private <T extends IGun> void invokeServerShoot(
            IFireMode<T> fireMode, ServerPlayer player, ItemStack stack, IGun gunModule, GunFirePacket packet) {
        fireMode.triggerServerShoot(player, stack, (T) gunModule, packet);
    }}

