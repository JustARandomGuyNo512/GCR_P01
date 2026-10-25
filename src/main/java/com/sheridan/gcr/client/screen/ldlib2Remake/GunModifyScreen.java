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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class GunModifyScreen extends Screen {
    private static final ResourceLocation SELECTED_ITEM_SLOT = GCR.RL(GCR.MODID, "textures/gui/component/selected_slot.png");

    private static final float MIN_MODEL_SCALE = 0.5f;
    private static final float MAX_MODEL_SCALE = 2.5f;
    /** 缩放按钮每次变化的量 */
    private static final float MODEL_ZOOM_STEP = 0.2f;
    /** 鼠标滚轮每一格的缩放量 */
    private static final float MODEL_WHEEL_ZOOM_STEP = 0.1f;

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
    private boolean inspectionMode = false;
    private boolean inspectionInteracted = false;
    private boolean inspectionRightDown = false;
    private boolean inspectionLeftDown = false;
    private boolean suppressSearch;
    private String appliedSearch = "";

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
        int maxPage = getMaxPage();
        if (maxPage < 1) {
            this.page = 1;
            return;
        }
        this.page = Mth.clamp(page, 1, maxPage);
    }

    public void showPage(int page) {
        int previous = this.page;
        setPage(page);
        if (this.page != previous) {
            selectedItemSlot = null;
        }
        updatePageContent();
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
        updateInspectionMode();
        updatePageContent();
        handleMsgLog();
        if (context != null) {
            context.onTick();
            Node selectedNode = context.getSelectedNode();
            if (selectedNode != null && selectedNode.getBelongsTo() != null) {
                GunModifyUI.setModuleActionsVisible(true);
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
                GunModifyUI.setModuleActionsVisible(false);
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
            if (!inspectionMode) {
                context.renderComponents(guiGraphics, mouseX, mouseY);
            }
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (selectedItemSlot != null && !inspectionMode) {
            guiGraphics.blit(SELECTED_ITEM_SLOT,
                        (int) (selectedItemSlot.getContentX() - 3),
                        (int) (selectedItemSlot.getContentY() - 3),
                        0,0, 22, 22, 22, 22);

        }
        if (validateMsg != null && msgDelayTick == 0 && !inspectionMode) {
            validateMsg.render(guiGraphics, 0, mouseX, mouseY, false);
        }
        RenderSystem.enableBlend();
        if (needUpdate) {
            guiGraphics.drawCenteredString(
                    this.font, Component.translatable("screen.need_update").getString(), this.width / 2, this.height / 2, 0xffffff);
        }
        renderInspectionOverlay(guiGraphics);
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
            } else if (selectedNode != null && selectedNode.getBelongsTo() != null) {
                SlotInstance belongsTo = selectedNode.getBelongsTo();
                if (belongsTo.allow(OperationType.REPLACE)
                        && module != selectedNode.getModule()
                        && belongsTo.accepts(module)
                        && countManager.getModuleLeftCount(module) > 0) {
                    replaceSelectedModule();
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
        modelScale += MODEL_ZOOM_STEP;
        modelScale = Mth.clamp(modelScale, MIN_MODEL_SCALE, MAX_MODEL_SCALE);
    }

    public void zoomOutModel() {
        modelScale -= MODEL_ZOOM_STEP;
        modelScale = Mth.clamp(modelScale, MIN_MODEL_SCALE, MAX_MODEL_SCALE);
    }

    /**
     * 鼠标滚轮缩放模型，鼠标位于 UI 控件上（例如滚动条、搜索框）时交给控件处理。
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inspectionMode) {
            if (scrollY != 0) {
                markInspectionInteraction();
                modelScale = Mth.clamp(modelScale + (scrollY > 0 ? MODEL_WHEEL_ZOOM_STEP : -MODEL_WHEEL_ZOOM_STEP), MIN_MODEL_SCALE, MAX_MODEL_SCALE);
            }
            return true;
        }
        if (needUpdate) {
            return false;
        }
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (scrollY == 0) {
            return false;
        }
        modelScale = Mth.clamp(
                modelScale + (scrollY > 0 ? MODEL_WHEEL_ZOOM_STEP : -MODEL_WHEEL_ZOOM_STEP),
                MIN_MODEL_SCALE, MAX_MODEL_SCALE);
        return true;
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

    public void enterInspectionMode() {
        inspectionMode = true;
        inspectionInteracted = false;
        resetModelPose();
        if (ui != null) {
            GunModifyUI.setVisible(false);
        }
    }

    private void exitInspectionMode() {
        inspectionMode = false;
        inspectionRightDown = false;
        inspectionLeftDown = false;
        isRollingModel = false;
        isDraggingModel = false;
        if (ui != null) {
            GunModifyUI.setVisible(true);
        }
    }

    private void markInspectionInteraction() {
        inspectionInteracted = true;
    }

    private void updateInspectionMode() {
        if (inspectionMode && !inspectionInteracted && !inspectionLeftDown && !inspectionRightDown) {
            modelRY += 0.01f;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (inspectionMode) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                exitInspectionMode();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                resetModelPose();
                inspectionInteracted = false;
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderInspectionOverlay(GuiGraphics guiGraphics) {
        if (!inspectionMode) {
            return;
        }
        int y = this.height - 32;
        guiGraphics.drawCenteredString(this.font, Component.translatable("screen.modify.inspect_line_1"), this.width / 2, y, 0xFFE7E2D6);
        guiGraphics.drawCenteredString(this.font, Component.translatable("screen.modify.inspect_line_2"), this.width / 2, y + 12, 0xFF9C978C);
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
            GunModifyUI.setWeaponName(itemStack.getHoverName());
        }
    }

    private void onSelectionChange(Node node, SlotInstance slot) {
        if (node == null && slot == null) {
            suppressSearch = true;
            GunModifyUI.clearSearchBox();
            suppressSearch = false;
            appliedSearch = "";
            allSuitableModules.clear();
            page = 1;
            selectedItemSlot = null;
            lastModularSlot = null;
            GunModifyUI.clearItemSlots();
            GunModifyUI.hideRight();
            GunModifyUI.setModuleActionsVisible(false);
            GunModifyUI.showIdlePrompt();
            updatePageContent();
            return;
        }
        SlotInstance selectedSlot = node == null ? slot : node.getBelongsTo();
        if (selectedSlot == null) {
            onSelectionChange(null, null);
            return;
        }
        GunModifyUI.showRight();
        GunModifyUI.showSelection(selectionTitle(node, selectedSlot));
        GunModifyUI.setModuleActionsVisible(node != null);
        if (selectedSlot == lastModularSlot) {
            return;
        }
        suppressSearch = true;
        GunModifyUI.clearSearchBox();
        suppressSearch = false;
        appliedSearch = "";
        GunModifyUI.clearItemSlots();
        selectedItemSlot = null;
        lastModularSlot = selectedSlot;
        updateAllSuitableModules(selectedSlot);
        page = 1;
        updatePageContent();
    }

    private Component selectionTitle(Node node, SlotInstance slot) {
        Component slotName = slotLabel(slot);
        if (node == null) {
            return slotName;
        }
        IModular module = node.getModule();
        if (module == null || module.getBindItem() == null) {
            return slotName;
        }
        return Component.translatable("screen.modify.selected_installed", slotName, new ItemStack(module.getBindItem()).getHoverName());
    }

    private Component slotLabel(SlotInstance slot) {
        String raw = slot.slotName().toLowerCase(Locale.ROOT);
        StringBuilder pretty = new StringBuilder();
        for (String part : raw.split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (!pretty.isEmpty()) {
                pretty.append(' ');
            }
            pretty.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                pretty.append(part.substring(1));
            }
        }
        return Component.translatableWithFallback("slot." + raw, pretty.toString());
    }

    private void updateAllSuitableModules(SlotInstance selectedSlot) {
        allSuitableModules.clear();
        if (selectedSlot == null) {
            return;
        }
        for (IModular module : ModuleRegister.all().values()) {
            if (selectedSlot.accepts(module) && module.getBindItem() != null) {
                allSuitableModules.add(new ItemStack(module.getBindItem()));
            }
        }
    }

    public void clearSearch() {
        suppressSearch = true;
        GunModifyUI.clearSearchBox();
        suppressSearch = false;
        appliedSearch = "";
        restoreSuitableList();
    }

    public void searchAttachments(String value) {
        if (suppressSearch || context == null) {
            return;
        }
        String query = value == null ? "" : value.trim();
        if (query.equals(appliedSearch)) {
            return;
        }
        if (query.isEmpty()) {
            appliedSearch = "";
            restoreSuitableList();
            return;
        }
        SlotInstance selectedSlot = currentSlot();
        if (selectedSlot == null) {
            return;
        }
        appliedSearch = query;
        String searchTerm = query.toLowerCase(Locale.ROOT);
        List<ItemStack> filteredResults = new ArrayList<>();
        for (IModular module : ModuleRegister.all().values()) {
            if (!selectedSlot.accepts(module) || module.getBindItem() == null) {
                continue;
            }
            ItemStack stack = new ItemStack(module.getBindItem());
            String moduleId = module.getID() == null ? "" : module.getID();
            String haystack = (stack.getHoverName().getString() + " " + stack.getItem().getDescriptionId() + " " + moduleId)
                    .toLowerCase(Locale.ROOT);
            if (haystack.contains(searchTerm)) {
                filteredResults.add(stack);
            }
        }
        allSuitableModules.clear();
        allSuitableModules.addAll(filteredResults);
        page = 1;
        selectedItemSlot = null;
        updatePageContent();
    }

    private void restoreSuitableList() {
        if (context == null) {
            return;
        }
        SlotInstance selectedSlot = currentSlot();
        if (selectedSlot == null) {
            return;
        }
        selectedItemSlot = null;
        updateAllSuitableModules(selectedSlot);
        page = 1;
        updatePageContent();
    }

    private SlotInstance currentSlot() {
        if (context == null) {
            return null;
        }
        Node node = context.getSelectedNode();
        return node == null ? context.getSelectedSlot() : node.getBelongsTo();
    }

    private void updatePageContent() {
        List<List<DisplaySlot>> slots = GunModifyUI.getItemSlots();
        if (slots == null || slots.isEmpty()) {
            GunModifyUI.syncPageInfo(this);
            return;
        }
        int columns = GunModifyUI.GRID_COLUMNS;
        int pageSize = GunModifyUI.PAGE_SIZE;
        int start = (page - 1) * pageSize;
        for (int offset = 0; offset < pageSize; offset++) {
            int row = offset / columns;
            int column = offset % columns;
            if (row >= slots.size() || column >= slots.get(row).size()) {
                continue;
            }
            DisplaySlot itemSlot = slots.get(row).get(column);
            int index = start + offset;
            if (index < 0 || index >= allSuitableModules.size()) {
                itemSlot.setItem(ItemStack.EMPTY);
                itemSlot.setCount(0);
                continue;
            }
            ItemStack itemStack = allSuitableModules.get(index);
            itemSlot.setItem(itemStack);
            if (itemStack.getItem() instanceof ModuleItem<?> item) {
                itemSlot.setCount(countManager.getModuleLeftCount(item.getModule()));
            } else {
                itemSlot.setCount(0);
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
        if (inspectionMode) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                inspectionRightDown = true;
                markInspectionInteraction();
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                inspectionLeftDown = true;
                markInspectionInteraction();
            }
            dragStartX = (float) mouseX;
            dragStartY = (float) mouseY;
            tempModelRX = modelRX;
            tempModelRY = modelRY;
            return true;
        }
        if (needUpdate) {
            return false;
        }
        boolean b = super.mouseClicked(mouseX, mouseY, button);
        if (context != null && !b) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && hasControlDown()) {
                isDraggingModel = true;
                isRollingModel = false;
                dragStartX = (float) mouseX;
                dragStartY = (float) mouseY;
                return true;
            }
            if (!context.beginPointer((float) mouseX, (float) mouseY)) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && !isDraggingModel) {
                    isRollingModel = true;
                    dragStartX = (float) mouseX;
                    dragStartY = (float) mouseY;
                }
            } else {
                this.setDragging(true);
            }
            return true;
        }
        return b;

    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (inspectionMode) {
            float dx = (float) (pMouseX - dragStartX);
            float dy = (float) (pMouseY - dragStartY);
            if (inspectionRightDown) {
                modelX -= dx * 0.5f;
                modelY -= dy * 0.5f;
            } else if (inspectionLeftDown) {
                modelRY += dx * 0.05f;
                modelRX += dy * 0.05f;
            }
            dragStartX = (float) pMouseX;
            dragStartY = (float) pMouseY;
            return true;
        }
        if (context != null && context.isPointerDragging()) {
            context.dragPointer((float) pMouseX, (float) pMouseY);
            return true;
        }
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
        if (inspectionMode) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                inspectionRightDown = false;
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                inspectionLeftDown = false;
            }
            tempModelRY = modelRY % 360;
            tempModelRX = modelRX % 360;
            return true;
        }
        if (context != null) {
            context.endPointer();
        }
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