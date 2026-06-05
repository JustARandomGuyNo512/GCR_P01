package com.sheridan.gcr.client.animation.command;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.animation.IAnimationSequence;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * example: ads_pose_limit(main, 0.1, 0.1)
 * */
@OnlyIn(Dist.CLIENT)
public class AdsPoseLimit extends Command{
    private final String boneName;
    private final float transScale;
    private final float rotScale;

    public AdsPoseLimit(String command, float timeStamp) {
        super(command, timeStamp);
        if (args.size() != 3) {
            throw new IllegalArgumentException("Invalid command args: " + command);
        }
        boneName = args.getFirst();
        transScale = Float.parseFloat(args.get(1));
        rotScale = Float.parseFloat(args.get(2));
    }


    @Override
    public void onFrame(IAnimated animated, IAnimationSequence sequence, ModuleRenderContext context) {
        float aimingProgress = Client.getAimingProgress(context.partialTicks);
        if (aimingProgress > 0) {
            IModularModel model = context.root.model;
            Bone bone = model.getBone(boneName);
            if (bone != null) {
                float trans = Mth.lerp(aimingProgress, 1, transScale);
                float rot = Mth.lerp(aimingProgress, 1, rotScale);
                bone.x *= trans;
                bone.y *= trans;
                bone.z *= trans;
                bone.xRot *= rot;
                bone.yRot *= rot;
                bone.zRot *= rot;
            }
        }
    }
}