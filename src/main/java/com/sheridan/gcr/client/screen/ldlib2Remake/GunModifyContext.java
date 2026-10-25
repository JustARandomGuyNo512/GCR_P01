package com.sheridan.gcr.client.screen.ldlib2Remake;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.model.BoneRenderStatus;
import com.sheridan.gcr.client.model.BufferedBoneMeshModel;
import com.sheridan.gcr.client.screen.Utils;
import com.sheridan.gcr.client.render.ModifyScreenRenderContext;
import com.sheridan.gcr.client.render.ModuleRenderNode;
import com.sheridan.gcr.client.render.RenderTypes;
import com.sheridan.gcr.client.render.VoxelShapeRenderer;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.IVoxel;
import com.sheridan.gcr.modularSys.ModuleHandler;
import com.sheridan.gcr.modularSys.builder.*;
import com.sheridan.gcr.modularSys.modules.IVoxelHandlerModule;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.slot.IRail;
import com.sheridan.gcr.network.c2s.CommitModuleTreePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

@OnlyIn(Dist.CLIENT)
public class GunModifyContext {
    private static final ResourceLocation SLOT_BOUNDARY = GCR.RL(GCR.MODID, "textures/gui/screen/slot_boundary.png");

    private static final Vector3f OUT_SCREEN = new Vector3f(Float.NaN, Float.NaN, Float.NaN);

    /** 判断导轨朝向时沿导轨采样的距离（单位：方块） */
    private static final float RAIL_DIRECTION_SAMPLE_STEP = 0.05f;
    /** 导轨在屏幕上的最小水平位移（像素），低于该值认为水平方向无法判断 */
    private static final float RAIL_DIRECTION_MIN_SCREEN_DELTA = 0.5f;

    public ItemStack itemStack;
    public IGun gun;
    public ModuleRenderNode renderRoot;
    public Node nodeRoot;
    public IBuilder builder;
    public Accessor accessor;

    public boolean renderNodeIcon = true;
    public boolean renderSlotIcon = true;

    private final Map<ModuleRenderNode, Pair<ModuleRenderNode, Vector3f>> renderNodeScreenPos = new HashMap<>();
    private final Map<SlotInstance, Pair<SlotInstance, Vector3f>> slotScreenPos = new HashMap<>();
    private final Map<SlotInstance, Matrix4f> slotRenderPos = new HashMap<>();
    private final Map<ModuleRenderNode, Node> renderNodeToNode = new HashMap<>();


    private ModuleRenderNode selectedRenderNode;
    private Node selectedNode;
    private SlotInstance selectedSlot;

    private final CollisionRes collisionRes;

    private final Set<String> showBoundingBox;
    private final Set<String> showSlotBoundary;

    private ValidateResult validateResult;

    private boolean treeModified;
    private int lastMutateId;

    private boolean collisionDirty = false;

    /**
     * 导轨移动方向：
     * +1 表示 scrollbar 值增大时模块在屏幕上向右移动，可以直接映射；
     * -1 表示模块在屏幕上向左移动，需要把 scrollbar 的值反转后再映射到导轨上。
     */
    private int railScrollSign = 1;

    private static final float NODE_HIT = 10f;
    private static final float SLOT_HIT = 10f;
    private static final float ICON = 18f;

    private enum DragMode { NONE, HOLD, RAIL }

    private final List<Callout> callouts = new ArrayList<>();
    private final List<DragRect> dragRects = new ArrayList<>();
    private String hoveredDragId;
    private DragMode dragMode = DragMode.NONE;
    private String draggingKey;
    private float pressX;
    private float pressY;
    private float lastPointerX;
    private float lastPointerY;
    private boolean dragPassedThreshold;
    private boolean reclickSelectedRail;
    private float railAxisX;
    private float railAxisY;
    private float railNormPerPixel;
    private boolean railAxisValid;
    private String railAxisNodeId;

    public GunModifyContext(ItemStack itemStack) {
        if (itemStack.getItem() instanceof GunItem gunItem) {
            this.itemStack = itemStack;
            this.gun = gunItem.getGun();

            builder = new Builder();
            IGun gun = gunItem.getGun();
            ListTag modulesTag = gun.getModulesTag(itemStack).copy();
            builder.initAndClear(modulesTag);
            validateResult = builder.checkWorkspace(true);
            if (validateResult.isCommitAllowed()) {
                builder.commit();
            }
            validateResult.sortIssues(false);

            renderRoot = ModuleHandler.buildRenderTree(itemStack);

            nodeRoot = ((WorkSpace)builder.getWorkspace()).getRoot();

            accessor = (Accessor) builder.getWriteableAccessor();
            lastMutateId = builder.getWorkspace().getMutateId();
            showBoundingBox = new HashSet<>();
            showSlotBoundary = new HashSet<>();
            collisionRes = new CollisionRes();
        } else {
            throw new IllegalArgumentException("ItemStack is not a gun");
        }
    }

    public Node getSelectedNode() {
        return selectedNode;
    }

    public SlotInstance getSelectedSlot() {
        return selectedSlot;
    }


    public boolean addUnit(IModular modular) {
        final boolean[] added = {false};
        builder.getWorkspace().addChild(selectedSlot, modular).ifPresentOrElse(unit -> {
            Node node = ((WorkSpace) builder.getWorkspace()).getNode(unit);
            if (node != null) {
                selectedNode = node;
                selectedSlot = null;
                rebuildRenderTree();
                treeModified = builder.diff();
                added[0] = true;
                builder.getCollisionHandler().update();
                onSelectionChange.accept(node, null);
            }
        }, () -> added[0] = false);
        return added[0];
    }

    public ValidateResult getValidateResult() {
        return validateResult;
    }

