package com.sheridan.gcr.client.screen.vanillaRemake;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class GunModifyUI {
    public static final int PAGE_SIZE = 50;
    private static final List<List<DisplaySlot>> itemSlots = new ArrayList<>();
    private static boolean rightVisible = true;
    private static boolean uiVisible = true;
    private static int rightX, top, bottom, panelWidth;
    private static EditBox searchBox;
    private static Button collapse, prev, next, clear, resetScroll, resetModel, zoomIn, zoomOut, inspect, drag;
    public static CButton btnReplaceModule, btnRemoveModule, btnCommit;
    public static CScroller scroller;

    public interface Actions {
        void resetModelPose(); void zoomInModel(); void zoomOutModel(); void enterInspectionMode();
        void replaceSelectedModule(); void removeSelectedModule(); void commitToServer();
        void trySetNodePos(float pos); void resetScroller(); void onAttachmentSlotClick(DisplaySlot slot);
        int getPage(); void setPage(int page); void searchAttachments(String query); void clearSearch();
        void beginDrag();
    }
    private static Actions actions;
    private GunModifyUI() {}

    public static List<AbstractWidget> createWidgets(Actions actions, int width, int height) {
        GunModifyUI.actions = actions;
        itemSlots.clear();
        List<AbstractWidget> widgets = new ArrayList<>();
        panelWidth = Math.min(202, Math.max(150, width / 5));
        rightX = width - panelWidth - 8;
        top = 8;
        bottom = height - 8;
        int mainX = panelWidth + 16;
        int mainRight = rightX - 8;
        collapse = button(rightX - 18, top, 18, 18, "<", b -> collapseRight());
        searchBox = new EditBox(Minecraft.getInstance().font, rightX + 8, top + 24, panelWidth - 52, 18, Component.empty());
        searchBox.setHint(Component.translatable("tooltip.input.search_attachments"));
        searchBox.setTooltip(Tooltip.create(Component.translatable("tooltip.input.search_attachments")));
        clear = button(rightX + panelWidth - 20, top + 24, 18, 18, "×", b -> actions.clearSearch());
        Button search = button(rightX + panelWidth - 40, top + 24, 18, 18, "⌕", b -> actions.searchAttachments(searchBox.getValue()));
        widgets.addAll(List.of(collapse, searchBox, search, clear));
        for (int row = 0; row < 10; row++) {
            List<DisplaySlot> slots = new ArrayList<>();
            for (int col = 0; col < 5; col++) {
                DisplaySlot slot = new DisplaySlot(rightX + 12 + col * 20, top + 68 + row * 20, ItemStack.EMPTY);
                slot.setClickHandler(actions::onAttachmentSlotClick);
                slots.add(slot);
                widgets.add(slot);
            }
            itemSlots.add(slots);
        }
        prev = button(rightX + 18, bottom - 24, 28, 20, "<", b -> actions.setPage(actions.getPage() - 1));
        next = button(rightX + panelWidth - 46, bottom - 24, 28, 20, ">", b -> actions.setPage(actions.getPage() + 1));
        widgets.addAll(List.of(prev, next));
        drag = button(mainX, top, 20, 20, "↔", b -> actions.beginDrag());
        resetModel = button(mainX + 22, top, 20, 20, "R", b -> actions.resetModelPose());
        zoomIn = button(mainX + 46, top, 20, 20, "+", b -> actions.zoomInModel());
        zoomOut = button(mainX + 68, top, 20, 20, "-", b -> actions.zoomOutModel());
        inspect = button(mainX + 92, top, 54, 20, "检查", b -> actions.enterInspectionMode());
        int actionsX = Math.max(mainX + 150, mainRight - 66);
        btnReplaceModule = new CButton(actionsX, top, 20, 20, Component.literal("↻"), b -> actions.replaceSelectedModule());
        btnRemoveModule = new CButton(actionsX + 22, top, 20, 20, Component.literal("×"), b -> actions.removeSelectedModule());
        btnCommit = new CButton(actionsX + 44, top, 20, 20, Component.literal("✓"), b -> actions.commitToServer());
        widgets.addAll(List.of(drag, resetModel, zoomIn, zoomOut, inspect, btnReplaceModule, btnRemoveModule, btnCommit));
        scroller = new CScroller(mainX, bottom - 24, Math.max(40, mainRight - mainX - 24), 12).setValueChanged(actions::trySetNodePos);
        resetScroll = button(mainRight - 20, bottom - 28, 20, 20, "R", b -> actions.resetScroller());
        widgets.addAll(List.of(scroller, resetScroll));
        scroller.visible = false;
        resetScroll.visible = false;
        updateVisibility();
        return widgets;
    }

    private static Button button(int x, int y, int w, int h, String text, Button.OnPress action) {
        return Button.builder(Component.literal(text), action).bounds(x, y, w, h).build();
    }
    public static void collapseRight() { rightVisible = !rightVisible; updateVisibility(); }
    public static void hideRight() { rightVisible = false; updateVisibility(); }
    public static void showRight() { rightVisible = true; updateVisibility(); }
    public static void setVisible(boolean visible) { uiVisible = visible; updateVisibility(); }
    private static void updateVisibility() {
        boolean right = uiVisible && rightVisible;
        if (collapse != null) collapse.visible = uiVisible;
        if (searchBox != null) searchBox.visible = right;
        if (clear != null) clear.visible = right;
        if (prev != null) prev.visible = right;
        if (next != null) next.visible = right;
        for (List<DisplaySlot> row : itemSlots) for (DisplaySlot slot : row) slot.visible = right;
        for (AbstractWidget w : List.of(drag, resetModel, zoomIn, zoomOut, inspect, btnReplaceModule, btnRemoveModule, btnCommit)) if (w != null) w.visible = uiVisible;
        if (scroller != null && !uiVisible) scroller.visible = false;
        if (resetScroll != null && (scroller == null || !scroller.visible || !uiVisible)) resetScroll.visible = false;
    }
    public static void setScrollComponentsVisible(boolean visible) {
        if (scroller != null) scroller.visible = visible && uiVisible;
        if (resetScroll != null) resetScroll.visible = visible && uiVisible;
    }
    public static void setScrollPos(float pos) { if (scroller != null) scroller.setSilentValue(pos); }
    public static void resetScrollPos(float pos) { if (scroller != null) scroller.setValue(pos); }
    public static void clearItemSlots() { for (List<DisplaySlot> row : itemSlots) for (DisplaySlot slot : row) { slot.setItem(ItemStack.EMPTY); slot.setCount(0); } }
    public static List<List<DisplaySlot>> getItemSlots() { return itemSlots; }
    public static void clearSearchBox() { if (searchBox != null) searchBox.setValue(""); }
    public static String getSearchText() { return searchBox == null ? "" : searchBox.getValue(); }
    public static boolean isSearchFocused() { return searchBox != null && searchBox.isFocused(); }
    public static void syncPageInfo() {}
    public static void open() { Minecraft.getInstance().setScreen(new GunModifyScreen(Component.empty())); }
}
