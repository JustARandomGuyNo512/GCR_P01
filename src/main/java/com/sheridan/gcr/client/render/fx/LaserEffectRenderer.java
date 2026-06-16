package com.sheridan.gcr.client.render.fx;

import com.sheridan.gcr.client.render.delayed.Stage;
import com.sheridan.gcr.client.render.delayed.Task;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@OnlyIn(Dist.CLIENT)
public class LaserEffectRenderer {

    static {
        Stage.HIGH.addTask(new Task(LaserEffectRenderer::renderEffect).forever());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {

    }

    public static void recordEffectCall() {

    }

    public static void renderEffect(float partialTicks) {

    }
}
