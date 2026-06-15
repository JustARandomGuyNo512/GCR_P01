package com.sheridan.gcr.client.render.events;

import com.sheridan.gcr.client.screen.ldlib2Remake.GunModifyScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;


public class GuiEvents {
    @SubscribeEvent
    public static void onRenderInventoryTab(RenderGuiLayerEvent.Pre event) {
        ResourceLocation id = event.getName();
        if (id.equals(VanillaGuiLayers.HOTBAR) || id.equals(VanillaGuiLayers.EXPERIENCE_BAR) || id.equals(VanillaGuiLayers.EXPERIENCE_LEVEL) ||
                id.equals(VanillaGuiLayers.PLAYER_HEALTH) || id.equals(VanillaGuiLayers.FOOD_LEVEL)) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof GunModifyScreen) {
                event.setCanceled(true);
            }
        }
    }
}
