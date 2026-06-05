package com.sheridan.gcr.client.screen.ldlib2Remake;

import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class DisplaySlot extends ItemSlot {
    private int count = 0;

    @OnlyIn(Dist.CLIENT)
    @Override
    public void drawContents(@NotNull GUIContext guiContext) {
        ItemStack value = getValue();
        if (!value.isEmpty()) {
            if (count <= 0) {
                guiContext.setElementColor(Color.DARK_GRAY.getRGB());
            }
        }
        super.drawContents(guiContext);
        if (!value.isEmpty()) {
            if (count <= 0) {
                guiContext.graphics.flush();
                guiContext.setElementColor(Color.WHITE.getRGB());
            }
        }
    }

    @Override
    public void drawBackgroundOverlay(@NotNull GUIContext guiContext) {
        super.drawBackgroundOverlay(guiContext);
        if (!getValue().isEmpty()) {
            int color = count > 0 ? Color.GREEN.getRGB() : Color.RED.getRGB();
            float contentX = getContentX();
            float contentY = getContentY();
            guiContext.graphics.pose().pushPose();
            guiContext.graphics.pose().translate(contentX + 12, contentY + 12, 0.0f);
            guiContext.graphics.pose().scale(0.75f, 0.75f, 1.0f);
            String text = count > 99 ? "99+" : Integer.toString(count);
            guiContext.graphics.drawCenteredString(guiContext.mc.font, text, 0, 0, color);
            guiContext.graphics.pose().popPose();
        }
    }

    @Override
    protected void onMouseDown(@NotNull UIEvent event) {
        super.onMouseDown(event);
        event.hasHandler = true;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}