    private @NotNull Map<String, ModuleRenderNode> getModuleRenderNodeMap(ModifyScreenRenderContext context) {
        ModuleRenderNode root = context.getRoot();
        Map<String, ModuleRenderNode> renderNodes = new HashMap<>();
        Map<ModuleRenderNode, Map<String, BoneRenderStatus>> tempPoseMap = context.getRenderStatusMap();
        renderNodeScreenPos.clear();
        root.dfsTravel(node -> {
            if (node != root) {
                Map<String, BoneRenderStatus> stringPoseMap = tempPoseMap.get(node);
                if (stringPoseMap == null) {
                    return;
                }
                BoneRenderStatus rootPose = stringPoseMap.get("root");
                if (rootPose != null) {
                    Vector3f screenPos = getScreenPos(rootPose.pose.pose());
                    renderNodeScreenPos.put(node, Pair.of(node, screenPos));
                }
            }
            renderNodes.put(node.id, node);
        });
        return renderNodes;
    }

    public void onModuleTreeRender(ModifyScreenRenderContext context) {
        Map<String, ModuleRenderNode> renderNodes = getModuleRenderNodeMap(context);
        slotScreenPos.clear();
        renderNodeToNode.clear();
        slotRenderPos.clear();
        dragRects.clear();
        Map<ModuleRenderNode, Map<String, BoneRenderStatus>> tempPoseMap = context.getRenderStatusMap();
        nodeRoot.dfs((n -> {
            String inTimeID = n.getID();
            ModuleRenderNode node = renderNodes.get(inTimeID);
            if (node != null) {
                Map<String, BoneRenderStatus> stringPoseMap = tempPoseMap.get(node);
                if (stringPoseMap == null) {
                    return;
                }
                renderNodeToNode.put(node, n);
                List<SlotInstance> slots = n.getSlots();
                for (SlotInstance slotInstance : slots) {
                    String slotName = slotInstance.slotName();
                    BoneRenderStatus bonePose = stringPoseMap.get(slotName);
                    if (bonePose != null) {
                        Vector3f screenPos = getScreenPos(bonePose.pose.pose());
                        slotScreenPos.put(slotInstance, Pair.of(slotInstance, screenPos));
                        slotRenderPos.put(slotInstance, bonePose.pose.pose());
                    }
                }
            }
        }));

        updateRailScrollSign();
        updateRailAxisCache();
        renderNodeToNode.forEach((renderNode, node) -> capturePickRect(renderNode, node, tempPoseMap.get(renderNode)));

        context.getRoot().dfsTravel(n -> {
            Node node = renderNodeToNode.get(n);
            if (node == null) {
                return;
            }
            if (showBoundingBox.contains(n.id)) {
                if (node.getModule() instanceof IVoxelHandlerModule module) {
                    Map<String, BoneRenderStatus> stringPoseMap = tempPoseMap.get(n);
                    Matrix4f pose = stringPoseMap == null ?
                            n.model.getRootPose().pose() :
                            stringPoseMap.get("root").pose.pose();
                    IVoxel voxel = module.getHandler().getVoxel(node.getUnit(), accessor);
                    if (voxel != null) {
                        VoxelShapeRenderer.renderRed(voxel, pose, context.bufferSource);
                    }
                }
            }
            if (showSlotBoundary.contains(n.id)) {
                if (node.getBelongsToSlot() instanceof IRail rail) {
                    Matrix4f matrix4f = slotRenderPos.get(node.getBelongsTo());
                    if (matrix4f != null) {
                        float pivot = rail.getNormalizedOriginOffest();
                        float length = rail.getLength();
                        VertexConsumer buffer = context.bufferSource.getBuffer(RenderTypes.getMuzzleFlash(SLOT_BOUNDARY));
                        Matrix4f rear = new Matrix4f(matrix4f);
                        rear.translate(0, 0, pivot * length);
                        rear.scale(0.5f);
                        Matrix4f far = new Matrix4f(matrix4f);
                        far.translate(0, 0, - (1 - pivot) * length);
                        far.scale(0.5f);
                        Direction direction = node.getBelongsToSlot().getDirection();
                        drawQuad(buffer, rear, 1, 0.5f, 0, direction);
                        drawQuad(buffer, far, 1, 0.5f, 0, direction);
                    }
                }
            }
        });

    }

    private void drawQuad(VertexConsumer builder, Matrix4f matrix, float r, float g, float b, Direction direction) {
        float a1 = direction == Direction.UPPER ? 1 : 0;
        float a2 = direction == Direction.UPPER ? 0 : 1;
        float offsetY = direction == Direction.UPPER ? 0 : -0.8f;
        builder.addVertex(matrix, -0.5f, -0.1f + offsetY, 0.0F).setColor(r, g, b, a1).setUv(0, 1).setLight(157288880).setOverlay(OverlayTexture.NO_OVERLAY);
        builder.addVertex(matrix, 0.5f, -0.1f + offsetY, 0.0F).setColor(r, g, b, a1).setUv(1, 1).setLight(157288880).setOverlay(OverlayTexture.NO_OVERLAY);
        builder.addVertex(matrix, 0.5f, 0.9f + offsetY, 0.0F).setColor(r, g, b, a2).setUv(1, 0).setLight(157288880).setOverlay(OverlayTexture.NO_OVERLAY);
        builder.addVertex(matrix, -0.5f, 0.9f + offsetY, 0.0F).setColor(r, g, b, a2).setUv(0, 0).setLight(157288880).setOverlay(OverlayTexture.NO_OVERLAY);
    }

    public void rebuildRenderTree() {
        ModuleRenderNode moduleRenderNode = ModuleHandler.buildRenderTreeByNode(nodeRoot);
        renderNodeScreenPos.clear();
        renderRoot = moduleRenderNode;
        renderNodeToNode.clear();
        showBoundingBox.clear();
        clearAndRemapSelectedRenderNode();
    }

