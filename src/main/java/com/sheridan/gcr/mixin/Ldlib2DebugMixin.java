package com.sheridan.gcr.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"com.lowdragmc.lowdraglib2.Platform"})
public class Ldlib2DebugMixin {

    @Inject(method = "isDevEnv", at = @At("HEAD"), cancellable = true, remap = false)
    private static void test(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
        cir.cancel();
    }
}
