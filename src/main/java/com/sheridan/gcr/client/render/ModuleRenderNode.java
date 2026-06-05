package com.sheridan.gcr.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class ModuleRenderNode {
    private static final float PI = (float) Math.PI;
    @NotNull
    public IModularModel model;
    public String id;
    public String moduleId;
    public Map<String, List<ModuleRenderNode>> slots;
    private CompoundTag customParams;
    private @Nullable ReadOnlyTag states;
    public float z;
    public boolean reverseModel;

    public ModuleRenderNode(@NotNull IModularModel model, String id, String moduleId, boolean reverseModel) {
        this.model = model;
        this.id = id;
        this.moduleId = moduleId;
        this.reverseModel = reverseModel;
    }

    public void addSlot(String slotName, ModuleRenderNode node) {
        if (slots == null) {
            slots = new Object2ObjectArrayMap<>();
        }
        if (model.hasSlot(slotName)) {
            slots.computeIfAbsent(slotName, (key) -> new ArrayList<>()).add(node);
        }
    }

    public void setCustomParamTag(CompoundTag tag) {
        customParams = tag;
    }

    public void setStates(@Nullable ReadOnlyTag states) {
        this.states = states;
    }
    @Nullable
    public ReadOnlyTag getStates() {
        return states;
    }

    public boolean hasState(String state) {
        return states != null && states.contains(state);
    }

    public int getCustomParam(String key) {
        if (customParams == null) {
            return -1;
        }
        return customParams.getInt(key);
    }

    public void dfsTravel(Consumer<ModuleRenderNode> visitor) {
        visitor.accept(this);
        if (slots != null) {
            slots.values().forEach((nodes) -> {
                for (ModuleRenderNode node : nodes) {
                    node.dfsTravel(visitor);
                }
            });
        }
    }

    public void dfs(Function<ModuleRenderNode, Boolean> visitor) {
        Boolean apply = visitor.apply(this);
        if (!apply) {
            return;
        }
        if (slots != null) {
            slots.values().forEach((nodes) -> {
                for (ModuleRenderNode node : nodes) {
                    node.dfs(visitor);
                }
            });
        }
    }

    void initTranslate(PoseStack poseStack) {
        if (z != 0) {
            poseStack.translate(0, 0, z);
        }
        if (reverseModel) {
            poseStack.mulPose(new Quaternionf().rotateXYZ(0, 0, PI));
        }
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ModuleRenderNode node) {
            return node.id.equals(id);
        }
        return false;
    }

    public boolean hasChild(String slotName) {
        return slots != null && slots.containsKey(slotName);
    }

    public void removeSlot(String slotName, ModuleRenderNode node) {
        if (slots != null) {
            slots.get(slotName).remove(node);
        }
    }
}