    public void clearAndRemapSelectedRenderNode() {
        selectedRenderNode = null;
        if (selectedNode != null) {
            renderRoot.dfs(node -> {
                if (selectedRenderNode == null && selectedNode.getID().equals(node.id)) {
                    selectedRenderNode = node;
                    return false;
                }
                return true;
            });
        }
    }


    /**
     * scrollbar 的值 -> 模块在导轨上的位置。
     * 模型被旋转后模块在屏幕上的移动方向会和 scrollbar 滑块相反，
     * 这里根据当前视角下导轨的朝向决定是否需要把值反转。
     */
    public void trySetSelectedNodePos(float scrollValue) {
        if (selectedNode == null || selectedNode.isFixedPosition() || selectedRenderNode == null) {
            return;
        }
        if (selectedNode.getBelongsToSlot() instanceof IRail rail) {
            float originalPos = selectedRenderNode.z;
            rail.setChildPosition(selectedNode.getUnit(), accessor, scrollValueToRailPos(scrollValue));
            selectedRenderNode.z = selectedNode.getUnit().getZOffset();
            if (Math.abs(originalPos - selectedRenderNode.z) >= 1e-5) {
                treeModified = true;
                collisionDirty = true;
                builder.getWorkspace().increaseMutateId();
            }
        }
    }

    /** 导轨上的归一化位置 -> scrollbar 的值（与 {@link #scrollValueToRailPos} 互为逆运算） */
    public float railPosToScrollValue(float railPos) {
        return railScrollSign >= 0 ? railPos : 1f - railPos;
    }

    /** scrollbar 的值 -> 导轨上的归一化位置 */
    public float scrollValueToRailPos(float scrollValue) {
        return railScrollSign >= 0 ? scrollValue : 1f - scrollValue;
    }

    /**
     * 根据当前渲染出的槽位姿态更新导轨的移动方向。
     * 槽位骨骼的局部 +Z 是模块移动的正方向，而导轨位置增大时模块向局部 -Z 移动，
     * 所以采样两点比较它们在屏幕上的水平位移即可得到方向和 scrollbar 是否一致。
     */
    private void updateRailScrollSign() {
        if (selectedNode == null || selectedNode.isFixedPosition()) {
            return;
        }
        if (!(selectedNode.getBelongsToSlot() instanceof IRail)) {
            return;
        }
        SlotInstance slot = selectedNode.getBelongsTo();
        if (slot == null) {
            return;
        }
        Matrix4f slotPose = slotRenderPos.get(slot);
        if (slotPose == null) {
            return;
        }
        int sign = computeRailScrollSign(slotPose, selectedNode.getUnit().getZOffset());
        if (sign != 0 && sign != railScrollSign) {
            railScrollSign = sign;
        }
    }

    /**
     * 返回 +1 表示导轨位置增大时模块在屏幕上向右移动，-1 表示向左移动，0 表示无法判断。
     */
    private int computeRailScrollSign(Matrix4f slotPose, float zOffset) {
        Matrix4f from = new Matrix4f(slotPose).translate(0, 0, zOffset);
        Matrix4f to = new Matrix4f(slotPose).translate(0, 0, zOffset - RAIL_DIRECTION_SAMPLE_STEP);
        Vector3f fromScreen = getScreenPos(from);
        Vector3f toScreen = getScreenPos(to);
        if (fromScreen == OUT_SCREEN || toScreen == OUT_SCREEN) {
            return 0;
        }
        float deltaX = toScreen.x - fromScreen.x;
        if (Math.abs(deltaX) < RAIL_DIRECTION_MIN_SCREEN_DELTA) {
            return 0;
        }
        return deltaX > 0 ? 1 : -1;
    }

    public void renderComponents(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        hoveredDragId = hitDragRect(mouseX, mouseY);
        renderCallouts(guiGraphics);
        renderNodeIcons(guiGraphics);
        drawDragHighlight(guiGraphics);
    }

    public void onTick() {
        if (treeModified) {
            builder.getCollisionHandler().update();
        }
        doCollisionCheck();
        collisionDirty = false;
        if (lastMutateId != builder.getWorkspace().getMutateId()) {
            validateResult = builder.checkWorkspace(false);
            validateResult.sortIssues(false);
            lastMutateId = builder.getWorkspace().getMutateId();
        }
        if (selectedNode != null && renderRoot != null) {
            renderRoot.dfs(node -> {
                if (selectedNode.getID().equals(node.id)) {
                    selectedRenderNode = node;
                    return false;
                }
                return true;
            });
        }
    }

    public boolean treeModified() {
        return treeModified;
    }

    public boolean allowSave() {
        if (treeModified) {
            return validateResult.isCommitAllowed() && collisionRes.isOk();
        }
        return false;
    }

    private WorkSpace getWorkSpace() {
        return (WorkSpace) builder.getWorkspace();
    }

    private void doCollisionCheck() {
        collisionRes.clear();
        ICollisionHandler collisionHandler = builder.getCollisionHandler();
        WorkSpace workSpace = getWorkSpace();
        workSpace.getSequencedUnits().forEach(unit -> {
            String thisID = workSpace.getUnitId(unit);
            List<Unit> collision = collisionHandler.collision(unit);
            Pair<Boolean, Boolean> booleanBooleanPair = collisionHandler.collisionWithRailBoundary(unit);
            for (Unit other : collision) {
                collisionRes.add(thisID, workSpace.getUnitId(other));
            }
            if (booleanBooleanPair.getLeft() || booleanBooleanPair.getRight()) {
                collisionRes.add(thisID, booleanBooleanPair);
            }
        });
        updateCollisionResShowOn();
    }

