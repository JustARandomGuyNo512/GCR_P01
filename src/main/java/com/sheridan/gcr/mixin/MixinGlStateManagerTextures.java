package com.sheridan.gcr.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin({GlStateManager.class})
public class MixinGlStateManagerTextures {

    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 12), require = 0)
    private static int iris$increaseMaximumAllowedTextureUnits(int existingValue) {
        return 128;
    }
}
