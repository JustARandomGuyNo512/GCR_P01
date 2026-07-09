package com.sheridan.gcr.modularSys.fire.closedBolt;

import com.sheridan.gcr.Utils;
import com.sheridan.gcr.client.SprintingHandler;
import com.sheridan.gcr.modularSys.fire.FireMode;
import com.sheridan.gcr.modularSys.fire.IFireMode;
import com.sheridan.gcr.modularSys.modules.IAmmoSource;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.guns.ar.AR;
import com.sheridan.gcr.modularSys.task.GunTaskHandler;
import com.sheridan.gcr.modularSys.task.IGunTask;
import com.sheridan.gcr.network.c2s.GunFirePacket;
import com.sheridan.gcr.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.awt.*;
import java.util.Map;

public abstract class ARFireMode extends FireMode<AR> {
    public ARFireMode(String name) {
        super(name);
    }

    @Override
    public int modifyRpm(int baseRpm) {
        return baseRpm;
    }

    boolean stuckMsgNoticed = false;
    boolean removeStuckTaskSent = false;
    @OnlyIn(Dist.CLIENT)
    @Override
    public FireControl clientIntentToFire(Player player, ItemStack stack, AR gun) {
        String identityID = gun.getIdentityID(stack);
        if (IGun.NONE.equals(identityID)) {
            player.sendSystemMessage(Component.literal("server data not synced"));
            return FireControl.EXIT_FIRE_STATE;
        }
        SprintingHandler.INSTANCE.exitSprinting(Utils.secondToTick(1.3f));
        if (SprintingHandler.INSTANCE.getSprintingProgress() != 0) {
            return FireControl.CANCEL_FIRE;
        }
        if (!GunTaskHandler.INSTANCE.blockShoot()) {//有任务在执行
            boolean stuck = gun.isStuck(stack);
            if (stuck) {
                if (!stuckMsgNoticed) {
                    Minecraft.getInstance().gui.setOverlayMessage(
                            Component.translatable("gcr.overlay.stuck")
                                    .setStyle(Style.EMPTY.withColor(Color.RED.getRGB())), false);
                    stuckMsgNoticed = true;
                    return FireControl.EXIT_FIRE_STATE;
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
        return FireControl.EXIT_FIRE_STATE;
    }

    @Override
    public void triggerServerShoot(Player player, ItemStack stack, AR gun, GunFirePacket packet) {
        boolean stuck = Math.random() < gun.getFaultRate(stack);
        if (useAmmo(player, stack, gun, stuck)) {
            gun.serverShoot(player, stack, packet.shootId, packet);
        }
    }

    protected boolean useAmmoClient(Player player, ItemStack stack, AR gun) {
        boolean ammoUsed = useAmmo(player, stack, gun, false);
        boolean stuck = gun.isStuck(stack);
        if (stuck) {
            IFireMode.stopFire();
        }
        return ammoUsed;
    }

    protected boolean useAmmo(Player player, ItemStack stack, AR gun, @Deprecated boolean handleStuck) {
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

    protected void useMagAmmo(ItemStack itemStack, AR gun, Player player, boolean handleStuck) {
        IAmmoSource mag = gun.getMagAttachment(itemStack);
        CompoundTag gunStates = gun.rootNodeTag(itemStack);
        if (mag == null) {
            if (handleStuck) {
                gun.setStuck(true, gunStates);
                ModSounds.sound(1, 1, player, ModSounds.GUN_STUCK.get());
                if (player.level().isClientSide) {
                    Minecraft.getInstance().gui.setOverlayMessage(
                            Component.translatable("gcr.overlay.stuck")
                                    .setStyle(Style.EMPTY.withColor(Color.RED.getRGB())), false);
                }

            }
            return;
        }
        CompoundTag magStates = gun.getAmmoSourceTag(itemStack);
        if (mag.getAmmoLeft(magStates) <= 0) {
            gun.setBoltLocked(true, gunStates);
            return;
        }
        if (handleStuck) {
            gun.setStuck(true, gunStates);
            ModSounds.sound(1, 1, player, ModSounds.GUN_STUCK.get());
            if (player.level().isClientSide) {
                Minecraft.getInstance().gui.setOverlayMessage(
                        Component.translatable("gcr.overlay.stuck")
                                .setStyle(Style.EMPTY.withColor(Color.RED.getRGB())), false);
            }
            return;
        }
        int magAmmoLeft = mag.getAmmoLeft(magStates);
        gun.setGunAmmoLeft(itemStack, 1);
        mag.setAmmoLeft(magAmmoLeft - 1, magStates);
    }

    @Override
    public Class<AR> getGunClass() {
        return AR.class;
    }
}