    private void updateCollisionResShowOn() {
        showBoundingBox.clear();
        showSlotBoundary.clear();
        collisionRes.getNodesToNodes().forEach(pair -> {
            showBoundingBox.add(pair.getLeft());
            showBoundingBox.add(pair.getRight());
        });
        Map<String, Pair<Boolean, Boolean>> nodesWithSelfBoundary = collisionRes.nodesWithSelfBoundary;
        showSlotBoundary.addAll(nodesWithSelfBoundary.keySet());
        showBoundingBox.addAll(nodesWithSelfBoundary.keySet());
    }

    public boolean onMouseClick(float mouseX, float mouseY) {
        boolean clickedNode = clickRenderNode(mouseX, mouseY);
        if (clickedNode) {
            return true;
        } else {
            return clickSlot(mouseX, mouseY);
        }
    }

    public void playDownSound(SoundManager handler) {
        handler.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }


    private BiConsumer<Node, SlotInstance> onSelectionChange = (node, slot) -> {};

    public void setOnSelectionChange(BiConsumer<Node, SlotInstance> onSelectionChange) {
        this.onSelectionChange = onSelectionChange;
    }



    public boolean clickRenderNode(float mouseX, float mouseY) {
        if (!renderNodeIcon) {
            return false;
        }
        for (Pair<ModuleRenderNode, Vector3f> pair : renderNodeScreenPos.values()) {
            Vector3f pos = pair.getValue();
            if (pos == OUT_SCREEN) {
                continue;
            }
            ModuleRenderNode node = pair.getKey();
            float dx = pos.x - mouseX;
            float dy = pos.y - mouseY;
            double dis = Math.sqrt(dx * dx + dy * dy);
            if (dis < NODE_HIT) {
                if (selectedRenderNode == node) {
                    selectedRenderNode = null;
                    selectedNode = null;
                    onSelectionChange.accept(null, selectedSlot);
                } else {
                    selectedRenderNode = node;
                    Node newSelected = renderNodeToNode.get(node);
                    if (selectedSlot != null) {
                        selectedSlot = null;
                    }
                    selectedNode = newSelected;
                    onSelectionChange.accept(selectedNode, null);
                }
                playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }
        }
        return false;
    }

    public boolean clickSlot(float mouseX, float mouseY) {
        if (!renderSlotIcon) {
            return false;
        }
        for (Pair<SlotInstance, Vector3f> pair : slotScreenPos.values()) {
            if (pair.getLeft().isHidden() || isSlotOccupied(pair.getLeft())) {
                continue;
            }
            Vector3f pos = pair.getValue();
            if (pos == OUT_SCREEN) {
                continue;
            }
            SlotInstance slot = pair.getKey();
            float y = pos.y + 6;
            float dx = pos.x - mouseX;
            float dy = y - mouseY;
            if (Math.sqrt(dx * dx + dy * dy) < SLOT_HIT) {
                if (selectedSlot == slot) {
                    selectedSlot = null;
                    onSelectionChange.accept(selectedNode, null);
                } else {
                    if (selectedNode != null) {
                        selectedRenderNode = null;
                        selectedNode = null;
                    }
                    selectedSlot = slot;
                    onSelectionChange.accept(null, selectedSlot);
                }
                playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }
        }
        return false;
    }

    private void renderNodeIcons(GuiGraphics guiGraphics) {
        if (renderNodeIcon) {
            for (Pair<ModuleRenderNode, Vector3f> pair : renderNodeScreenPos.values()) {
                Vector3f pos = pair.getValue();
                if (pos == OUT_SCREEN) {
                    continue;
                }
                ModuleRenderNode key = pair.getKey();
                Node node = renderNodeToNode.get(key);
                boolean hot = key == selectedRenderNode || (node != null && node.getID().equals(hoveredDragId));
                boolean warn = node != null && collisionRes.collidesWithNode(node.getID());
                markPoint(guiGraphics, pos.x, pos.y, hot, warn);
            }
        }

        if (renderSlotIcon) {
            for (Pair<SlotInstance, Vector3f> pair : slotScreenPos.values()) {
                if (pair.getLeft().isHidden() || isSlotOccupied(pair.getLeft())) {
                    continue;
                }
                Vector3f pos = pair.getValue();
                if (pos == OUT_SCREEN) {
                    continue;
                }
                markPoint(guiGraphics, pos.x, pos.y + 6, pair.getKey() == selectedSlot, false);
            }
        }
    }

    /**
     * 槽位已经被占满（不能再放入模块）。
     * 占满的槽位不再渲染也不可点击，避免玩家误以为还能继续添加模块。
     */
    public boolean isSlotOccupied(SlotInstance slot) {
        return getWorkSpace().getNodes(slot).size() >= slot.getSlot().maxCapacity();
    }

    private Vector3f getScreenPos(Matrix4f matrix4f) {
        return project(matrix4f, 0f, 0f, 0f);
    }

    private Vector3f project(Matrix4f matrix4f, float x, float y, float z) {
        Matrix4f m0 = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f m1 = new Matrix4f(RenderSystem.getProjectionMatrix());
        Vector4f vector4f = new Matrix4f(matrix4f).transform(new Vector4f(x, y, z, 1.0F));
        Vector4f v = vector4f.mul(m0).mul(m1);
        if (Math.abs((v.x / v.w)) > 1 || Math.abs((v.y / v.w)) > 1 || v.z < 0.0075f) {
            return OUT_SCREEN;
        }
        float w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        float h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        float screenX = ((v.x / v.w) * w + w) * 0.5f;
        float screenY = (-(v.y / v.w) * h + h) * 0.5f;
        return new Vector3f(screenX, screenY, v.z);
    }

    private void capturePickRect(ModuleRenderNode renderNode, Node node, Map<String, BoneRenderStatus> poses) {
        if (node == null || node == nodeRoot) {
            return;
        }
        ScreenBounds bounds = new ScreenBounds();
        if (!projectVoxel(node, bounds)) {
            projectMesh(renderNode, poses, bounds);
        }
        if (bounds.hits == 0) {
            return;
        }
        dragRects.add(new DragRect(node.getID(), renderNode,
                bounds.minX - 3f, bounds.minY - 3f, bounds.maxX + 3f, bounds.maxY + 3f, bounds.depth / bounds.hits));
    }

