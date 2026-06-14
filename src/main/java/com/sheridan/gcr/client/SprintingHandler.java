package com.sheridan.gcr.client;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.task.GunTaskHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SprintingHandler implements ISprintingHandler{
    public static SprintingHandler INSTANCE = new SprintingHandler();
    private float sprintingProgress;
    private float lastSprintingProgress;
    private float exitSpeed;
    private boolean inSprinting;
    private int sprintingCoolDown;

    public static void _debugReloadInstance(ISprintingHandler sprintingHandler) {
        if (!GCR.IS_DEVELOPMENT) return;
        INSTANCE = (SprintingHandler)sprintingHandler;
    }

    public void tick(LocalPlayer player) {
        if (player != null) {
            if (sprintingCoolDown != 0) {
                sprintingCoolDown = Math.max(0, sprintingCoolDown - 1);
                inSprinting = false;
                lastSprintingProgress = sprintingProgress;
                sprintingProgress = Math.max(0, sprintingProgress - exitSpeed);
                return;
            }
            IGun gun = Client.WEAPON_STATUS.getGun();
            if (gun != null) {
                inSprinting = shouldEnterSprinting(player);
                float weight = Client.WEAPON_STATUS.getWeight();
                float weightFactor = (float) Math.exp(-weight * 0.25f);
                float agility = Client.WEAPON_STATUS.getAgility() * 0.16f;

                exitSpeed = 0.05f + (weightFactor + agility) * 0.1f * (gun.isPistol() ? 1.25f : 1f);

                exitSpeed = Mth.clamp(exitSpeed, 0.05f, 0.25f);
                if (inSprinting) {
                    lastSprintingProgress = sprintingProgress;
                    sprintingProgress = Math.min(1, sprintingProgress + exitSpeed);
                } else {
                    lastSprintingProgress = sprintingProgress;
                    sprintingProgress = Math.max(0, sprintingProgress - exitSpeed);
                }
            } else {
                exitSprinting(20);
            }
        } else {
            sprintingProgress = 0;
            inSprinting = false;
        }
    }

    public boolean isSprinting() {
        return inSprinting;
    }

    private boolean shouldEnterSprinting(LocalPlayer player) {
        if (!player.isSprinting()) {
            return false;
        }
        boolean allowSprinting = GunTaskHandler.INSTANCE.allowSprinting();
        if (!allowSprinting || Client.isAiming()) {
            exitSprinting(20);
        }
        return sprintingCoolDown == 0 && !player.getAbilities().flying && !player.isCrouching() &&
                allowSprinting && !Client.isAiming();
    }

    public void exitSprinting(int coolDownTicks)  {
        inSprinting = false;
        sprintingCoolDown = coolDownTicks;
    }

    public float getSprintingProgress() {
        return sprintingProgress;
    }

    public float getSprintingProgress(float particleTick) {
        return Mth.lerp(particleTick, lastSprintingProgress, sprintingProgress);
    }

    public void clear(int coolDown) {
        inSprinting = false;
        sprintingProgress = 0;
        lastSprintingProgress = 0;
        sprintingCoolDown = coolDown;
    }
}

