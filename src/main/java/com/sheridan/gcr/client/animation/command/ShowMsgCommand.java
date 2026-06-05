package com.sheridan.gcr.client.animation.command;

import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.animation.IAnimationSequence;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShowMsgCommand extends Command{
    private float coolDown;
    private final String msgKey;
    private long lastShow;
    private float startProgress;

    public ShowMsgCommand(String command, float timeStamp) {
        super(command, timeStamp);
        if (args.size() != 2) {
            throw new IllegalArgumentException("Invalid command args: " + command);
        }
        this.msgKey = args.getFirst();
        this.coolDown = Float.parseFloat(args.get(1));
        this.coolDown = Math.max(coolDown, 0.1f);
    }

    @Override
    public void bindDef(AnimationDef def) {
        super.bindDef(def);
        startProgress = timeStamp / def.lengthInSeconds();
    }

    @Override
    public void onFrame(IAnimated animated, IAnimationSequence sequence, ModuleRenderContext context) {
        if (sequence.getCurrentAnimatingProgress() >= startProgress) {
            float timeDist = (System.currentTimeMillis() - lastShow) * 0.001f;
            if (timeDist > coolDown) {
                Minecraft.getInstance().gui.setOverlayMessage(Component.translatable(msgKey), false);
                lastShow = System.currentTimeMillis();
            }
        }
    }
}