    private boolean projectVoxel(Node node, ScreenBounds bounds) {
        if (!(node.getModule() instanceof IVoxelHandlerModule module) || module.getHandler() == null) {
            return false;
        }
        Matrix4f slotPose = slotRenderPos.get(node.getBelongsTo());
        if (slotPose == null) {
            return false;
        }
        IVoxel voxel = module.getHandler().getVoxel(node.getUnit(), accessor);
        AABB box = voxel == null ? null : voxel.getBoundingBox();
        if (!usableBox(box)) {
            return false;
        }
        Matrix4f pose = new Matrix4f(slotPose).translate(0f, 0f, node.getUnit().getZOffset());
        if (node.shouldReverseModel()) {
            pose.rotateZ((float) Math.PI);
        }
        projectBox(pose, box, bounds);
        return bounds.hits > 0;
    }

    private void projectMesh(ModuleRenderNode renderNode, Map<String, BoneRenderStatus> poses, ScreenBounds bounds) {
        if (poses == null || !(renderNode.model instanceof BufferedBoneMeshModel mesh) || mesh.rootBone == null) {
            return;
        }
        mesh.rootBone.deptFirstTravel(bone -> {
            BoneRenderStatus status = poses.get(bone.name);
            if (status == null || !status.visible) {
                return;
            }
            AABB box = mesh.localBoneBounds(bone);
            if (!usableBox(box)) {
                return;
            }
            projectBox(status.pose.pose(), box, bounds);
        });
    }

    private void projectBox(Matrix4f pose, AABB box, ScreenBounds bounds) {
        for (double x : new double[]{box.minX, box.maxX}) {
            for (double y : new double[]{box.minY, box.maxY}) {
                for (double z : new double[]{box.minZ, box.maxZ}) {
                    Vector3f screen = project(pose, (float) x, (float) y, (float) z);
                    if (screen == OUT_SCREEN) {
                        continue;
                    }
                    bounds.hits++;
                    bounds.minX = Math.min(bounds.minX, screen.x);
                    bounds.minY = Math.min(bounds.minY, screen.y);
                    bounds.maxX = Math.max(bounds.maxX, screen.x);
                    bounds.maxY = Math.max(bounds.maxY, screen.y);
                    bounds.depth += screen.z;
                }
            }
        }
    }

    private static boolean usableBox(AABB box) {
        if (box == null) {
            return false;
        }
        return box.maxX - box.minX > 1.0e-4
                || box.maxY - box.minY > 1.0e-4
                || box.maxZ - box.minZ > 1.0e-4;
    }

    private String hitDragRect(float mouseX, float mouseY) {
        DragRect best = null;
        for (int i = dragRects.size() - 1; i >= 0; i--) {
            DragRect rect = dragRects.get(i);
            if (mouseX < rect.minX || mouseX > rect.maxX || mouseY < rect.minY || mouseY > rect.maxY) {
                continue;
            }
            if (best == null || rect.depth < best.depth - 0.002f
                    || (Math.abs(rect.depth - best.depth) <= 0.002f && rect.area() < best.area())) {
                best = rect;
            }
        }
        return best == null ? null : best.nodeId;
    }

    private boolean pressDragRect(float mouseX, float mouseY) {
        String hit = hitDragRect(mouseX, mouseY);
        if (hit == null) {
            return false;
        }
        for (DragRect rect : dragRects) {
            if (!hit.equals(rect.nodeId)) {
                continue;
            }
            Node node = renderNodeToNode.get(rect.renderNode);
            boolean rail = canDrag(node);
            boolean wasSelected = selectedNode != null && hit.equals(selectedNode.getID());
            if (wasSelected && !rail) {
                clearSelection();
                return true;
            }
            if (!wasSelected) {
                selectNode(rect.renderNode);
            }
            if (selectedNode == null) {
                return true;
            }
            draggingKey = "n:" + selectedNode.getID();
            dragMode = rail ? DragMode.RAIL : DragMode.HOLD;
            pressX = lastPointerX = mouseX;
            pressY = lastPointerY = mouseY;
            dragPassedThreshold = false;
            reclickSelectedRail = wasSelected && rail;
            return true;
        }
        return false;
    }

    /** A rail part can move when its bounds hitbox is not jammed against both ends. */
    private boolean canDrag(Node node) {
        if (!isRailNode(node)) {
            return false;
        }
        Pair<Boolean, Boolean> bounds = collisionRes.wasNodeCollidedBoundary(node.getID());
        return !(bounds.getLeft() && bounds.getRight());
    }

    private boolean isHoveredCallout(Callout callout) {
        return hoveredDragId != null && callout.key.equals("n:" + hoveredDragId);
    }

    private void drawDragHighlight(GuiGraphics graphics) {
        String selectedId = selectedNode == null ? null : selectedNode.getID();
        for (DragRect rect : dragRects) {
            boolean hover = rect.nodeId.equals(hoveredDragId);
            boolean selected = rect.nodeId.equals(selectedId);
            if (!hover && !selected) {
                continue;
            }
            int x0 = Math.round(rect.minX);
            int y0 = Math.round(rect.minY);
            int x1 = Math.round(rect.maxX);
            int y1 = Math.round(rect.maxY);
            if (hover) {
                graphics.fill(x0, y0, x1, y1, 0x33F2D48A);
            }
            int color = hover ? 0xFFF2D48A : 0xA6F2D48A;
            graphics.fill(x0, y0, x1, y0 + 1, color);
            graphics.fill(x0, y1 - 1, x1, y1, color);
            graphics.fill(x0, y0, x0 + 1, y1, color);
            graphics.fill(x1 - 1, y0, x1, y1, color);
        }
    }

