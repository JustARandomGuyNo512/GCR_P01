package com.sheridan.gcr.client.animation.command;

import com.sheridan.gcr.Utils;
import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.animation.IAnimationSequence;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class Command {
    public String command;
    public List<String> args;
    public int tick;
    public float timeStamp;
    public AnimationDef def;

    public Command(String command, float timeStamp) {
        this.command = command;
        this.timeStamp = timeStamp;
        this.tick = Utils.secondToTick(timeStamp);
        this.resolve();
    }

    protected void resolve() {
        // 去掉结尾的分号
        String raw = command.replace(";", "").trim();

        this.args = new ArrayList<>();

        int left = raw.indexOf('(');
        int right = raw.lastIndexOf(')');

        // 没有参数，例如: play;
        if (left == -1 || right == -1 || right < left) {
            this.command = raw;
            return;
        }

        // 命令名
        this.command = raw.substring(0, left).trim();

        // 参数区
        String argPart = raw.substring(left + 1, right).trim();
        if (argPart.isEmpty()) {
            return;
        }

        // 拆分参数
        String[] split = argPart.split(",");
        for (String s : split) {
            args.add(s.trim());
        }
    }

    public void bindDef(AnimationDef def) {
        this.def = def;
    }

    public void onTick() {

    }

    public void onFrame(IAnimated animated, IAnimationSequence sequence, ModuleRenderContext context) {

    }

    public void onRemove() {

    }
}
