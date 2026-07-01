package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.GunEffect;
import com.sheridan.gcr.client.GunEffectManager;
import com.sheridan.gcr.client.KeyBinds;
import com.sheridan.gcr.client.SprintingHandler;
import com.sheridan.gcr.client.recoil.IRecoilUpdater;
import com.sheridan.gcr.client.recoil.RecoilHandler;
import com.sheridan.gcr.entity.ModEntities;
import com.sheridan.gcr.entity.projectile.GrenadeEntity;
import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.*;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.views.IM203View;
import com.sheridan.gcr.modularSys.task.GunTaskHandler;
import com.sheridan.gcr.modularSys.task.other.CheckingTask;
import com.sheridan.gcr.network.c2s.SubWeaponFirePacket;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class M203 extends SubWeapon implements IVoxelHandlerModule, IArmHandlerModular, IStateModular, IM203View {
    private final IVoxelHandler voxelHandler;
    private final AdditionalPropModifier modifier;


    protected float reloadLengthInSeconds;

    protected float impulseZ;
    protected float impulsePitch;
    protected float impulseYaw;
    protected float impulseRoll;
    protected float spread;
    protected float velocity;
    protected float explodeRadius;


    public M203(ResourceLocation id, float weight, IVoxelHandler voxelHandler, AdditionalPropModifier modifier,
                float reloadLengthInSeconds, float impulseZ, float impulsePitch, float impulseYaw, float impulseRoll,
                float spread, float velocity, float explodeRadius, int safeTicks) {
        super(id, true, weight, Direction.NONE);
        this.voxelHandler = voxelHandler;
        this.modifier = modifier;
        this.reloadLengthInSeconds = reloadLengthInSeconds;
        this.impulseZ = impulseZ;
        this.impulsePitch = impulsePitch;
        this.impulseYaw = impulseYaw;
        this.impulseRoll = impulseRoll;
        this.spread = spread;
        this.velocity = velocity;
        this.explodeRadius = explodeRadius;
    }

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
    public void onUpdate(StatesUpdateContext context) {}

    @Override
    public String getChamberStatus(CompoundTag states) {
        return CHAMBER_STATUS.get(states);
    }


    @OnlyIn(Dist.CLIENT)
    private boolean shouldNotHandleShoot() {
        return SprintingHandler.INSTANCE.isSprinting() ||
                GunTaskHandler.INSTANCE.hasTask();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void onKeyPressed(int keyCode, int action, String thisNodeId, Unit unit, IGun gun, ItemStack itemStack) {
        super.onKeyPressed(keyCode, action, thisNodeId, unit, gun, itemStack);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || action != 1) {
            return;
        }
        if (keyCode == KeyBinds.USE_GRENADE_LAUNCHER.getKey().getValue()) {
            if (shouldNotHandleShoot()) {
                return;
            }
            CompoundTag states = gun.getNodeStatesTag(itemStack, thisNodeId);
            String chamberStatus = getChamberStatus(states);
            //if (!CHAMBER_LOADED.equals(chamberStatus)) {
            //    handleReload();
            //} else {
            String identityID = gun.getIdentityID(itemStack);
            clientShoot(thisNodeId, identityID);
            //}
        } else if (keyCode == KeyBinds.CHECK_SUB_WEAPON.getKey().getValue()) {
            if (!Client.isAiming()) {
                CheckingTask task = new CheckingTask(itemStack, gun, CheckingTask.CHECK_SUB_WEAPON, Map.of(
                        "animation_name", "check_grenade"
                ));
                GunTaskHandler.INSTANCE.setTask(task);
            }
        }
    }

    public void serverShoot(SubWeaponFirePacket packet, ItemStack itemStack, ServerPlayer player) {
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        pitch += packet.gunKickPitch;
        yaw += packet.gunKickYaw;

        Level level = player.level();

        GrenadeEntity grenade = new GrenadeEntity(ModEntities.GRENADE.get(), level);
        grenade.shootFromRotation(player, pitch, yaw, 0.0F, velocity, spread, explodeRadius);
        level.addFreshEntity(grenade);
        int latency = player.connection.latency();
        ModSounds.sound(3F, (float) (0.9f + Math.random() * 0.1f), player, ModSounds.M203_FIRE.get());
        // 广播开火事件
        BroadcastLivingFirePacket firePacket = new BroadcastLivingFirePacket(
                player.getId(),
                packet.nodeId,
                packet.gunId,
                latency
        );
        PacketDistributor.sendToPlayersTrackingEntity(
                player,
                firePacket
        );
    }

    @OnlyIn(Dist.CLIENT)
    protected void handleReload() {

    }

    @OnlyIn(Dist.CLIENT)
    protected void clientShoot(String nodeId, String gunId) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        IRecoilUpdater recoilUpdater = RecoilHandler.INSTANCE.getRecoilUpdater();
        recoilUpdater.applyImpulse(-impulseZ, impulsePitch, impulseYaw, 0,0, impulseRoll);
        SoundEvent soundEvent = ModSounds.M203_FIRE.get();
        ModSounds.sound(1, 1, player, soundEvent);
        GunEffectManager.updateEffectTimestamp(player.getId(), GunEffect.SHOOT, nodeId, System.currentTimeMillis());
        Client.WEAPON_STATUS.lastShoot = System.currentTimeMillis();
        float gunKickPitch = recoilUpdater.getGunKickPitch();
        float gunKickYaw = recoilUpdater.getGunKickYaw();
        PacketDistributor.sendToServer(new SubWeaponFirePacket(gunId, nodeId, gunKickPitch, gunKickYaw));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        AdditionalPropModifier modifier = getModifier();
        if (modifier != null) {
            modifier.appendHoverText(tooltipComponents);
        }
    }
}