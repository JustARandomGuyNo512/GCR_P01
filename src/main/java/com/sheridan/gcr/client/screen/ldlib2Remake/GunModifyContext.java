package com.sheridan.gcr.client.screen.ldlib2Remake;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.model.BoneRenderStatus;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
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
    private static final ResourceLocation NODE = GCR.RL(GCR.MODID, "textures/gui/screen/node.png");
    private static final ResourceLocation NODE_SELECTED = GCR.RL(GCR.MODID, "textures/gui/screen/node_selected.png");

    private static final ResourceLocation SLOT_EMPTY = GCR.RL(GCR.MODID, "textures/gui/screen/empty.png");
    private static final ResourceLocation SLOT_EMPTY_SELECTED = GCR.RL(GCR.MODID, "textures/gui/screen/empty_selected.png");
    private static final ResourceLocation SLOT_OCCUPIED = GCR.RL(GCR.MODID, "textures/gui/screen/empty_occupied.png");
    private static final ResourceLocation SLOT_OCCUPIED_SELECTED = GCR.RL(GCR.MODID, "textures/gui/screen/occupied_selected.png");
    private static final ResourceLocation SLOT_BOUNDARY = GCR.RL(GCR.MODID, "textures/gui/screen/slot_boundary.png");

    private static final Vector3f OUT_SCREEN = new Vector3f(Float.NaN, Float.NaN, Float.NaN);
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

    public GunModifyContext(ItemStack itemStack) {
        if (itemStack.getItem() instanceof GunItem gunItem) {
            this.itemStack = itemStack;
            this.gun = gunItem.getGun();

            builder = new Builder();
            IGun gun = gunItem.getGun();
            ListTag modulesTag = gun.getModulesTag(itemStack).copy();
            builder.init(modulesTag);
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


    public void trySetSelectedNodePos(float progress) {
        if (selectedNode == null || selectedNode.isFixedPosition()) {
            return;
        }
        if (selectedNode.getBelongsToSlot() instanceof IRail rail) {
            float originalPos = selectedRenderNode.z;
            rail.setChildPosition(selectedNode.getUnit(), accessor, progress);
            selectedRenderNode.z = selectedNode.getUnit().getZOffset();
            if (Math.abs(originalPos - selectedRenderNode.z) >= 1e-5) {
                treeModified = true;
                collisionDirty = true;
                builder.getWorkspace().increaseMutateId();
            }
        }
    }
    public void renderComponents(GuiGraphics guiGraphics) {
        renderNodeIcons(guiGraphics);
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
            if (dis < 2f) {
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
            if (pair.getLeft().isHidden()) {
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
            if (Math.sqrt(dx * dx + dy * dy) < 4f) {
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
                ResourceLocation texture = key == selectedRenderNode ? NODE_SELECTED : NODE;
                guiGraphics.blit(texture,
                        (int) pos.x - 2,
                        (int) pos.y - 2,  0,0, 4, 4, 4, 4);
            }
        }

        if (renderSlotIcon) {
            for (Pair<SlotInstance, Vector3f> pair : slotScreenPos.values()) {
                if (pair.getLeft().isHidden()) {
                    continue;
                }
                Vector3f pos = pair.getValue();
                if (pos == OUT_SCREEN) {
                    continue;
                }
                guiGraphics.blit(getSlotIcon(
                                pair.getKey()),
                        (int) pos.x - 4,
                        (int) pos.y + 2,  0,0, 8, 8, 8, 8);
            }
        }
    }

    private ResourceLocation getSlotIcon(SlotInstance slot) {
        int i = slot.getSlot().maxCapacity();
        int size = getWorkSpace().getNodes(slot).size();
        boolean occupied = size >= i;
        return slot == selectedSlot ?
                occupied ? SLOT_OCCUPIED_SELECTED : SLOT_EMPTY_SELECTED
                :
                occupied ? SLOT_OCCUPIED : SLOT_EMPTY;
    }

    private Vector3f getScreenPos(Matrix4f matrix4f) {
        Matrix4f m0 = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f m1 = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f m2 = new Matrix4f(matrix4f);
        Vector4f vector4f = m2.transform(new Vector4f(0, 0, 0, 1.0F));
        Vector4f v = vector4f.mul(m0).mul(m1);
        if (Math.abs((v.x / v.w)) > 1 || Math.abs((v.y / v.w)) > 1 || v.z < 0.0075f) {
            return OUT_SCREEN;
        } else {
            float w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            float h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            float screenX = ((v.x / v.w) * w + w) * 0.5f;
            float screenY = (-(v.y / v.w) * h + h) * 0.5f;
            float screenDepth = v.z;
            return new Vector3f(screenX, screenY, screenDepth);
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

        public boolean isOk() {
            return nodesToNodes.isEmpty() && nodesWithSelfBoundary.isEmpty();
        }

        public void clear() {
            nodesToNodes.clear();
            nodesWithSelfBoundary.clear();
        }
    }
}
