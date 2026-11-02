package com.sheridan.gcr.client.screen.ldlib2Remake;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.sheridan.gcr.GCR;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GunModifyUI {
    public static final IGuiTexture DRAG = SpriteTexture.of(
            GCR.RL(GCR.MODID, "textures/gui/component/drag.png")
    ).setSprite(0, 0, 32, 32);

    public static final IGuiTexture OK = SpriteTexture.of(
            GCR.RL(GCR.MODID, "textures/gui/component/ok.png")
    ).setSprite(0, 0, 32, 32);

    public static final IGuiTexture REPLACE = SpriteTexture.of(
            GCR.RL(GCR.MODID, "textures/gui/component/replace.png")
    ).setSprite(0, 0, 32, 32);

    public static final IGuiTexture STOP = SpriteTexture.of(
            GCR.RL(GCR.MODID, "textures/gui/component/stop.png")
    ).setSprite(0, 0, 32, 32);

    public static final IGuiTexture RESET = SpriteTexture.of(
            GCR.RL(GCR.MODID, "textures/gui/component/reset.png")
    ).setSprite(0, 0, 32, 32);

    public static final int GRID_COLUMNS = 6;
    public static final int GRID_ROWS = 3;
    public static final int PAGE_SIZE = GRID_COLUMNS * GRID_ROWS;

    private static final int INK = 0xFFE7E2D6;
    private static final int MUTED = 0xFF9C978C;
    private static final int HAIRLINE = 0x66E7E2D6;

    private static final IGuiTexture FILL = new ColorRectTexture(0xE0101216);
    private static final IGuiTexture RULE = new ColorRectTexture(HAIRLINE);
    private static final IGuiTexture WELL = IGuiTexture.group(
            new ColorRectTexture(0xCC171A1E),
            new ColorBorderTexture(1, 0x33E7E2D6));
    private static final IGuiTexture HOVER = new ColorRectTexture(0x28FFFFFF);
    private static final IGuiTexture PRESS = new ColorRectTexture(0x40E7E2D6);
    private static final IGuiTexture CHIP = IGuiTexture.group(
            new ColorRectTexture(0x22101418),
            new ColorBorderTexture(1, 0x99E7E2D6));
    private static final IGuiTexture CHIP_HOVER = IGuiTexture.group(
            new ColorRectTexture(0x33E7E2D6),
            new ColorBorderTexture(1, 0xFFE7E2D6));

    private static UIElement root;
    private static UIElement tray;
    private static UIElement catalogBody;
    private static Button btnCollapseRight;
    private static Label pageLabel;
    private static Label slotTitle;
    private static Label weaponName;
    private static UIElement actionRow;
    private static UIElement centerPrompt;

    public static CButton btnReplaceModule;
    public static CButton btnRemoveModule;
    public static CButton btnCommit;
    public static Button btnDragModel;
    public static Button btnResetModel;

    private static TextField searchBox;

    private static List<List<DisplaySlot>> itemSlots;

    public static ModularUI createModularUI(GunModifyScreen screen) {
        root = new UIElement();
        root.addChild(createViewport(screen));
        root.layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .paddingAll(14)
                .flexDirection(FlexDirection.COLUMN));
        return new ModularUI(UI.of(root));
    }

    private static UIElement createViewport(GunModifyScreen screen) {
        UIElement viewport = new UIElement().layout(layout -> layout
                .flex(1)
                .widthPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(8));

        UIElement trayDock = new UIElement()
                .layout(layout -> layout.widthPercent(100).flexDirection(FlexDirection.ROW))
                .addChildren(new UIElement().layout(layout -> layout.flex(1)), createTray(screen));
        viewport.addChildren(
                createTopBar(screen),
                new UIElement().layout(layout -> layout.flex(1)),
                createCaption(),
                trayDock);
        return viewport;
    }

    private static UIElement createTopBar(GunModifyScreen screen) {
        Label kicker = (Label) new Label()
                .setText("screen.modify.kicker", true)
                .textStyle(style -> style.textColor(MUTED).textShadow(false))
                .layout(layout -> layout.height(10));
        weaponName = (Label) new Label()
                .setText(Component.empty())
                .textStyle(style -> style
                        .textColor(INK)
                        .textShadow(false)
                        .textWrap(TextWrap.HOVER_ROLL)
                        .adaptiveWidth(false))
                .layout(layout -> layout.widthPercent(100).height(12));
        UIElement titles = new UIElement()
                .layout(layout -> layout.flex(1).flexDirection(FlexDirection.COLUMN).gapAll(1))
                .addChildren(kicker, weaponName);

        btnDragModel = iconButton(DRAG, "tooltip.btn.drag");
        btnDragModel.addEventListener(UIEvents.MOUSE_DOWN, screen::onDragModel);
        btnDragModel.addEventListener(UIEvents.MOUSE_UP, e -> screen.stopDragModel());
        btnResetModel = (Button) iconButton(RESET, "tooltip.btn.reset")
                .setOnClick(e -> screen.resetModelPose());

        Button zoomOut = textButton("-", "tooltip.btn.zoom_out", 16);
        zoomOut.setOnClick(e -> screen.zoomOutModel());
        Button zoomIn = textButton("+", "tooltip.btn.zoom_in", 16);
        zoomIn.setOnClick(e -> screen.zoomInModel());
        Button inspect = textButton("screen.modify.inspect", "tooltip.btn.inspect", 58);
        inspect.setText("screen.modify.inspect", true);
        inspect.setOnClick(e -> screen.enterInspectionMode());

        btnCommit = (CButton) new CButton()
                .setText("screen.modify.save", true)
                .setOnClick(e -> screen.commitToServer());
        chip(btnCommit);
        btnCommit.layout(layout -> layout.width(58).height(16));
        btnCommit.disable("tooltip.btn.unmodified");

        UIElement tools = new UIElement()
                .layout(layout -> layout
                        .flexDirection(FlexDirection.ROW)
                        .alignItems(AlignItems.CENTER)
                        .gapAll(6))
                .addChildren(btnDragModel, btnResetModel, zoomOut, zoomIn, inspect, btnCommit);

        return new UIElement()
                .layout(layout -> layout
                        .flexDirection(FlexDirection.ROW)
                        .alignItems(AlignItems.CENTER)
                        .widthPercent(100)
                        .gapAll(8))
                .addEventListener(UIEvents.MOUSE_DOWN, GunModifyUI::consumeClick)
                .addChildren(titles, tools);
    }

    private static UIElement createCaption() {
        Label promptLabel = (Label) new Label()
                .setText("screen.modify.hint.select", true)
                .textStyle(style -> style
                        .textAlignHorizontal(Horizontal.CENTER)
                        .textWrap(TextWrap.WRAP)
                        .adaptiveWidth(false)
                        .adaptiveHeight(true)
                        .textColor(INK)
                        .textShadow(true))
                .layout(layout -> layout.widthPercent(100));
        centerPrompt = new UIElement()
                .layout(layout -> layout.widthPercent(100).paddingBottom(6))
                .addChild(promptLabel);
        return centerPrompt;
    }

    private static UIElement createTray(GunModifyScreen screen) {
        slotTitle = (Label) new Label()
                .setText("screen.modify.catalog_title", true)
                .textStyle(style -> style
                        .textColor(INK)
                        .textShadow(false)
                        .textWrap(TextWrap.NONE)
                        .adaptiveWidth(true))
                .layout(layout -> layout.height(12));

        btnRemoveModule = (CButton) new CButton()
                .setText("screen.modify.remove", true)
                .setOnClick(e -> screen.removeSelectedModule());
        ghost(btnRemoveModule);
        btnRemoveModule.layout(layout -> layout.width(62).height(14));
        btnRemoveModule.disable("tooltip.btn.opt_not_allow");

        btnReplaceModule = (CButton) new CButton()
                .setText("screen.modify.replace", true)
                .setOnClick(e -> screen.replaceSelectedModule());
        ghost(btnReplaceModule);
        btnReplaceModule.layout(layout -> layout.width(68).height(14));
        btnReplaceModule.disable("tooltip.btn.no_module");

        actionRow = new UIElement()
                .layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(2))
                .addChildren(btnRemoveModule, btnReplaceModule);
        setShown(actionRow, false);

        btnCollapseRight = textButton("screen.modify.hide_list", "tooltip.btn.fold", 42);
        btnCollapseRight.setText("screen.modify.hide_list", true);
        btnCollapseRight.setOnClick(e -> collapseRight());

        UIElement actions = new UIElement()
                .layout(layout -> layout
                        .flexDirection(FlexDirection.ROW)
                        .alignItems(AlignItems.CENTER)
                        .gapAll(2))
                .addChildren(actionRow, btnCollapseRight);
        UIElement header = new UIElement()
                .layout(layout -> layout
                        .flexDirection(FlexDirection.COLUMN)
                        .widthPercent(100)
                        .gapAll(2))
                .addChildren(slotTitle, actions);

        searchBox = (TextField) new TextField()
                .setAnyString()
                .setTextResponder(screen::searchAttachments)
                .textFieldStyle(style -> style
                        .textColor(INK)
                        .cursorColor(INK)
                        .textShadow(false)
                        .placeholder(Component.translatable("tooltip.input.search_attachments")))
                .layout(layout -> layout.flex(1).height(14))
                .style(style -> style
                        .background(WELL)
                        .tooltips(Component.translatable("tooltip.input.search_attachments")))
                .setId("search_attachments");
        Button clear = textButton("x", "tooltip.btn.clear", 14);
        clear.setOnClick(e -> screen.clearSearch());
        UIElement searchRow = new UIElement()
                .layout(layout -> layout
                        .flexDirection(FlexDirection.ROW)
                        .alignItems(AlignItems.CENTER)
                        .widthPercent(100)
                        .gapAll(6))
                .addChildren(searchBox, clear);

        UIElement cards = createCards(screen);

        pageLabel = (Label) new Label()
                .setText(Component.translatable("screen.modify.page_empty"))
                .textStyle(style -> style
                        .textAlignHorizontal(Horizontal.CENTER)
                        .textAlignVertical(Vertical.CENTER)
                        .textColor(MUTED)
                        .textShadow(false))
                .layout(layout -> layout.flex(1).height(12));
        Button prev = textButton("<", "tooltip.btn.page_prev", 16);
        prev.setOnClick(e -> screen.showPage(screen.getPage() - 1));
        Button next = textButton(">", "tooltip.btn.page_next", 16);
        next.setOnClick(e -> screen.showPage(screen.getPage() + 1));
        UIElement pager = new UIElement()
                .layout(layout -> layout
                        .flexDirection(FlexDirection.ROW)
                        .alignItems(AlignItems.CENTER)
                        .justifyContent(AlignContent.CENTER)
                        .widthPercent(100)
                        .gapAll(8))
                .addChildren(prev, pageLabel, next);

        catalogBody = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .flexDirection(FlexDirection.COLUMN)
                        .gapAll(6))
                .addChildren(searchRow, cards, pager);

        UIElement body = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .flexDirection(FlexDirection.COLUMN)
                        .paddingAll(6)
                        .gapAll(4))
                .addChildren(header, catalogBody);

        tray = new UIElement()
                .layout(layout -> layout.width(188).flexDirection(FlexDirection.COLUMN))
                .style(style -> style.background(FILL))
                .addEventListener(UIEvents.MOUSE_DOWN, GunModifyUI::consumeClick)
                .addChildren(rule(), body)
                .setId("content_right_main");
        setShown(tray, false);
        return tray;
    }

    private static UIElement createCards(GunModifyScreen screen) {
        itemSlots = new ArrayList<>();
        UIElement grid = new UIElement().layout(layout -> layout
                .flexDirection(FlexDirection.COLUMN)
                .alignItems(AlignItems.CENTER)
                .widthPercent(100)
                .gapAll(2));
        for (int rowIndex = 0; rowIndex < GRID_ROWS; rowIndex++) {
            UIElement row = new UIElement().layout(layout -> layout
                    .flexDirection(FlexDirection.ROW)
                    .justifyContent(AlignContent.CENTER)
                    .gapAll(2));
            List<DisplaySlot> slots = new ArrayList<>();
            for (int column = 0; column < GRID_COLUMNS; column++) {
                DisplaySlot slot = new DisplaySlot();
                slot.addEventListener(UIEvents.MOUSE_DOWN, screen::onAttachmentSlotClick);
                slot.layout(layout -> layout.width(18).height(18));
                slot.style(style -> style.background(WELL));
                slot.slotStyle(style -> style.hoverOverlay(HOVER).showItemTooltips(true));
                row.addChild(slot);
                slots.add(slot);
            }
            itemSlots.add(slots);
            grid.addChild(row);
        }
        return grid;
    }

    private static UIElement rule() {
        return new UIElement()
                .layout(layout -> layout.widthPercent(100).height(1))
                .style(style -> style.background(RULE));
    }

    private static Button iconButton(IGuiTexture icon, String tooltip) {
        Button button = (Button) new Button()
                .addPreIcon(icon)
                .noText()
                .layout(layout -> layout.width(16).height(16))
                .style(style -> style.tooltips(Component.translatable(tooltip)));
        ghost(button);
        return button;
    }

    private static Button textButton(String text, String tooltip, float width) {
        Button button = (Button) new Button()
                .setText(text)
                .layout(layout -> layout.width(width).height(14).justifyItems(AlignItems.CENTER))
                .style(style -> style.tooltips(Component.translatable(tooltip)));
        ghost(button);
        return button;
    }

    private static void ghost(Button button) {
        button.buttonStyle(style -> style
                .baseTexture(IGuiTexture.EMPTY)
                .hoverTexture(HOVER)
                .pressedTexture(PRESS));
        button.style(style -> style.background(IGuiTexture.EMPTY));
        button.textStyle(style -> style
                .textColor(INK)
                .textShadow(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));
    }

    private static void chip(Button button) {
        button.buttonStyle(style -> style
                .baseTexture(CHIP)
                .hoverTexture(CHIP_HOVER)
                .pressedTexture(PRESS));
        button.style(style -> style.background(IGuiTexture.EMPTY));
        button.textStyle(style -> style
                .textColor(INK)
                .textShadow(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));
    }

    private static void consumeClick(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent event) {
        event.hasHandler = true;
        event.stopPropagation();
    }

    private static void setShown(UIElement element, boolean shown) {
        if (element == null) {
            return;
        }
        element.setVisible(shown);
        element.setDisplay(shown);
    }

    public static void setWeaponName(Component name) {
        if (weaponName != null && name != null) {
            weaponName.setText(name);
        }
    }

    public static void setModuleActionsVisible(boolean visible) {
        if (actionRow == null || actionRow.isVisible() == visible) {
            return;
        }
        setShown(actionRow, visible);
    }

    public static void showIdlePrompt() {
        setShown(centerPrompt, true);
        setShown(tray, false);
        if (slotTitle != null) {
            slotTitle.setText(Component.translatable("screen.modify.catalog_title"));
        }
    }

    public static void showSelection(Component title) {
        setShown(centerPrompt, false);
        setShown(tray, true);
        if (slotTitle != null) {
            slotTitle.setText(title);
        }
        fitTray(title);
    }

    /** Grow the panel so the slot title is fully inside it. */
    private static void fitTray(Component title) {
        if (tray == null || title == null) {
            return;
        }
        int text = Minecraft.getInstance().font.width(title) + 20;
        int buttons = 62 + 68 + 42 + 16;
        int grid = GRID_COLUMNS * 18 + (GRID_COLUMNS - 1) * 2 + 16;
        int width = Math.max(text, Math.max(buttons, grid));
        int screen = Minecraft.getInstance().getWindow().getGuiScaledWidth() - 36;
        if (screen > 96) {
            width = Math.min(width, screen);
        }
        tray.getLayout().width(width);
    }

    public static void collapseRight() {
        if (catalogBody == null) {
            return;
        }
        boolean show = !catalogBody.isVisible();
        setShown(catalogBody, show);
        if (btnCollapseRight != null) {
            btnCollapseRight.setText(show ? "screen.modify.hide_list" : "screen.modify.show_list", true);
            btnCollapseRight.getStyle().tooltips(Component.translatable(show ? "tooltip.btn.fold" : "tooltip.btn.unfold"));
        }
    }

    public static void hideRight() {
        setShown(catalogBody, false);
        setShown(tray, false);
    }

    public static void showRight() {
        setShown(tray, true);
        setShown(catalogBody, true);
        if (btnCollapseRight != null) {
            btnCollapseRight.setText("screen.modify.hide_list", true);
            btnCollapseRight.getStyle().tooltips(Component.translatable("tooltip.btn.fold"));
        }
    }

    public static void clearItemSlots() {
        if (itemSlots == null) {
            return;
        }
        for (List<DisplaySlot> row : itemSlots) {
            for (DisplaySlot itemSlot : row) {
                itemSlot.setItem(ItemStack.EMPTY);
                itemSlot.setCount(0);
            }
        }
    }

    public static List<List<DisplaySlot>> getItemSlots() {
        return itemSlots;
    }

    public static void clearSearchBox() {
        if (searchBox == null) {
            return;
        }
        String value = searchBox.getValue();
        if (value != null && !value.isEmpty()) {
            searchBox.setText("");
        }
    }

    public static void setVisible(boolean visible) {
        if (root != null) {
            root.setVisible(visible);
        }
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new GunModifyScreen(Component.empty()));
    }

    public static void syncPageInfo(GunModifyScreen screen) {
        if (pageLabel == null) {
            return;
        }
        int maxPage = screen.getMaxPage();
        if (maxPage <= 0) {
            pageLabel.setText(Component.translatable("screen.modify.page_empty"));
            return;
        }
        pageLabel.setText(Component.translatable("screen.modify.page", screen.getPage(), maxPage));
    }
}