    private static final class ScreenBounds {
        private float minX = Float.MAX_VALUE;
        private float minY = Float.MAX_VALUE;
        private float maxX = -Float.MAX_VALUE;
        private float maxY = -Float.MAX_VALUE;
        private float depth;
        private int hits;
    }

    private static final class DragRect {
        private final String nodeId;
        private final ModuleRenderNode renderNode;
        private final float minX;
        private final float minY;
        private final float maxX;
        private final float maxY;
        private final float depth;

        private DragRect(String nodeId, ModuleRenderNode renderNode, float minX, float minY, float maxX, float maxY, float depth) {
            this.nodeId = nodeId;
            this.renderNode = renderNode;
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.depth = depth;
        }

        private float area() {
            return Math.max(0f, maxX - minX) * Math.max(0f, maxY - minY);
        }
    }

    public List<IModular> removeSelectedNode() {
        if (selectedNode != null) {
            if (builder.getWorkspace().removeChild(selectedNode.getUnit())) {
                selectedSlot = selectedNode.getBelongsTo();
                List<IModular> temp = new ArrayList<>();
                selectedNode.dfs(n -> temp.add(n.getModule()));
                rebuildRenderTree();
                selectedNode = null;
                selectedRenderNode = null;
                treeModified = builder.diff();
                builder.getCollisionHandler().update();
                onSelectionChange.accept(null, selectedSlot);
                return temp;
            }
        }
        return List.of();
    }

    public boolean commitAndSync() {
        if (treeModified && !collisionDirty) {
            ValidateResult res = builder.checkWorkspace(true);
            if (res.isCommitAllowed()) {
                builder.commit();
                ListTag write = builder.getWarehouse().write();
                PacketDistributor.sendToServer(new CommitModuleTreePacket(write));
                return true;
            }
        }
        return false;
    }

    public Pair<Unit, List<Unit>> replaceSelectNode(IModular modular) {
        AtomicReference<Pair<Unit, List<Unit>>> res = new AtomicReference<>(
                Pair.of(null, List.of())
        );
        if (selectedNode != null && modular != null) {
            WorkSpace workspace = getWorkSpace();
            Unit old = selectedNode.getUnit();
            workspace.replaceChild(old, modular).ifPresent(
                    pair -> {
                        Unit unit = pair.getLeft();
                        Node node = workspace.getNode(unit);
                        if (node != null) {
                            selectedNode = node;
                            selectedSlot = null;
                            onSelectionChange.accept(selectedNode, null);
                            rebuildRenderTree();
                            treeModified = builder.diff();
                            builder.getCollisionHandler().update();
                            res.set(pair);
                        }
                    });
        }
        return res.get();
    }

    public boolean beginPointer(float mouseX, float mouseY) {
        for (int i = callouts.size() - 1; i >= 0; i--) {
            Callout callout = callouts.get(i);
            if (mouseX < callout.x - 3 || mouseX > callout.x + callout.w + 3
                    || mouseY < callout.y - 3 || mouseY > callout.y + callout.h + 3) {
                continue;
            }
            if (callout.renderNode != null) {
                selectNode(callout.renderNode);
            } else if (callout.slot != null) {
                selectSlot(callout.slot);
            }
            draggingKey = callout.key;
            dragMode = callout.rail ? DragMode.RAIL : DragMode.HOLD;
            pressX = lastPointerX = mouseX;
            pressY = lastPointerY = mouseY;
            dragPassedThreshold = false;
            reclickSelectedRail = false;
            return true;
        }
        return pressDragRect(mouseX, mouseY);
    }

    public boolean isPointerDragging() {
        return dragMode != DragMode.NONE;
    }

    public void dragPointer(float x, float y) {
        if (dragMode == DragMode.NONE) {
            return;
        }
        if (!dragPassedThreshold) {
            float dx = x - pressX;
            float dy = y - pressY;
            if (dx * dx + dy * dy < 9f) {
                return;
            }
            dragPassedThreshold = true;
        }
        float dx = x - lastPointerX;
        float dy = y - lastPointerY;
        if (dragMode == DragMode.RAIL) {
            slideSelectedRail(dx, dy);
        }
        lastPointerX = x;
        lastPointerY = y;
    }

    public void endPointer() {
        if (reclickSelectedRail && !dragPassedThreshold) {
            clearSelection();
        }
        dragMode = DragMode.NONE;
        draggingKey = null;
        reclickSelectedRail = false;
        dragPassedThreshold = false;
    }

    private void selectNode(ModuleRenderNode renderNode) {
        Node node = renderNodeToNode.get(renderNode);
        if (node == null || (selectedRenderNode == renderNode && selectedNode == node)) {
            return;
        }
        selectedRenderNode = renderNode;
        selectedNode = node;
        selectedSlot = null;
        invalidateRailAxis();
        onSelectionChange.accept(selectedNode, null);
        playDownSound(Minecraft.getInstance().getSoundManager());
    }

    private void selectSlot(SlotInstance slot) {
        if (slot == null || (selectedSlot == slot && selectedNode == null)) {
            return;
        }
        selectedRenderNode = null;
        selectedNode = null;
        selectedSlot = slot;
        invalidateRailAxis();
        onSelectionChange.accept(null, selectedSlot);
        playDownSound(Minecraft.getInstance().getSoundManager());
    }

    private void clearSelection() {
        selectedRenderNode = null;
        selectedNode = null;
        selectedSlot = null;
        invalidateRailAxis();
        onSelectionChange.accept(null, null);
        playDownSound(Minecraft.getInstance().getSoundManager());
    }

    private void invalidateRailAxis() {
        railAxisValid = false;
        railAxisNodeId = null;
    }

