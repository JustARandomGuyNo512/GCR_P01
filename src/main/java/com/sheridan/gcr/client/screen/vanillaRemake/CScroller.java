package com.sheridan.gcr.client.screen.vanillaRemake;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public class CScroller extends net.minecraft.client.gui.components.AbstractSliderButton {
    private double minValue = 0.0D;
    private double maxValue = 1.0D;
    private Function<Float, Boolean> preValueChange = f -> true;
    private Consumer<Float> valueChanged = f -> {};
    private boolean dragging;

    public CScroller(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty(), 0.0D);
    }

    public CScroller setPreValueChange(Function<Float, Boolean> callback) {
        preValueChange = callback;
        return this;
    }

    public CScroller setValueChanged(Consumer<Float> callback) {
        valueChanged = callback;
        return this;
    }

    public void setBounds(double min, double max) {
        minValue = Math.min(min, max);
        maxValue = Math.max(min, max);
        setValue(getValue());
    }

    public void setSilentValue(double newValue) {
        double clamped = Mth.clamp(newValue, minValue, maxValue);
        value = maxValue == minValue ? 0 : (clamped - minValue) / (maxValue - minValue);
        updateMessage();
    }

    public double getValue() { return minValue + (maxValue - minValue) * value; }

    public void setValue(double newValue) {
        double clamped = Mth.clamp(newValue, minValue, maxValue);
        if (!preValueChange.apply((float) clamped)) return;
        value = maxValue == minValue ? 0 : (clamped - minValue) / (maxValue - minValue);
        updateMessage();
        applyValue();
    }

    @Override
    protected void updateMessage() { setMessage(Component.empty()); }

    @Override
    protected void applyValue() { valueChanged.accept((float) getValue()); }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY() + height / 2 - 1, getX() + width, getY() + height / 2 + 1, 0xFF555555);
        int thumbX = getX() + (int) (value * (width - 8));
        graphics.fill(thumbX, getY(), thumbX + 8, getY() + height, isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFBBBBBB);
    }

    @Override
    public void onClick(double mouseX, double mouseY) { dragging = true; }

    @Override
    public void onRelease(double mouseX, double mouseY) { dragging = false; }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (!dragging) return;
        double trackWidth = width - 8;
        if (trackWidth <= 0) return;
        double normalized = Mth.clamp((mouseX - getX() - 4) / trackWidth, 0, 1);
        setValue(minValue + normalized * (maxValue - minValue));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) { return false; }
}
