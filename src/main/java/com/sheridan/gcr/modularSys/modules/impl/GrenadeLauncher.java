package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.GunEffect;
import com.sheridan.gcr.client.GunEffectManager;
import com.sheridan.gcr.client.KeyBinds;
import com.sheridan.gcr.client.SprintingHandler;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.client.recoil.IRecoilUpdater;
import com.sheridan.gcr.client.recoil.RecoilHandler;
import com.sheridan.gcr.entity.ModEntities;
import com.sheridan.gcr.entity.projectile.GrenadeEntity;
import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.*;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.views.IGrenadeLauncherView;
import com.sheridan.gcr.modularSys.task.GunTaskHandler;
import com.sheridan.gcr.modularSys.task.other.CheckingTask;
import com.sheridan.gcr.modularSys.task.reload.SubWeaponReloadTask;
import com.sheridan.gcr.network.c2s.SubWeaponFirePacket;
import com.sheridan.gcr.network.c2s.SubWeaponReloadPacket;
import com.sheridan.gcr.network.s2c.BroadcastLivingFirePacket;
import com.sheridan.gcr.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 挂载式榴弹发射器（枪管下挂副武器）的公共实现。
 *
 * <p>M203 与 GP25 的全部行为都是共享的，只有四件事随型号变化，由子类提供：</p>
 * <ul>
 *     <li>{@link #getFireSound()} —— 客户端与服务端开火音效；</li>
 *     <li>{@link #getProjectileModelType()} —— 生成的 {@link GrenadeEntity} 外观；</li>
 *     <li>{@link #getChamberStatusAfterFire()} —— 击发后的弹膛状态
 *         （M203 是 {@code fired}，GP25 直接回到 {@code empty}）；</li>
 *     <li>{@link #getCheckAnimationName()} —— 检视动画名。</li>
 * </ul>
 *
 * <p>子类只需要声明自己实现的型号视图接口，其余（按键、装填、开火、广播、tooltip）全部继承。</p>
 */
public abstract class GrenadeLauncher extends SubWeapon
        implements IVoxelHandlerModule, IArmHandlerModular, IStateModular, IGrenadeLauncherView {

    /** 检视（check）动作的默认动画名。 */
    public static final String DEFAULT_CHECK_ANIMATION = "check_grenade";

    private final IVoxelHandler voxelHandler;
    private final AdditionalPropModifier modifier;

    protected final int reloadLength;
    protected final int reloadSendPacketDelay;

    protected final float impulseZ;
    protected final float impulsePitch;
    protected final float impulseYaw;
    protected final float impulseRoll;
    protected final float spread;
    protected final float velocity;
    protected final float explodeRadius;

    public GrenadeLauncher(ResourceLocation id, float weight, IVoxelHandler voxelHandler, AdditionalPropModifier modifier,
                           float reloadLengthInSeconds, float reloadSendPacketDelayInSeconds,
                           float impulseZ, float impulsePitch, float impulseYaw, float impulseRoll,
                           float spread, float velocity, float explodeRadius) {
        super(id, true, weight, Direction.NONE);
        this.voxelHandler = voxelHandler;
        this.modifier = modifier;
        this.reloadLength = (int) (reloadLengthInSeconds * 20);
        this.reloadSendPacketDelay = (int) (reloadSendPacketDelayInSeconds * 20);
        this.impulseZ = impulseZ;
        this.impulsePitch = impulsePitch;
        this.impulseYaw = impulseYaw;
        this.impulseRoll = impulseRoll;
        this.spread = spread;
        this.velocity = velocity;
        this.explodeRadius = explodeRadius;
    }

    // ==================== 型号差异 ====================

    /** 开火音效（客户端与服务端共用）。 */
    protected abstract SoundEvent getFireSound();

    /** 生成的榴弹实体外观类型。 */
    protected abstract int getProjectileModelType();

    /**
     * 击发后写入弹膛 states 的状态。
     *
     * @see IGrenadeLauncherView#CHAMBER_EMPTY
     * @see IGrenadeLauncherView#CHAMBER_LOADED
     */
    protected abstract String getChamberStatusAfterFire();

    /** 检视动作使用的动画名，对应状态视图/控制器里注册的检视动画。 */
    protected String getCheckAnimationName() {
        return DEFAULT_CHECK_ANIMATION;
    }

    // ==================== 模块接口 ====================

    @Override
    public IVoxelHandler getHandler() {
        return voxelHandler;
    }

    @Override
    public int getPriority(boolean rightArm) {
        return rightArm ? IArmHandlerModular.NONE_PRIORITY : IArmHandlerModular.SUB_WEAPON_PRIORITY;
    }

    @Override
    public @Nullable AdditionalPropModifier getModifier() {
        return modifier;
    }

    @Override
    public void onInitStates(CompoundTag states, String nodeId, String moduleId) {
        CHAMBER_STATUS.init(states);
    }

    @Override
    public void onUpdate(StatesUpdateContext context) {
    }

    @Override
    public String getChamberStatus(CompoundTag states) {
        return CHAMBER_STATUS.get(states);
    }

    // ==================== 服务端 ====================

    @Override
    public void serverReload(SubWeaponReloadPacket packet, ItemStack itemStack, ServerPlayer player, IGun gun, SubWeapon subWeapon) {
        CompoundTag nodeStatesTag = gun.getNodeStatesTag(itemStack, packet.nodeId);
        if (nodeStatesTag == null) {
            return;
        }
        CHAMBER_STATUS.set(CHAMBER_LOADED, nodeStatesTag);
        gun.notifyDataChanged(itemStack);
    }

    @Override
    public void serverShoot(SubWeaponFirePacket packet, ItemStack itemStack, ServerPlayer player, IGun gun, SubWeapon subWeapon) {
        CompoundTag nodeStatesTag = gun.getNodeStatesTag(itemStack, packet.nodeId);
        if (nodeStatesTag == null || !Objects.equals(CHAMBER_STATUS.get(nodeStatesTag), CHAMBER_LOADED)) {
            return;
        }

        float pitch = player.getXRot() + packet.gunKickPitch;
        float yaw = player.getYRot() + packet.gunKickYaw;
        Level level = player.level();

        GrenadeEntity grenade = new GrenadeEntity(ModEntities.GRENADE.get(), level);
        grenade.setModelType(getProjectileModelType());
        grenade.shootFromRotation(player, pitch, yaw, 0.0F, velocity, spread, explodeRadius);
        level.addFreshEntity(grenade);
        CHAMBER_STATUS.set(getChamberStatusAfterFire(), nodeStatesTag);

        int latency = player.connection.latency();
        ModSounds.sound(3F, (float) (0.9f + Math.random() * 0.1f), player, getFireSound());
        // 广播开火事件
        PacketDistributor.sendToPlayersTrackingEntity(player, new BroadcastLivingFirePacket(
                player.getId(),
                packet.nodeId,
                packet.gunId,
                latency
        ));
    }

    // ==================== 客户端 ====================

    @OnlyIn(Dist.CLIENT)
    @Override
    public void onKeyPressed(int keyCode, int action, String thisNodeId, Unit unit, IGun gun, ItemStack itemStack) {
        super.onKeyPressed(keyCode, action, thisNodeId, unit, gun, itemStack);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || action != 1) {
            return;
        }
        if (keyCode == KeyBinds.USE_GRENADE_LAUNCHER.getKey().getValue()) {
            CompoundTag states = gun.getNodeStatesTag(itemStack, thisNodeId);
            if (states == null) {
                return;
            }
            if (!CHAMBER_LOADED.equals(getChamberStatus(states))) {
                handleClientReload(itemStack, gun, thisNodeId, gun.getIdentityID(itemStack));
            } else if (!shouldNotHandleShoot()) {
                clientShoot(thisNodeId, gun.getIdentityID(itemStack), states, itemStack);
            }
        } else if (keyCode == KeyBinds.CHECK_SUB_WEAPON.getKey().getValue()) {
            if (!Client.isAiming()) {
                GunTaskHandler.INSTANCE.setTask(new CheckingTask(itemStack, gun, CheckingTask.CHECK_SUB_WEAPON, Map.of(
                        "animation_name", getCheckAnimationName()
                )));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void handleClientReload(ItemStack itemStack, IGun gun, String nodeId, String gunId) {
        SprintingHandler.INSTANCE.exitSprinting(20);
        clearCheckTrack();
        GunTaskHandler.INSTANCE.setTask(new SubWeaponReloadTask(
                itemStack, gun, reloadLength, reloadSendPacketDelay, nodeId, gunId));
    }

    @OnlyIn(Dist.CLIENT)
    protected void clientShoot(String nodeId, String gunId, CompoundTag states, ItemStack itemStack) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        IRecoilUpdater recoilUpdater = RecoilHandler.INSTANCE.getRecoilUpdater();
        recoilUpdater.applyImpulse(-impulseZ, impulsePitch, impulseYaw, 0, 0, impulseRoll);
        ModSounds.sound(1, 1, player, getFireSound());
        GunEffectManager.updateEffectTimestamp(player.getId(), GunEffect.SHOOT, nodeId, System.currentTimeMillis());
        Client.WEAPON_STATUS.lastShoot = System.nanoTime();
        PacketDistributor.sendToServer(new SubWeaponFirePacket(
                gunId, nodeId, recoilUpdater.getGunKickPitch(), recoilUpdater.getGunKickYaw()));
        clearCheckTrack();
        CHAMBER_STATUS.set(getChamberStatusAfterFire(), states);
    }

    /** 开火/装填/检视前退出疾跑，并判断当前是否不该执行开火。 */
    @OnlyIn(Dist.CLIENT)
    protected boolean shouldNotHandleShoot() {
        SprintingHandler.INSTANCE.exitSprinting(20);
        return SprintingHandler.INSTANCE.getSprintingProgress() != 0 || GunTaskHandler.INSTANCE.hasTask();
    }

    @OnlyIn(Dist.CLIENT)
    private void clearCheckTrack() {
        Client.getGunRenderer().dispatchAnimationEvent(EventType.CLEAR_TRACK, Map.of("name", "check"));
    }

    // ==================== Tooltip ====================

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        AdditionalPropModifier modifier = getModifier();
        if (modifier != null) {
            modifier.appendHoverText(tooltipComponents);
        }
    }
}
