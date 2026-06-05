package com.sheridan.gcr.modularSys.modules;

import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface IInteractiveModular {
    @OnlyIn(Dist.CLIENT)
    default void onKeyPressed(int keyCode, String thisNodeId, Unit unit, IGun gun, ItemStack itemStack) {}

    @OnlyIn(Dist.CLIENT)
    default void onMousePressed(int button, int action, String thisNodeId, Unit unit, IGun gun, ItemStack itemStack) {}

    /**
     * @return true if the event is handled
     * */
    @OnlyIn(Dist.CLIENT)
    default boolean onMouseScroll(double mx, double my, double deltaX, double deltaY, String thisNodeId, Unit unit, IGun gun, ItemStack itemStack) {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    default void onClientTick(String thisNodeId, Unit unit, IGun gun, ItemStack itemStack) {}
}
