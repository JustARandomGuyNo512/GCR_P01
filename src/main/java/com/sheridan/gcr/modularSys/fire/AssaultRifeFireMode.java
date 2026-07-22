package com.sheridan.gcr.modularSys.fire;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.modularSys.modules.IAmmoSource;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.guns.SlottedGunMainPart;
import com.sheridan.gcr.modularSys.modules.guns.ak.AK;
import com.sheridan.gcr.modularSys.modules.guns.ar.AR;
import com.sheridan.gcr.modularSys.task.GunTaskHandler;
import com.sheridan.gcr.modularSys.task.IGunTask;
import com.sheridan.gcr.network.c2s.GunFirePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.awt.*;
import java.util.Map;

public abstract class AssaultRifeFireMode<T extends SlottedGunMainPart> extends FireMode<T> {
    public AssaultRifeFireMode(String name) {
        super(name);
    }

    boolean stuckMsgNoticed = false;
    boolean removeStuckTaskSent = false;
    @OnlyIn(Dist.CLIENT)
    @Override
    public FireControl onClientIntentToFire(Player player, ItemStack stack, T gun) {
        boolean stuck = gun.isStuck(stack);
        if (stuck) {
            if (!stuckMsgNoticed) {
                Minecraft.getInstance().gui.setOverlayMessage(
                        Component.translatable("gcr.overlay.stuck")
                                .setStyle(Style.EMPTY.withColor(Color.RED.getRGB())), false);
                stuckMsgNoticed = true;
                if (Client.WEAPON_STATUS.fireCount > 0) {
                    return FireControl.EXIT_FIRE_STATE;
                }
            }
            if (!removeStuckTaskSent) {
                IGunTask<?> task = gun.getTask(stack, IGunTask.TaskType.REMOVE_STUCK, Map.of());
                if (task != null) {
                    GunTaskHandler.INSTANCE.setTask(task);
                }
                removeStuckTaskSent = true;
            }
            return FireControl.EXIT_FIRE_STATE;
        }
        stuckMsgNoticed = false;
        removeStuckTaskSent = false;
        int ammoLeft = gun.getGunAmmoLeft(stack);
        return ammoLeft > 0 ? FireControl.ALLOW_FIRE : FireControl.EXIT_FIRE_STATE;
    }

    @Override
    public void triggerServerShoot(Player player, ItemStack stack, T gun, GunFirePacket packet) {
        boolean stuck = shouldStuck(player, stack, gun, packet);
        if (useAmmo(player, stack, gun, stuck)) {
            gun.serverShoot(player, stack, packet.shootId, packet);
        }
    }

    protected boolean shouldStuck(Player player, ItemStack stack, T gun, GunFirePacket packet) {
        float stuckRate = gun.getStuckRate(stack);
        float maxStuckRate = gun.getMaxStuckRate(stack);
        float currHeat = gun.getCurrHeat(stack, player.level().getGameTime());
        float heatStuckRatio = gun.getHeatStuckRatio(stack);
        currHeat *= heatStuckRatio;
        currHeat = Mth.clamp(currHeat, 0, 1);
        currHeat = (float) Math.pow(currHeat, 3);
        float finalRate = Mth.lerp(currHeat, stuckRate, maxStuckRate);
        return Math.random() < finalRate;
    }

    protected boolean useAmmoClient(Player player, ItemStack stack, T gun) {
        boolean ammoUsed = useAmmo(player, stack, gun, false);
        boolean stuck = gun.isStuck(stack);
        if (stuck) {
            IFireMode.stopFire();
        }
        return ammoUsed;
    }

    protected boolean useAmmo(Player player, ItemStack stack, T gun, @Deprecated boolean handleStuck) {
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
        if (useAmmoClient(player, stack, gun)) {
            gun.clientShoot(player, stack);
            sendPacket();
            IFireMode.stopFire();
            Client.WEAPON_STATUS.onShoot();
            Client.getGunRenderer().dispatchAnimationEvent(EventType.SHOOT);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void triggerClientShootFullAuto(Player player, ItemStack stack, T gun) {
        if (useAmmoClient(player, stack, gun)) {
            boolean stuck = gun.isStuck(stack);
            gun.clientShoot(player, stack);
            sendPacket();
            if (stuck) {
                IFireMode.stopFire();
            } else {
                Client.WEAPON_STATUS.fireCount++;
            }
            Client.WEAPON_STATUS.onShoot();
            Client.getGunRenderer().dispatchAnimationEvent(EventType.SHOOT);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void triggerClientShootBurst(Player player, ItemStack stack, T gun, int burstCount) {
        if (useAmmoClient(player, stack, gun)) {
            boolean stuck = gun.isStuck(stack);
            gun.clientShoot(player, stack);
            sendPacket();
            if (stuck) {
                IFireMode.stopFire();
            } else {
                Client.WEAPON_STATUS.fireCount ++;
                if (Client.WEAPON_STATUS.fireCount >= burstCount) {
                    IFireMode.stopFire();
                }
            }
            Client.WEAPON_STATUS.onShoot();
            Client.getGunRenderer().dispatchAnimationEvent(EventType.SHOOT);
        }
    }
}
