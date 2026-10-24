package com.sheridan.gcr.client.screen.ldlib2Remake;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CButton extends Button {
    private boolean enabled = true;

    @Override
    public void drawContents(@NotNull GUIContext guiContext) {
        if (!enabled) {
            guiContext.graphics.setColor(0.4f, 0.4f, 0.4f, 0.5f);
        }
        super.drawContents(guiContext);
        guiContext.graphics.flush();
        guiContext.graphics.setColor(1, 1, 1, 1);
    }

    @Override
    public void drawBackgroundOverlay(@NotNull GUIContext guiContext) {
        if (!enabled) {
            guiContext.graphics.setColor(0.4f, 0.4f, 0.4f, 0.5f);
        }
        super.drawBackgroundOverlay(guiContext);
        guiContext.graphics.flush();
        guiContext.graphics.setColor(1, 1, 1, 1);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable(String tooltipKey) {
        enabled = true;
        setVisible(true);
        getStyle().tooltips(Component.translatable(tooltipKey));
    }

    public void disable(String tooltipKey) {
        enabled = false;
        setVisible(true);
        getStyle().tooltips(Component.translatable(tooltipKey));
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    @Override
    public @NotNull Button setOnClick(@Nullable UIEventListener onClick) {
        return super.setOnClick(event -> {
            if (enabled && onClick != null) {
                onClick.handleEvent(event);
            }
        });
    }
}
