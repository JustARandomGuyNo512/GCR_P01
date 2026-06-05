package com.sheridan.gcr.items;

import com.sheridan.gcr.modularSys.modules.guns.Gun;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;


public class GunItem extends ModuleItem<Gun>{

    public GunItem(Gun module) {
        super(module);
    }

    public IGun getGun() {
        return getModule();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    @SuppressWarnings("removal")
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ArmPoseHandler.ARM_POSE_HANDLER);
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        System.out.println("getDefaultInstance");
        return super.getDefaultInstance();
    }

}
