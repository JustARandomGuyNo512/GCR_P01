package com.sheridan.gcr.client.animation.command;

import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.animation.IAnimationSequence;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.ModuleRenderNode;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.ModuleModelRegister;
import com.sheridan.gcr.items.MagItem;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.task.GunTaskHandler;
import com.sheridan.gcr.modularSys.task.IGunTask;
import com.sheridan.gcr.modularSys.task.reload.AKReloadTask;
import com.sheridan.gcr.modularSys.task.reload.ARReloadTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ShadowNodeRender extends Command{
    public static final int SHADOW_NODE_RENDER_KEY = 70000;
    public String refBoneName;
    public String targetBoneName;
    public boolean copyNodeStatus;
    public float startProgress;
    public float startTimestamp;
    private final ShadowEntry tempEntry;
    private long lastStartTime = 0;
    private IAnimationSequence lastSequence = null;

    public ShadowNodeRender(String command, float timeStamp) {
        super(command, timeStamp);
        if (args.size() != 4) {
            throw new IllegalArgumentException("Invalid command args: " + command);
        }
        startTimestamp = Float.parseFloat(args.getFirst());//指令起点
        refBoneName = args.get(1);//从哪个骨骼中获取源渲染节点
        targetBoneName = args.get(2);//将复制节点放到哪个骨骼的姿态上
        copyNodeStatus = Boolean.parseBoolean(args.get(3));//是否复制数据
        tempEntry = new ShadowEntry(targetBoneName);
    }

    @Override
    public void bindDef(AnimationDef def) {
        super.bindDef(def);
        startProgress = startTimestamp / def.lengthInSeconds();
    }

    @Override
    public void onFrame(IAnimated animated, IAnimationSequence sequence, ModuleRenderContext context) {
        if (sequence.getCurrentAnimatingProgress() >= startProgress) {
            ModuleRenderNode moduleRenderNode = context.currentRenderNode();
            List<ModuleRenderNode> moduleRenderNodes = moduleRenderNode.slots.get(refBoneName);
            ModuleRenderNode shadowRefNode = null;
            if (moduleRenderNodes != null && !moduleRenderNodes.isEmpty()) {
                shadowRefNode = moduleRenderNodes.getFirst();
            } else if ("MAG".equalsIgnoreCase(refBoneName)) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null && GunTaskHandler.INSTANCE.hasTask()) {
                    IGunTask<?> task = GunTaskHandler.INSTANCE.getTask();
                    int targetSlot = -1;
                    if (task instanceof ARReloadTask arTask) {
                        targetSlot = arTask.targetSlot;
                    } else if (task instanceof AKReloadTask akTask) {
                        targetSlot = akTask.targetSlot;
                    }
                    if (targetSlot >= 0 && targetSlot < player.getInventory().items.size()) {
                        ItemStack magStack = player.getInventory().items.get(targetSlot);
                        if (magStack.getItem() instanceof MagItem magItem) {
                            IModular modular = magItem.getModule();
                            IModularModel model = ModuleModelRegister.get(modular);
                            if (model != null) {
                                shadowRefNode = new ModuleRenderNode(model, "shadow_mag", modular.getID(), false);
                            }
                        }
                    }
                }
            }

            if (shadowRefNode == null) {
                return;
            }

            if (tempEntry.shadowNode == null || (sequence.getStartTime() != lastStartTime || lastSequence != sequence)) {
                tempEntry.shadowNode = shadowRefNode.copySelfOnly(copyNodeStatus);
                lastStartTime = sequence.getStartTime();
                lastSequence = sequence;
            }
            ShadowEntries localStorage = context.getLocalStorage(SHADOW_NODE_RENDER_KEY, ShadowEntries.class);
            if (localStorage == null) {
                localStorage = new ShadowEntries();
                context.setLocalStorage(SHADOW_NODE_RENDER_KEY, localStorage);
            }
            tempEntry.node = moduleRenderNode;
            localStorage.entries.add(tempEntry);
        }
    }

    public static class ShadowEntries{
        public List<ShadowEntry> entries;

        public ShadowEntries() {
            entries = new ArrayList<>();
        }
    }

    public static class ShadowEntry {
        public ModuleRenderNode node;
        public String targetBoneName;
        public ModuleRenderNode shadowNode;

        public ShadowEntry(String targetBoneName) {
            this.targetBoneName = targetBoneName;
        }
    }
}
