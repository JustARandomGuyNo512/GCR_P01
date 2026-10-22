package com.sheridan.gcr.client.screen.vanillaRemake;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CButton extends Button {
    private boolean enabled = true;
    @Nullable
    private final OnPress clickHandler;

    public CButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        this(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public CButton(int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration narration) {
        super(x, y, width, height, message, onPress, narration);
        this.clickHandler = onPress;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!enabled) {
            guiGraphics.setColor(0.4f, 0.4f, 0.4f, 0.5f);
        }
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.flush();
        guiGraphics.setColor(1, 1, 1, 1);
    }

    @Override
    public void onPress() {
        if (enabled && clickHandler != null) {
            clickHandler.onPress(this);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable(String tooltipKey) {
        enabled = true;
        visible = true;
        setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
    }

    public void disable(String tooltipKey) {
        enabled = false;
        visible = true;
        setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }
}
