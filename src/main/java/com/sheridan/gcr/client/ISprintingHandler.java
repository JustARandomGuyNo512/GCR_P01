package com.sheridan.gcr.client;

import net.minecraft.client.player.LocalPlayer;

/**
 * for class hot reload debug not functional!
 * */
public interface ISprintingHandler {
    void tick(LocalPlayer player);

    boolean isSprinting();

    float getSprintingProgress(float partialTicks);

    float getSprintingProgress();

    void exitSprinting(int coolDownTicks);
}
