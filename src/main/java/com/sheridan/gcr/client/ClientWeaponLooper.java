package com.sheridan.gcr.client;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.TimerTask;

@OnlyIn(Dist.CLIENT)
public class ClientWeaponLooper extends TimerTask {
    int mainHandDelay;
    @Override
    public void run() {
        try {
            work();
        } catch (Exception ignored){}
    }

    private void work() {
        if (Client.CANCEL_WEAPON_LOOPER.get()) {
            return;
        }
        if (mainHandDelay <= 0 && Client.LEFT_BUTTON_PRESSED.get()) {
            postShootTask();
        }
        handleCoolDown();
    }

    private void postShootTask() {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof GunItem gunItem) {
                IGun gun = gunItem.getGun();
                if (gun == null) {
                    return;
                }
                mainHandDelay = Client.handleClientShoot(stack, gun, player);
            }
        }
    }

    private void handleCoolDown() {
        mainHandDelay = mainHandDelay > 0 ? mainHandDelay - 1 : mainHandDelay;
    }
}
