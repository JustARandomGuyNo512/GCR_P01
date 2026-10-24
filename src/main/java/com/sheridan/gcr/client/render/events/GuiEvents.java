package com.sheridan.gcr.client.render.events;

import com.sheridan.gcr.client.screen.ldlib2Remake.GunModifyScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;


public class GuiEvents {
    @SubscribeEvent
    public static void onRenderInventoryTab(RenderGuiOverlayEvent.Pre event) {
        ResourceLocation id = event.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.HOTBAR) || id.equals(VanillaGuiOverlay.EXPERIENCE_BAR) ||
                id.equals(VanillaGuiOverlay.PLAYER_HEALTH) || id.equals(VanillaGuiOverlay.FOOD_LEVEL)) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof GunModifyScreen) {
                event.setCanceled(true);
            }
        }
    }
}
