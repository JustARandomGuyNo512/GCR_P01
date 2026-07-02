package com.sheridan.gcr.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.client.recoil.RecoilCameraHandler;
import com.sheridan.gcr.items.GunItem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    public boolean isBobbingLevelView;
    public boolean isRenderingHand = false;

    @Inject(method = "getFov", at = @At("HEAD"))
    public void onGetFov(Camera pActiveRenderInfo, float pPartialTicks, boolean pUseFOVSetting, CallbackInfoReturnable<Double> cir) {
        isBobbingLevelView = pUseFOVSetting;
    }

//    @Inject(method = "renderItemInHand", at = @At("HEAD"))
//    public void onStartRenderHand(Camera camera, float partialTick, Matrix4f projectionMatrix, CallbackInfo ci) {
//        isRenderingHand = true;
//    }
//
//
//    public void onGetFov() {
//
//    }
//
//    @Inject(method = "renderItemInHand", at = @At("TAIL"))
//    public void onEndRenderHand(Camera camera, float partialTick, Matrix4f projectionMatrix, CallbackInfo ci) {
//        isRenderingHand = false;
//    }

    @Inject(method = "bobView", at = @At("HEAD"))
    public void onBobbingView(PoseStack pPoseStack, float pPartialTicks, CallbackInfo ci) {
        if (isBobbingLevelView) {
            Player player = Minecraft.getInstance().player;
            if (player != null && player.getMainHandItem().getItem() instanceof GunItem gun) {
                RecoilCameraHandler.getInstance().onBobbingView(pPoseStack, pPartialTicks, gun.getGun());
            }
        }
    }

}
