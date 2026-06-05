package com.sheridan.gcr.mixin;


import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.render.IGunRenderer;
import com.sheridan.gcr.items.GunItem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(ItemRenderer.class)
public class RenderItemMixin {

    @Inject(at = @At("HEAD"), method = "render", cancellable = true)
    public void Other(ItemStack itemStack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay, BakedModel model, CallbackInfo ci) {
        if (itemStack != null && itemStack.getItem() instanceof GunItem gun) {
            if (displayContext == ItemDisplayContext.GUI) {
                return;
            }
            IGunRenderer gunRenderer = Client.getGunRenderer();
            gunRenderer.renderOther(null, itemStack, gun.getGun(), displayContext, poseStack, bufferSource, combinedLight, combinedOverlay);
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V", cancellable = true)
    public void FirstAndThirdPersonAndEntity(@Nullable LivingEntity entity, ItemStack itemStack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource, @Nullable Level level, int combinedLight, int combinedOverlay, int seed, CallbackInfo ci) {
        if (itemStack != null && itemStack.getItem() instanceof GunItem gun) {
            IGunRenderer gunRenderer = Client.getGunRenderer();
            if (entity == null) {
                gunRenderer.renderOther(null, itemStack, gun.getGun(), displayContext, poseStack, bufferSource, combinedLight, combinedOverlay);
            } else {
                if (displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                    gunRenderer.renderFirstPerson((LocalPlayer) entity, itemStack, gun.getGun(), poseStack, combinedLight, combinedOverlay);
                } else if (displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                    gunRenderer.renderOther(entity, itemStack, gun.getGun(), displayContext, poseStack, bufferSource, combinedLight, combinedOverlay);
                }
            }
            ci.cancel();
        }

    }
}
