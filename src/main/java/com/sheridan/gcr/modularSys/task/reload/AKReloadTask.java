package com.sheridan.gcr.modularSys.task.reload;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.animation.AnimationVariants;
import com.sheridan.gcr.client.animation.command.MaskMagAmmo;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.modularSys.modules.IAmmoSource;
import com.sheridan.gcr.modularSys.modules.guns.ak.AK;
import com.sheridan.gcr.modularSys.task.GunTask;
import com.sheridan.gcr.modularSys.task.IGunTask;
import com.sheridan.gcr.network.c2s.GunReloadPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.awt.*;
import java.util.Map;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class AKReloadTask extends GunTask<AK> {
    private static final String GUN_PREFIX = "ak74m";
    private String category;
    private String animationName;
    private IAmmoSource magAttachment;
    public int sendPacketDelay;

    public AKReloadTask(ItemStack itemStack, AK gun) {
        super(itemStack, gun);
        CompoundTag states = gun.rootNodeTag(itemStack);
        boolean hasMagAttachment = gun.hasMagAttachment(states);
        if (!hasMagAttachment) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendSystemMessage(Component.translatable("gcr.overlay.require_mag").withColor(Color.RED.getRGB()));
            }
            length = 0;
            return;
        }
        int ammoLeft = gun.getGunAmmoLeft(itemStack);

        if (ammoLeft > 0) {
            category = AnimationVariants.MAG_RELOAD;
        } else {
            category = AnimationVariants.MAG_RELOAD_EMPTY;
        }

        AnimationVariants.Pick pick = AnimationVariants.pick(GUN_PREFIX, category);
        animationName = pick.animationName();
        Map<String, Float> taskTimers = gun.baseProperties.taskTimers;
        this.length = pick.lengthTicks();
        sendPacketDelay = pick.sendPacketDelayTicks(GUN_PREFIX, taskTimers);
        magAttachment = gun.getMagAttachment(itemStack);
    }

    @Override
    public boolean equals(IGunTask<?> other) {
        if (!(other instanceof AKReloadTask otherTask) || !Objects.equals(otherTask.category, this.category)) {
            return false;
        }
        return super.equals(other);
    }

    @Override
    public void start() {
        if (animationName != null) {
            Client.getGunRenderer().dispatchAnimationEvent(EventType.RELOAD, Map.of("animation_name", animationName));
        }
    }

    @Override
    public void onTick(Player player) {
        super.onTick(player);
        if (tick == sendPacketDelay) {
            PacketDistributor.sendToServer(new GunReloadPacket());
        }
    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onComplete() {

    }


    @Override
    public TaskType getType() {
        return TaskType.RELOAD;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    protected int getPredictedAmmoCount() {
        return magAttachment == null ? 1 : magAttachment.getMaxCapacity();
    }

    @Override
    public int getCustomVariable(String variableName) {
        if (MaskMagAmmo.VARIABLE_KEY.equals(variableName)) {
            return getPredictedAmmoCount();
        }
        return -1;
    }
}
