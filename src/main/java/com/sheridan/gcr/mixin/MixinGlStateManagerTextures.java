package com.sheridan.gcr.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin({GlStateManager.class})
public class MixinGlStateManagerTextures {
    /*
    * 我不到啊，我看Iris这么干的我也这么干了:D
    * */
    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 12), require = 0)
    private static int gcr$increaseMaximumAllowedTextureUnits(int existingValue) {
        return 128;
    }
}
