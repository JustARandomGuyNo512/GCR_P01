package com.sheridan.gcr.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.class)
public class PlayerMixin {

//    @Inject(at = @At("HEAD"), method = "getCurrentItemAttackStrengthDelay", cancellable = true)
//    public void changeEquipSpeed(CallbackInfoReturnable<Float> cir) {
//        ItemStack mainHandItem = ((Player) (Object) this).getMainHandItem();
//        if (mainHandItem.getItem() instanceof GunItem gunItem) {
//            IGun gun = gunItem.getGunModule();
//            //1~4
//            float speed = 1f / 2f * 20.0f;
//            cir.setReturnValue(speed);
//        }
//    }
}
