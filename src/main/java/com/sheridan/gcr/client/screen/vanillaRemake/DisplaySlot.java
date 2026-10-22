package com.sheridan.gcr.client.screen.vanillaRemake;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class DisplaySlot extends net.minecraft.client.gui.components.AbstractWidget {
    private ItemStack stack;
    private int count;
    private Consumer<DisplaySlot> clickHandler = slot -> {};

    public DisplaySlot(int x, int y, ItemStack stack) {
        super(x, y, 18, 18, Component.empty());
        this.stack = stack;
    }

    public ItemStack getValue() { return stack; }
    public void setItem(ItemStack stack) { this.stack = stack; }
    public void setCount(int count) { this.count = count; }
    public int getCount() { return count; }
    public void setClickHandler(Consumer<DisplaySlot> clickHandler) { this.clickHandler = clickHandler; }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (stack.isEmpty()) return;
        if (count <= 0) graphics.setColor(0.3f, 0.3f, 0.3f, 1.0f);
        graphics.renderItem(stack, getX() + 1, getY() + 1);
        graphics.flush();
        graphics.setColor(1, 1, 1, 1);
        graphics.renderItemDecorations(Minecraft.getInstance().font, stack, getX() + 1, getY() + 1, "");
        graphics.pose().pushPose();
        graphics.pose().translate(getX() + 13, getY() + 13, 0.0f);
        graphics.pose().scale(0.75f, 0.75f, 1.0f);
        graphics.drawCenteredString(Minecraft.getInstance().font, count > 99 ? "99+" : Integer.toString(count), 0, 0, count > 0 ? 0xFF00FF00 : 0xFFFF0000);
        graphics.pose().popPose();
    }

    @Override
    public void onClick(double mouseX, double mouseY) { clickHandler.accept(this); }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}
}
