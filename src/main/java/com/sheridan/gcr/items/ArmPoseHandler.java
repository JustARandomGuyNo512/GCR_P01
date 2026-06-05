package com.sheridan.gcr.items;

import com.sheridan.gcr.data.ModData;
import com.sheridan.gcr.data.PlayerCommonStatus;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;


@OnlyIn(Dist.CLIENT)
public class ArmPoseHandler implements IClientItemExtensions {

    public static final ArmPoseHandler ARM_POSE_HANDLER = new ArmPoseHandler();

    @Override
    public HumanoidModel.ArmPose getArmPose(@NotNull LivingEntity entityLiving, @NotNull InteractionHand hand, @NotNull ItemStack itemStack) {
        if (entityLiving.hasData(ModData.PLAYER_STATUS)) {
            PlayerCommonStatus data = entityLiving.getData(ModData.PLAYER_STATUS);
            return data.isReloading() ?
                    HumanoidModel.ArmPose.CROSSBOW_CHARGE :
                    HumanoidModel.ArmPose.BOW_AND_ARROW;
        } else {
            return HumanoidModel.ArmPose.BOW_AND_ARROW;
        }
    }
}