    private void updateRailAxisCache() {
        railAxisValid = false;
        railAxisNodeId = null;
        if (!isRailNode(selectedNode) || !(selectedNode.getBelongsToSlot() instanceof IRail rail)) {
            return;
        }
        SlotInstance slot = selectedNode.getBelongsTo();
        Matrix4f slotPose = slot == null ? null : slotRenderPos.get(slot);
        float length = rail.getLength();
        if (slotPose == null || length <= 1.0e-4f) {
            return;
        }
        float z = selectedNode.getUnit().getZOffset();
        Vector3f from = getScreenPos(new Matrix4f(slotPose).translate(0, 0, z));
        Vector3f to = getScreenPos(new Matrix4f(slotPose).translate(0, 0, z - RAIL_DIRECTION_SAMPLE_STEP));
        if (from == OUT_SCREEN || to == OUT_SCREEN) {
            return;
        }
        float sx = to.x - from.x;
        float sy = to.y - from.y;
        float screenLen = (float) Math.sqrt(sx * sx + sy * sy);
        if (screenLen < 0.25f) {
            return;
        }
        railAxisX = sx / screenLen;
        railAxisY = sy / screenLen;
        railNormPerPixel = (RAIL_DIRECTION_SAMPLE_STEP / length) / screenLen;
        railAxisNodeId = selectedNode.getID();
        railAxisValid = true;
    }

    private void slideSelectedRail(float dx, float dy) {
        if (!railAxisValid || !isRailNode(selectedNode) || !selectedNode.getID().equals(railAxisNodeId)) {
            return;
        }
        if (!(selectedNode.getBelongsToSlot() instanceof IRail rail)) {
            return;
        }
        float along = dx * railAxisX + dy * railAxisY;
        float current = rail.getNormalizedChildPosition(selectedNode.getUnit());
        float next = Mth.clamp(current + along * railNormPerPixel, 0f, 1f);
        Pair<Boolean, Boolean> bounds = collisionRes.wasNodeCollidedBoundary(selectedNode.getID());
        if (bounds.getLeft() && next < current) {
            next = current;
        }
        if (bounds.getRight() && next > current) {
            next = current;
        }
        if (Math.abs(next - current) < 1.0e-5f) {
            return;
        }
        trySetSelectedNodePos(railPosToScrollValue(next));
    }

    private boolean isRailNode(Node node) {
        return node != null && !node.isFixedPosition() && node.getBelongsToSlot() instanceof IRail;
    }

    private void renderCallouts(GuiGraphics graphics) {
        rebuildCallouts();
        for (Callout callout : callouts) {
            float centerX = callout.x + callout.w * 0.5f;
            float centerY = callout.y + callout.h * 0.5f;
            float dx = callout.anchorX - centerX;
            float dy = callout.anchorY - centerY;
            float scaleX = dx == 0f ? Float.MAX_VALUE : (callout.w * 0.5f) / Math.abs(dx);
            float scaleY = dy == 0f ? Float.MAX_VALUE : (callout.h * 0.5f) / Math.abs(dy);
            float scale = Math.min(scaleX, scaleY);
            boolean hot = callout.selected || isHoveredCallout(callout);
            int line = callout.warn ? 0xCCE24B4B : (hot ? 0xA6F4F1EA : 0x66C8C2B6);
            drawLeader(graphics, centerX + dx * scale, centerY + dy * scale, callout.anchorX, callout.anchorY, line);
        }
        for (Callout callout : callouts) {
            int left = Math.round(callout.x);
            int top = Math.round(callout.y);
            int size = Math.round(callout.w);
            boolean hot = callout.selected || isHoveredCallout(callout);
            int plate = callout.warn ? 0xF06A2420 : (hot ? 0xF0E7D7A8 : 0xF0E6E0D4);
            int outline = callout.warn ? 0xFFE24B4B : (hot ? 0xFFF2D48A : 0xFF6E6A62);
            graphics.fill(left, top, left + size, top + size, plate);
            graphics.renderOutline(left, top, size, size, outline);
            if (!callout.icon.isEmpty()) {
                float scale = (size - 4f) / 16f;
                graphics.pose().pushPose();
                graphics.pose().translate(left + (size - 16f * scale) / 2f, top + (size - 16f * scale) / 2f, 200f);
                graphics.pose().scale(scale, scale, 1f);
                graphics.renderItem(callout.icon, 0, 0);
                graphics.pose().popPose();
            }
            if (callout.warn) {
                drawWarnBadge(graphics, left + size - 5, top - 2);
            }
        }
    }

    private void rebuildCallouts() {
        callouts.clear();
        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        List<CalloutDraft> drafts = new ArrayList<>();
        if (renderNodeIcon) {
            for (Pair<ModuleRenderNode, Vector3f> pair : renderNodeScreenPos.values()) {
                Vector3f pos = pair.getValue();
                if (pos == OUT_SCREEN) {
                    continue;
                }
                ModuleRenderNode renderNode = pair.getKey();
                Node node = renderNodeToNode.get(renderNode);
                if (node == null) {
                    continue;
                }
                drafts.add(new CalloutDraft("n:" + node.getID(), pos.x, pos.y, nodeIcon(node),
                        isRailNode(node), renderNode == selectedRenderNode, renderNode, null,
                        collisionRes.collidesWithNode(node.getID())));
            }
        }
        if (renderSlotIcon) {
            for (Pair<SlotInstance, Vector3f> pair : slotScreenPos.values()) {
                SlotInstance slot = pair.getLeft();
                if (slot.isHidden() || isSlotOccupied(slot)) {
                    continue;
                }
                Vector3f pos = pair.getValue();
                if (pos == OUT_SCREEN) {
                    continue;
                }
                drafts.add(new CalloutDraft("s:" + System.identityHashCode(slot), pos.x, pos.y + 6, ItemStack.EMPTY,
                        false, slot == selectedSlot && selectedNode == null, null, slot, false));
            }
        }
        if (drafts.isEmpty()) {
            return;
        }
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (CalloutDraft draft : drafts) {
            minX = Math.min(minX, draft.anchorX);
            maxX = Math.max(maxX, draft.anchorX);
            minY = Math.min(minY, draft.anchorY);
            maxY = Math.max(maxY, draft.anchorY);
        }
        float centerX = (minX + maxX) * 0.5f;
        float centerY = (minY + maxY) * 0.5f;
        float pad = ICON * 0.5f + 8f;
        float radiusX = Math.max((maxX - minX) * 0.5f + 86f, 120f);
        float radiusY = Math.max((maxY - minY) * 0.5f + 72f, 88f);
        radiusX = Math.min(radiusX, Math.max(48f, Math.min(centerX - pad, width - centerX - pad)));
        radiusY = Math.min(radiusY, Math.max(40f, Math.min(centerY - 28f - pad, height - centerY - pad)));
        List<CalloutDraft> above = new ArrayList<>();
        List<CalloutDraft> below = new ArrayList<>();
        for (CalloutDraft draft : drafts) {
            (draft.anchorY <= centerY ? above : below).add(draft);
        }
        placeOnOval(above, true, centerX, centerY, radiusX, radiusY);
        placeOnOval(below, false, centerX, centerY, radiusX, radiusY);
    }

