package com.sheridan.gcr.modularSys;

import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.ModuleModelRegister;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.render.ModuleRenderNode;
import com.sheridan.gcr.modularSys.builder.Node;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ModuleHandler {

    @Nullable
    @OnlyIn(Dist.CLIENT)
    public static ModuleRenderNode buildRenderTreeByNode(Node root) {
        if (root == null) {
            return null;
        }
        final ModuleRenderNode[] rootRenderNode = {null};
        Map<String, ModuleRenderNode> renderNodes = new Object2ObjectOpenHashMap<>();
        root.dfs(node -> {
            IModular module = node.getModule();
            IModularModel model = ModuleModelRegister.get(module);
            if (model != null) {
                ModuleRenderNode parentNode =
                        node.getParent() == null ?
                                null :
                                renderNodes.get(node.getParent().getID());
                if (parentNode != null) {
                    String id = node.getID();
                    ModuleRenderNode moduleRenderNode = loadRenderNode(module, model, node, id, node.shouldReverseModel());
                    parentNode.addSlot(node.getBelongsToSlotName(), moduleRenderNode);
                    renderNodes.put(id, moduleRenderNode);
                } else {
                    if (rootRenderNode[0] == null) {
                        String id = node.getID();
                        ModuleRenderNode moduleRenderNode = loadRenderNode(module, model, node, id, node.shouldReverseModel());
                        rootRenderNode[0] = moduleRenderNode;
                        renderNodes.put(id, moduleRenderNode);
                    }
                }
            }
        });
        return rootRenderNode[0];
    }

    @OnlyIn(Dist.CLIENT)
    private static ModuleRenderNode loadRenderNode(IModular modular, IModularModel model, Node fromNode, String id, boolean reverseModel) {
        ModuleRenderNode moduleRenderNode = new ModuleRenderNode(model, id, fromNode.getModuleID(), reverseModel);
        Map<String, Integer> customRenderProperties = fromNode.getCustomRenderParams();
        if (!customRenderProperties.isEmpty()) {
            CompoundTag param = new CompoundTag();
            for (Map.Entry<String, Integer> entry : customRenderProperties.entrySet()) {
                param.putInt(entry.getKey(), entry.getValue());
            }
            moduleRenderNode.setCustomParamTag(param);
        }
        moduleRenderNode.z = fromNode.getUnit().getZOffset();
        return moduleRenderNode;
    }

    @SuppressWarnings("deprecation")
    @Nullable
    @OnlyIn(Dist.CLIENT)
    public static ModuleRenderNode buildRenderTree(ItemStack itemStack) {
        CustomData orDefault = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!orDefault.isEmpty()) {
            //直接使用源NBT，避免数据复制
            CompoundTag unsafe = orDefault.getUnsafe();
            //时序列表，首节点视作渲染root，不止局限于从root构建
            ListTag modulesTag = unsafe.getList(IGun.MODULES_KEY, ListTag.TAG_COMPOUND);
            if (modulesTag.isEmpty()) {
                return null;
            }
            CompoundTag states = unsafe.getCompound(IGun.STATES_KEY);
            ModuleRenderNode root = null;
            Object2ObjectOpenHashMap<String, ModuleRenderNode> nodeMap = new Object2ObjectOpenHashMap<>();
            for (Tag tag : modulesTag) {
                CompoundTag moduleTag = (CompoundTag) tag;
                String string = moduleTag.getString(Unit.MODULE_ID);
                IModularModel model = ModuleModelRegister.getByID(string);
                if (model == null) {
                    continue;
                }
                ModuleRenderNode parentNode = nodeMap.get(moduleTag.getString(Unit.PARENT_ID));
                if (parentNode != null) {
                    String id = moduleTag.getString(Unit.IN_TIME_ID);
                    ModuleRenderNode moduleRenderNode = loadRenderNode(model, moduleTag, states, id, string);
                    parentNode.addSlot(moduleTag.getString(Unit.SLOT_NAME), moduleRenderNode);
                    nodeMap.put(id, moduleRenderNode);
                } else {
                    if (root == null) {
                        String id = moduleTag.getString(Unit.IN_TIME_ID);
                        ModuleRenderNode moduleRenderNode = loadRenderNode(model, moduleTag, states, id, string);
                        root = moduleRenderNode;
                        nodeMap.put(id, moduleRenderNode);
                    }
                }
            }
            return root;
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    private static ModuleRenderNode loadRenderNode(IModularModel model, CompoundTag tag, CompoundTag states, String id, String moduleId) {
        boolean reverseModel = tag.getBoolean(Unit.REVERSE_MODEL);
        ModuleRenderNode moduleRenderNode = new ModuleRenderNode(model, id, moduleId, reverseModel);
        if (tag.contains(Unit.RENDER_PARAMS)) {
            moduleRenderNode.setCustomParamTag(tag.getCompound(Unit.RENDER_PARAMS));
        }
        moduleRenderNode.z = tag.getFloat(Unit.OFFSET_Z);
        if (states.contains(id)) {
            moduleRenderNode.setStates(new ReadOnlyTag(states.getCompound(id)));
        }
        return moduleRenderNode;
    }
}
