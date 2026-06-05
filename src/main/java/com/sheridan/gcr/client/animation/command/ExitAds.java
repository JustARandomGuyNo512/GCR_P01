package com.sheridan.gcr.client.animation.command;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExitAds extends Command{
    public ExitAds(String command, float timeStamp) {
        super(command, timeStamp);
    }
}