    /** Evenly space icons along the top or bottom half of an oval around the weapon. */
    private void placeOnOval(List<CalloutDraft> row, boolean above, float centerX, float centerY, float radiusX, float radiusY) {
        if (row.isEmpty()) {
            return;
        }
        row.sort(Comparator.comparingDouble(draft -> draft.anchorX));
        int count = row.size();
        for (int i = 0; i < count; i++) {
            double along = (i + 0.5) / count;
            double theta = above ? Math.PI + along * Math.PI : Math.PI - along * Math.PI;
            float x = centerX + radiusX * (float) Math.cos(theta) - ICON * 0.5f;
            float y = centerY + radiusY * (float) Math.sin(theta) - ICON * 0.5f;
            CalloutDraft draft = row.get(i);
            callouts.add(new Callout(draft.key, draft.anchorX, draft.anchorY, x, y, ICON, ICON, draft.icon,
                    draft.rail, draft.selected, draft.renderNode, draft.slot, draft.warn));
        }
    }

    private static ItemStack nodeIcon(Node node) {
        IModular module = node.getModule();
        if (module != null && module.getBindItem() != null) {
            return new ItemStack(module.getBindItem());
        }
        return ItemStack.EMPTY;
    }

    private static void drawLeader(GuiGraphics graphics, float x1, float y1, float x2, float y2, int color) {
        Utils.drawLine(graphics, x1, y1, x2, y2, 1.25f, color);
    }

    /** A few pixels, just enough to see where the part sits. */
    private static void markPoint(GuiGraphics graphics, float x, float y, boolean selected, boolean warn) {
        int cx = Math.round(x);
        int cy = Math.round(y);
        int color = warn ? 0xFFE24B4B : (selected ? 0xFFF2D48A : 0xFFF4F1EA);
        int radius = warn ? 2 : 1;
        graphics.fill(cx - radius, cy - radius, cx + radius, cy + radius, color);
    }

    /** Red badge with a small exclamation, drawn over the corner of a colliding icon. */
    private static void drawWarnBadge(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 7, y + 8, 0xFFE24B4B);
        graphics.fill(x + 3, y + 1, x + 4, y + 5, 0xFFFFFFFF);
        graphics.fill(x + 3, y + 6, x + 4, y + 7, 0xFFFFFFFF);
    }

    private record CalloutDraft(String key, float anchorX, float anchorY, ItemStack icon, boolean rail, boolean selected,
                                ModuleRenderNode renderNode, SlotInstance slot, boolean warn) {}

    private static final class Callout {
        private final String key;
        private final float anchorX;
        private final float anchorY;
        private final float x;
        private final float y;
        private final float w;
        private final float h;
        private final ItemStack icon;
        private final boolean rail;
        private final boolean selected;
        private final boolean warn;
        private final ModuleRenderNode renderNode;
        private final SlotInstance slot;

        private Callout(String key, float anchorX, float anchorY, float x, float y, float w, float h, ItemStack icon,
                         boolean rail, boolean selected, ModuleRenderNode renderNode, SlotInstance slot, boolean warn) {
            this.key = key;
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.icon = icon;
            this.rail = rail;
            this.selected = selected;
            this.warn = warn;
            this.renderNode = renderNode;
            this.slot = slot;
        }
    }

    private static class CollisionRes{
        private final List<Pair<String, String>> nodesToNodes;
        private final Map<String, Pair<Boolean, Boolean>> nodesWithSelfBoundary;

        public CollisionRes() {
            nodesToNodes = new ArrayList<>();
            nodesWithSelfBoundary = new HashMap<>();
        }

        public void add(String id1, String id2) {
            nodesToNodes.add(Pair.of(id1, id2));
        }

        public void add(String id, Pair<Boolean, Boolean> collision) {
            nodesWithSelfBoundary.put(id, collision);
        }

        public List<Pair<String, String>> getNodesToNodes() {
            return nodesToNodes;
        }
        public Pair<Boolean, Boolean> wasNodeCollidedBoundary(String id) {
            return nodesWithSelfBoundary.getOrDefault(id, Pair.of(false, false));
        }

        public boolean collidesWithNode(String id) {
            for (Pair<String, String> pair : nodesToNodes) {
                if (id.equals(pair.getLeft()) || id.equals(pair.getRight())) {
                    return true;
                }
            }
            return false;
        }

        public boolean isOk() {
            return nodesToNodes.isEmpty() && nodesWithSelfBoundary.isEmpty();
        }

        public void clear() {
            nodesToNodes.clear();
            nodesWithSelfBoundary.clear();
        }
    }
}
