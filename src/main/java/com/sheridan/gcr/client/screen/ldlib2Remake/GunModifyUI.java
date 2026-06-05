package com.sheridan.gcr.client.screen.ldlib2Remake;// MyItemPanelUI.java


import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.sheridan.gcr.GCR;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

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

    public static final IGuiTexture SEARCH = SpriteTexture.of(
            GCR.RL(GCR.MODID, "textures/gui/component/search.png")
    ).setSprite(0, 0, 32, 32);

    public static final int PAGE_SIZE = 5 * 10;
    private static UIElement right;
    private static UIElement contentRight;
    private static Button btnCollapseRight;
    private static Label pageLabel;

    private static UIElement left;

    private static UIElement main;
    public static CButton btnReplaceModule;
    public static CButton btnRemoveModule;
    public static CButton btnCommit;
    public static Button btnDragModel;
    public static Button btnResetModel;

    public static UIElement scrollComponents;
    public static CScroller scroller;
    private static Button btnResetScroller;
    private static TextField searchBox;

    private static List<List<DisplaySlot>> itemSlots;

    public static ModularUI createModularUI(GunModifyScreen screen) {
        var root = new UIElement();
        UIElement rightContent = createRightContent(screen);
        UIElement mainContent = createMainContent(screen);
        UIElement leftContent = createLeftContent(screen);
        root.addChildren(leftContent, mainContent, rightContent);
        root.layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.ROW));
        UI ui = UI.of(root);
        return new ModularUI(ui);
    }

    public static UIElement createLeftContent(GunModifyScreen screen) {
        UIElement leftContent = new UIElement()
                .layout(layoutStyle -> layoutStyle
                        .width(16 * 5 + 30)
                        .minHeight(239)
                        .heightPercent(100));
        return leftContent;
    }

    public static UIElement createMainContent(GunModifyScreen screen) {
        UIElement mainContent = new UIElement().layout(layoutStyle -> layoutStyle.flex(1));
        UIElement mainUpperContent = createMainUpperContent(screen);
        UIElement mainLowerContent = createMainLowerContent(screen);
        mainContent.addChildren(mainUpperContent, mainLowerContent);
        return mainContent;
    }

    private static UIElement createMainUpperContent(GunModifyScreen screen) {
        UIElement content = new UIElement();
        content.layout(layoutStyle -> layoutStyle
                        .gapAll(3)
                        .widthPercent(100)
                        .heightPercent(70));
        UIElement paddingTop = new UIElement().layout(layoutStyle -> layoutStyle.height(15).widthPercent(100));
        content.addChild(paddingTop);

        btnDragModel = (Button) new Button()
                .addPreIcon(DRAG)
                .noText()
                .style(basicStyle -> basicStyle.tooltips(Component.translatable("tooltip.btn.drag")))
                .addEventListener(UIEvents.MOUSE_DOWN, screen::onDragModel)
                .addEventListener(UIEvents.MOUSE_UP, e -> screen.stopDragModel());
        btnResetModel = (Button) new Button()
                .setOnClick(e -> screen.resetModelPose())
                .addPreIcon(RESET)
                .noText()
                .style(basicStyle -> basicStyle.tooltips(Component.translatable("tooltip.btn.reset")));

        btnReplaceModule = (CButton) new CButton().setOnClick(e -> screen.replaceSelectedModule()).addPreIcon(REPLACE).noText();
        btnRemoveModule = (CButton) new CButton().setOnClick(e -> screen.removeSelectedModule()).addPreIcon(STOP).noText();
        btnCommit = (CButton) new CButton().setOnClick(e -> screen.commitToServer()).addPreIcon(OK).noText();

        btnDragModel.layout(layoutStyle -> layoutStyle.width(16).height(16));
        btnResetModel.layout(layoutStyle -> layoutStyle.width(16).height(16));
        btnReplaceModule.layout(layoutStyle -> layoutStyle.width(16).height(16));
        btnRemoveModule.layout(layoutStyle -> layoutStyle.width(16).height(16));
        btnCommit.layout(layoutStyle -> layoutStyle.width(16).height(16));

        UIElement topNaviBar = new UIElement().layout(layoutStyle -> layoutStyle
                .flexDirection(FlexDirection.ROW)
                .widthPercent(100)
                .gapAll(5));

        Button zoomIn = (Button) new Button().setOnClick(e -> screen.zoomInModel())
                .setText("+")
                .layout(layoutStyle -> layoutStyle.width(16).height(16).justifyItems(AlignItems.CENTER))
                .style(basicStyle -> basicStyle.tooltips(Component.translatable("tooltip.btn.zoom_in")));
        Button zoomOut = (Button) new Button().setOnClick(e -> screen.zoomOutModel())
                .setText("-")
                .layout(layoutStyle -> layoutStyle.width(16).height(16).justifyItems(AlignItems.CENTER))
                .style(basicStyle -> basicStyle.tooltips(Component.translatable("tooltip.btn.zoom_out")));

        topNaviBar.addChildren(btnDragModel, btnResetModel, zoomIn, zoomOut);

        UIElement naviBarGap = new UIElement();
        naviBarGap.layout(layoutStyle -> layoutStyle.flex(1));
        topNaviBar.addChildren(naviBarGap, btnReplaceModule, btnRemoveModule, btnCommit);


        scrollComponents = new UIElement().layout(layoutStyle -> layoutStyle
                .gapAll(2)
                .alignItems(AlignItems.CENTER)
                .justifyContent(AlignContent.CENTER)
                .flexDirection(FlexDirection.ROW)
                .widthPercent(100));

        UIElement scrollerBox = new UIElement().layout(layoutStyle -> layoutStyle
                .flex(1)
                .maxWidth(200)
                .justifyContent(AlignContent.CENTER)
                .alignItems(AlignItems.CENTER)
                .paddingTop(6));

        scroller = (CScroller) new CScroller()
                .setRange(0f, 1f)
                .setScrollBarSize(13f)
                .headButton(btn -> {btn.setVisible(false); btn.getLayout().width(0).height(0);})
                .tailButton(btn -> {btn.setVisible(false); btn.getLayout().width(0).height(0);})
                .setOnValueChanged(screen::trySetNodePos)
                .layout(layoutStyle -> layoutStyle.widthPercent(100));
        scrollerBox.addChild(scroller);

        btnResetScroller = (Button) new Button()
                .setOnClick(e -> screen.resetScroller())
                .noText()
                .addPreIcon(RESET)
                .style(basicStyle -> basicStyle.tooltips(Component.translatable("tooltip.btn.reset_scroll")))
                .layout(layoutStyle -> layoutStyle
                        .width(16)
                        .height(16));
        scrollComponents.addChildren(scrollerBox, btnResetScroller);
        setScrollComponentsVisible(false);

        content.addChildren(
                topNaviBar,
                new UIElement().layout(layoutStyle -> layoutStyle
                        .flex(1)),
                scrollComponents);

        return content;
    }

    public static void setScrollComponentsVisible(boolean visible) {
        if (scrollComponents == null) {
            return;
        }
        scrollComponents.setVisible(visible);
    }

    public static void setScrollPos(float pos) {
        if (scroller != null) {
            scroller.setValue(pos);
        }
    }

    public static void defaultScrollDelta() {
        setScrollDelta(0.1f);
    }

    public static void setScrollDelta(float delta) {
        if (scroller == null) {
            return;
        }
        scroller.scrollerStyle(style -> style.scrollDelta(delta));
    }


    private static UIElement createMainLowerContent(GunModifyScreen screen) {
        UIElement content = new UIElement();
        content.layout(layoutStyle -> layoutStyle
                .widthPercent(100)
                .heightPercent(30));

        return content;
    }

    public static void collapseRight() {
        boolean visible = contentRight.isVisible();
        visible = !visible;
        contentRight.setVisible(visible);
        btnCollapseRight.setText(visible ? "tooltip.btn.fold" : "tooltip.btn.unfold", true);
    }

    public static void hideRight() {
        boolean visible = contentRight.isVisible();
        if (visible) {
            collapseRight();
        }
    }

    public static void showRight() {
        boolean visible = contentRight.isVisible();
        if (!visible) {
            collapseRight();
        }
    }

    private static void createItemSlots(GunModifyScreen screen) {
        itemSlots = new ArrayList<>();
        int itemTabColumns = 10;
        for (int i = 0; i < itemTabColumns; i++) {
            itemSlots.add(List.of(
                    (DisplaySlot) new DisplaySlot().addEventListener(UIEvents.MOUSE_DOWN, screen::onAttachmentSlotClick),
                    (DisplaySlot) new DisplaySlot().addEventListener(UIEvents.MOUSE_DOWN, screen::onAttachmentSlotClick),
                    (DisplaySlot) new DisplaySlot().addEventListener(UIEvents.MOUSE_DOWN, screen::onAttachmentSlotClick),
                    (DisplaySlot) new DisplaySlot().addEventListener(UIEvents.MOUSE_DOWN, screen::onAttachmentSlotClick),
                    (DisplaySlot) new DisplaySlot().addEventListener(UIEvents.MOUSE_DOWN, screen::onAttachmentSlotClick)));
        }
    }

    public static void clearItemSlots() {
        if (itemSlots != null) {
            for (List<DisplaySlot> itemSlots : itemSlots) {
                for (DisplaySlot itemSlot : itemSlots) {
                    itemSlot.setItem(ItemStack.EMPTY);
                }
            }
        }
    }

    public static List<List<DisplaySlot>> getItemSlots() {
        return itemSlots;
    }

    public static void clearSearchBox() {
        if (searchBox != null) {
            searchBox.setText("");
        }
    }

    private static UIElement createRightContent(GunModifyScreen screen) {
        btnCollapseRight = new Button()
                .setText("tooltip.btn.fold", true)
                .setOnClick(e -> collapseRight());
        right = new UIElement()
                .addChild(btnCollapseRight)
                .layout(layoutStyle -> layoutStyle
                        .width(16 * 5 + 30)
                        .minHeight(239)
                        .heightPercent(100));

        createItemSlots(screen);

        UIElement itemTab = new UIElement().layout(layout -> layout.justifyContent(AlignContent.CENTER));
        for (List<DisplaySlot> itemSlotList : itemSlots) {
            UIElement itemTabRow = new UIElement().layout(layoutStyle -> layoutStyle
                    .flexDirection(FlexDirection.ROW)
                    .justifyContent(AlignContent.CENTER)
                    .gapAll(2)
            );
            for (DisplaySlot itemSlot : itemSlotList) {
                itemTabRow.addChild(itemSlot);
            }
            itemTab.addChild(itemTabRow);
        }
        pageLabel = (Label) new Label()
                .textStyle(textStyle -> textStyle.textAlignHorizontal(Horizontal.CENTER))
                .setText(" -- / -- ")
                .layout(layoutStyle -> layoutStyle.flex(1));
        searchBox = (TextField) new TextField()
                .layout(layout -> layout.flex(1))
                .style(basicStyle -> basicStyle.tooltips(Component.translatable("tooltip.input.search_attachments")))
                .setId("search_attachments")
                .addEventListener(UIEvents.KEY_DOWN, e -> {
                    if (e.keyCode == GLFW.GLFW_KEY_ENTER) {
                        TextField searchBox = (TextField) e.currentElement;
                        String value = searchBox.getValue();
                        screen.searchAttachments(value);
                    }
                });
        contentRight = new UIElement().addChildren(
                        new UIElement()
                                .layout(layout -> layout.flexDirection(FlexDirection.ROW))
                                .addChildren(
                                        searchBox,
                                        new Button()
                                                .setOnClick(e -> {
                                                    String value = searchBox.getValue();
                                                    screen.searchAttachments(value);
                                                })
                                                .noText()
                                                .addPreIcon(SEARCH)
                                                .style(basicStyle -> basicStyle.tooltips(Component.translatable("tooltip.btn.search"))),
                                        new Button()
                                                .setOnClick(e -> screen.clearSearch())
                                                .noText()
                                                .addPreIcon(STOP)
                                                .style(basicStyle -> basicStyle.tooltips(Component.translatable("tooltip.btn.clear")))
                                ),
                        new Label().setText("tooltip.tab.suitable_attachments", true).layout(layoutStyle -> layoutStyle.paddingLeft(2)),
                        itemTab,
                        new UIElement()
                                .layout(layout -> layout
                                        .flexDirection(FlexDirection.ROW)
                                        .justifyContent(AlignContent.CENTER)
                                        .gapAll(2)
                                )
                                .addChildren(
                                        new Button().setText("<")
                                                .setOnClick(e -> {
                                                    int page = screen.getPage();
                                                    screen.setPage(page - 1);
                                                    syncPageInfo(screen);
                                                }),
                                        pageLabel,
                                        new Button().setText(">")
                                                .setOnClick(e -> {
                                                    int page = screen.getPage();
                                                    screen.setPage(page + 1);
                                                    syncPageInfo(screen);
                                                })
                        )
                ).addEventListener(UIEvents.MOUSE_DOWN, e -> {})
                .layout(layoutStyle -> layoutStyle
                        .widthPercent(100)
                        .flex(1)
                        .paddingAll(3))
                .style(style -> style.background(Sprites.BORDER))
                .setId("content_right_main");
        right.addChild(contentRight);
        return right;
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new GunModifyScreen(Component.empty()));
    }

    public static void syncPageInfo(GunModifyScreen screen) {
        int page = screen.getPage();
        int maxPage = screen.getMaxPage();
        if (maxPage == 0) {
            pageLabel.setText(" -- / -- ");
            return;
        }
        pageLabel.setText(page + " / " + maxPage);
    }
}