package com.sheridan.gcr.client.screen.ldlib2Remake;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.render.ModifyScreenRenderContext;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.items.ModuleItem;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.ModuleRegister;
import com.sheridan.gcr.modularSys.builder.*;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.slot.IRail;
import com.sheridan.gcr.modularSys.slot.OperationType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class GunModifyScreen extends Screen {
    private static final ResourceLocation SELECTED_ITEM_SLOT = GCR.RL(GCR.MODID, "textures/gui/component/selected_slot.png");

    private GunModifyContext context;
    private NoLogChatComponent validateMsg;
    private boolean isDraggingModel = false;
    private boolean isRollingModel = false;
    private boolean needUpdate = false;
    private float modelRX;
    private float modelRY;
    private float modelX;
    private float modelY;
    private float modelScale = 1;
    private float tempModelRX;
    private float tempModelRY;
    private float dragStartX;
    private float dragStartY;
    private int msgDelayTick = 0;

    private ModularUI ui;

    private final List<ItemStack> allSuitableModules = new ArrayList<>();

    public ModularCountManager countManager = new ModularCountManager();

    private int page = 1;

    private SlotInstance lastModularSlot = null;

    private DisplaySlot selectedItemSlot = null;

    protected GunModifyScreen(Component title) {
        super(title);
        Minecraft instance = Minecraft.getInstance();
        if (instance.player != null) {
            countManager.onTick(instance.player);
        }
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        if (allSuitableModules.isEmpty()) {
            return;
        }
        if (this.page == page) {
            return;
        }
        int maxPage = getMaxPage();
        page = Math.max(page, 1);
        if (page > maxPage) {
            page = page % maxPage;
        }
        this.page = page;
    }

    public int getMaxPage() {
        int maxPageSize = allSuitableModules.size() / GunModifyUI.PAGE_SIZE;
        if (allSuitableModules.size() % GunModifyUI.PAGE_SIZE != 0) {
            maxPageSize++;
        }
        return maxPageSize;
    }

    @Override
    public void init() {
        super.init();
        Client.getGunRenderer().setHideFPRender(true);
        var modularUI = GunModifyUI.createModularUI(this);
        modularUI.setScreenAndInit(this);
        this.addRenderableWidget(modularUI.getWidget());
        this.ui = modularUI;
        checkAndCreateContext();
        if (context != null) {
            onSelectionChange(context.getSelectedNode(), context.getSelectedSlot());
        }
    }

    public void onServerResp(boolean success, String msg) {
        needUpdate = false;
        context = null;
        countManager = new ModularCountManager();
        checkAndCreateContext();
        if (msg != null && !msg.isEmpty()) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.literal(msg));
                this.minecraft.player.sendSystemMessage(Component.literal("success: " + success));
                msgDelayTick = 60;
            }
        }
    }

    public ModularUI getUi() {
        return ui;
    }


    public<T> T getElement(String id, Class<T> clazz) {
        if (ui != null) {
            UIElement elementById = ui.getElementById(id);
            if (clazz.isInstance(elementById)) {
                return clazz.cast(elementById);
            }
        }
        return null;
    }

    public float getRailPos() {
        System.out.println("get rail pos");
        return 0;
    }

    public void setRailPos(float pos) {
        System.out.println("set rail pos: " + pos);
    }

    public<T> Optional<T> getElementOptional(String id, Class<T> clazz) {
        T element = getElement(id, clazz);
        return element == null ? Optional.empty() : Optional.of(element);
    }

    @Override
    public void onClose() {
        super.onClose();
        Client.getGunRenderer().setHideFPRender(false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.minecraft != null && this.minecraft.player != null) {
            countManager.onTick(this.minecraft.player);
        }
        msgDelayTick = Math.max(0, msgDelayTick - 1);
        updatePageContent();
        handleMsgLog();
        if (context != null) {
            context.onTick();
            Node selectedNode = context.getSelectedNode();
            if (selectedNode != null && selectedNode.getBelongsTo() != null) {
                SlotInstance belongsTo = selectedNode.getBelongsTo();
                if (belongsTo.allow(OperationType.REMOVE)) {
                    GunModifyUI.btnRemoveModule.enable("tooltip.btn.remove_module");
                } else {
                    GunModifyUI.btnRemoveModule.disable("tooltip.btn.opt_not_allow");
                }
                if (belongsTo.allow(OperationType.REPLACE)) {//如果允许替换操作
                    IModular selectedModule = getSelectedModuleFromItem();
                    if (selectedItemSlot != null && selectedModule != null) { //选择了某个item物品
                        if (selectedModule == selectedNode.getModule()) {//不能替换相同的模块
                            GunModifyUI.btnReplaceModule.disable("tooltip.btn.same_module");
                        } else if (belongsTo.accepts(selectedModule)) {//选择的是模块物品并且槽位接受
                            GunModifyUI.btnReplaceModule.enable("tooltip.btn.replace_to");
                        } else {
                            GunModifyUI.btnReplaceModule.disable("tooltip.btn.slot_not_support");
                        }
                    } else {
                        GunModifyUI.btnReplaceModule.disable("tooltip.btn.no_module");
                    }
                } else {
                    GunModifyUI.btnReplaceModule.disable("tooltip.btn.opt_not_allow");
                }
            } else {
                GunModifyUI.btnReplaceModule.setVisible(false);
                GunModifyUI.btnRemoveModule.setVisible(false);
            }
            if (context.treeModified()) {
                if (context.allowSave()) {
                    GunModifyUI.btnCommit.enable("tooltip.btn.save");
                } else {
                    GunModifyUI.btnCommit.disable("tooltip.btn.has_error");
                }
            } else {
                //没有改动
                GunModifyUI.btnCommit.disable("tooltip.btn.unmodified");
            }
        }
    }

    private void handleMsgLog() {
        if (this.minecraft != null && validateMsg == null) {
            validateMsg = new NoLogChatComponent(this.minecraft);
        }
        if (validateMsg != null) {
            validateMsg.clearMessages(true);
            ValidateResult validateResult = context.getValidateResult();
            if (validateResult != null) {
                List<ValidationError> issues = validateResult.getIssues();
                for (ValidationError issue : issues) {
                    validateMsg.addMessage(issue.getComponentMsg());
                }
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderGunModel();
        if (context != null) {
            context.renderComponents(guiGraphics);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (selectedItemSlot != null) {
            guiGraphics.blit(SELECTED_ITEM_SLOT,
                        (int) (selectedItemSlot.getContentX() - 3),
                        (int) (selectedItemSlot.getContentY() - 3),
                        0,0, 22, 22, 22, 22);

        }
        if (validateMsg != null && msgDelayTick == 0) {
            validateMsg.render(guiGraphics, 0, mouseX, mouseY, false);
        }
        RenderSystem.enableBlend();
        if (needUpdate) {
            guiGraphics.drawCenteredString(
                    this.font, Component.translatable("screen.need_update").getString(), this.width / 2, this.height / 2, 0xffffff);
        }
        RenderSystem.disableBlend();
    }

    private void renderGunModel() {
        if (context != null) {
            Client.getGunRenderer().renderGunModifyScreen(
                    context.itemStack,
                    context.gun,
                    context.renderRoot,
                    -modelX * 0.01f,
                    modelY * 0.01f,
                    modelRX,
                    modelRY,
                    modelScale,
                    this::updateRenderContext);

        }
    }

    public void onAttachmentSlotClick(UIEvent event) {
        if (context == null) {
            return;
        }
        DisplaySlot slot = (DisplaySlot) event.currentElement;
        if (slot.getValue().isEmpty()) {
            return;
        }
        if (slot.getCount() <= 0) {
            return;
        }
        selectedItemSlot = slot;
        SlotInstance selectedSlot = context.getSelectedSlot();
        Node selectedNode = context.getSelectedNode();
        ItemStack value = slot.getValue();
        if (value.getItem() instanceof ModuleItem<?> moduleItem) {
            IModular module = moduleItem.getModule();
            if (module instanceof IGun) {
                return;
            }
            if (selectedNode == null && selectedSlot != null && selectedSlot.accepts(module)) {
                int moduleLeftCount = countManager.getModuleLeftCount(module);
                if (moduleLeftCount > 0) {
                    if (context.addUnit(module)) {
                        countManager.onModuleUsed(module);
                        selectedItemSlot = null;
                    }
                }
            }
        }
    }

    public void onDragModel(UIEvent event) {
        dragStartX = event.x;
        dragStartY = event.y;
        isDraggingModel = true;
    }

    public void stopDragModel() {
        dragStartX = 0;
        dragStartY = 0;
        isDraggingModel = false;
    }

    public void zoomInModel() {
        modelScale += 0.2f;
        modelScale = Mth.clamp(modelScale, 0.5f, 2.5f);
    }

    public void zoomOutModel() {
        modelScale -= 0.2f;
        modelScale = Mth.clamp(modelScale, 0.5f, 2.5f);
    }

    public void removeSelectedModule() {
        List<IModular> removed = context.removeSelectedNode();
        for (IModular modular : removed) {
            countManager.onModuleTakeOffFromGun(modular);
        }
        if (!removed.isEmpty()) {
            selectedItemSlot = null;
        }
    }

    private IModular getSelectedModuleFromItem() {
        if (selectedItemSlot == null) {
            return null;
        }
        int count = selectedItemSlot.getCount();
        if (count <= 0) {
            return null;
        }
        Item item = selectedItemSlot.getValue().getItem();
        return item instanceof ModuleItem<?> moduleItem ? moduleItem.getModule() : null;
    }

    public void replaceSelectedModule() {
        IModular selectedModuleFromItem = getSelectedModuleFromItem();
        if (selectedModuleFromItem == null) {
            return;
        }
        Pair<Unit, List<Unit>> unitListPair = context.replaceSelectNode(selectedModuleFromItem);
        Unit installed = unitListPair.getLeft();
        if (installed != null) {
            selectedItemSlot = null;
            List<Unit> removed = unitListPair.getRight();
            for (Unit unit : removed) {
                countManager.onModuleTakeOffFromGun(unit.getModule());
            }
            countManager.onModuleUsed(installed.getModule());
        }

    }


    public void commitToServer() {
        if (context.commitAndSync()) {
            needUpdate = true;
        }
    }

    public void resetModelPose() {
        modelRX = 0;
        modelRY = 0;
        tempModelRX = 0;
        tempModelRY = 0;
        modelX = 0;
        modelY = 0;
        modelScale = 1;
        dragStartX = 0;
        dragStartY = 0;
    }

    public void trySetNodePos(float pos) {
        if (context != null) {
            context.trySetSelectedNodePos(pos);
        }
    }

    private void updateRenderContext(ModifyScreenRenderContext moduleRenderContext) {
        if (context != null) {
            context.onModuleTreeRender(moduleRenderContext);
        }
    }

    private void checkAndCreateContext() {
        if (this.context != null) {
            return;
        }
        ItemStack itemStack = checkAndGet();
        if (itemStack != null) {
            this.context = new GunModifyContext(itemStack);
            this.context.setOnSelectionChange(this::onSelectionChange);
        }
    }

    private void onSelectionChange(Node node, SlotInstance slot) {
        if (node == null && slot == null) {
            GunModifyUI.clearItemSlots();
            GunModifyUI.hideRight();
            GunModifyUI.setScrollComponentsVisible(false);
            GunModifyUI.clearSearchBox();
            selectedItemSlot = null;
        } else {
            SlotInstance selectedSlot = node == null ? slot : node.getBelongsTo();
            if (node != null && node.getBelongsToSlot() instanceof IRail rail && !node.isFixedPosition()) {
                GunModifyUI.setScrollComponentsVisible(true);
                GunModifyUI.setScrollPos(rail.getNormalizedChildPosition(node.getUnit()));
            } else {
                GunModifyUI.setScrollComponentsVisible(false);
            }
            GunModifyUI.showRight();
            if (selectedSlot == lastModularSlot) {
                return;
            }
            GunModifyUI.clearSearchBox();
            GunModifyUI.clearItemSlots();
            selectedItemSlot = null;
            lastModularSlot = selectedSlot;
            updateAllSuitableModules(selectedSlot);
            setPage(0);
            updatePageContent();
        }
    }

    private void updateAllSuitableModules(SlotInstance selectedSlot) {
        Map<String, IModular> all = ModuleRegister.all();
        List<IModular> suitableModules = new ArrayList<>();
        for (Map.Entry<String, IModular> entry : all.entrySet()) {
            IModular value = entry.getValue();
            if (selectedSlot.accepts(value)) {
                suitableModules.add(value);
            }
        }
        allSuitableModules.clear();
        for (IModular suitableModule : suitableModules) {
            ModuleItem<?> bindItem = suitableModule.getBindItem();
            ItemStack itemStack = new ItemStack(bindItem);
            allSuitableModules.add(itemStack);
        }
    }

    public void resetScroller() {
        if (context != null && context.getSelectedNode() != null) {
            Node selectedNode = context.getSelectedNode();
            if (selectedNode.getBelongsToSlot() instanceof IRail rail) {
                float normalizedOriginOffest = rail.getNormalizedOriginOffest();
                GunModifyUI.setScrollPos(normalizedOriginOffest);
            }
        }
    }

    public void clearSearch() {
        if (context == null) {
            return;
        }
        Node node = context.getSelectedNode();
        SlotInstance slot = context.getSelectedSlot();
        SlotInstance selectedSlot = node == null ? slot : node.getBelongsTo();
        if (selectedSlot == null) {
            return;
        }
        GunModifyUI.clearSearchBox();
        GunModifyUI.clearItemSlots();
        selectedItemSlot = null;
        updateAllSuitableModules(selectedSlot);
        setPage(0);
        updatePageContent();
    }

    public void searchAttachments(String value) {
        if (context == null) {
            return;
        }

        if (value == null || value.isEmpty()) {
            return;
        }

        // 1. 重新获取当前槽位允许的所有完整模块列表
        SlotInstance selectedSlot = context.getSelectedNode() == null ?
                context.getSelectedSlot() :
                context.getSelectedNode().getBelongsTo();

        if (selectedSlot == null) {
            return;
        }

        // 获取该槽位支持的所有模块
        Map<String, IModular> allModules = ModuleRegister.all();
        List<ItemStack> filteredResults = new ArrayList<>();

        for (IModular module : allModules.values()) {
            if (selectedSlot.accepts(module)) {
                ItemStack stack = new ItemStack(module.getBindItem());

                // 2. 搜索过滤逻辑
                // 匹配物品的本地化名称（例如 "红点瞄准镜"）或 ID（"red_dot"）
                String displayName = stack.getHoverName().getString().toLowerCase();
                String searchTerm = value.toLowerCase();

                if (searchTerm.isEmpty() || displayName.contains(searchTerm)) {
                    filteredResults.add(stack);
                }
            }
        }

        // 3. 更新类成员列表并重置 UI 状态
        this.allSuitableModules.clear();
        this.allSuitableModules.addAll(filteredResults);

        this.page = 1; // 搜索后回到第一页
        this.selectedItemSlot = null; // 清除当前选择

        // 4. 刷新显示
        this.updatePageContent();

        // 如果你的 UI 类里有同步页码的方法，也调用一下
        GunModifyUI.syncPageInfo(this);
        GunModifyUI.clearItemSlots();
    }

    private void updatePageContent() {
        List<List<DisplaySlot>> itemSlots = GunModifyUI.getItemSlots();
        int pageSize = GunModifyUI.PAGE_SIZE;
        for (int i = (page - 1) * pageSize; i < page * pageSize; i++) {
            if (i >= allSuitableModules.size()) {
                break;
            }
            ItemStack itemStack = allSuitableModules.get(i);
            DisplaySlot itemSlot = itemSlots.get(i / 5).get(i % 5);
            itemSlot.setItem(itemStack);
            if (itemStack.getItem() instanceof ModuleItem<?> item) {
                IModular module = item.getModule();
                int moduleLeftCount = countManager.getModuleLeftCount(module);
                itemSlot.setCount(moduleLeftCount);
            }

        }
        GunModifyUI.syncPageInfo(this);
    }


    private ItemStack checkAndGet() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getMainHandItem().getItem() instanceof GunItem) {
            return player.getMainHandItem();
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (needUpdate) {
            return false;
        }
        boolean b = super.mouseClicked(mouseX, mouseY, button);
        if (context != null && !b) {
            if (!context.onMouseClick((float) mouseX, (float) mouseY)) {
                if (!isDraggingModel) {
                    isRollingModel = true;
                }
                dragStartX = (float) mouseX;
                dragStartY = (float) mouseY;
            }
        }
        return b;

    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (isDraggingModel) {
            modelX -= (float) ((pMouseX - dragStartX) * 0.5f);
            modelY -= (float) ((pMouseY - dragStartY) * 0.5f);
            dragStartX = (float) pMouseX;
            dragStartY = (float) pMouseY;
            isRollingModel = false;
        } else if (isRollingModel) {
            modelRY = tempModelRY + (float) (pMouseX - dragStartX) * 0.05f;
            modelRX = tempModelRX + (float) (pMouseY - dragStartY) * 0.05f;
        } else {
            for (Renderable renderable : this.renderables) {
                if (renderable instanceof AbstractWidget widget) {
                    widget.mouseMoved(pMouseX, pMouseY);
                }
            }
        }
        return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (isRollingModel) {
            isRollingModel = false;
            tempModelRY = modelRY % 360;
            tempModelRX = modelRX % 360;
        }
        isDraggingModel = false;
        dragStartX = 0;
        dragStartY = 0;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (needUpdate) {
            super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    public boolean isItemSlotSelected(DisplaySlot testSlot) {
        return selectedItemSlot == testSlot;
    }

}