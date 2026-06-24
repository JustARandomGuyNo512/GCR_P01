package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.client.KeyBinds;
import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.*;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.states.Str;
import com.sheridan.gcr.modularSys.modules.views.IAmmoSourceView;
import com.sheridan.gcr.modularSys.modules.views.IM203View;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class M203 extends SubWeapon implements IVoxelHandlerModule, IArmHandlerModular, IStateModular, IM203View {
    private final IVoxelHandler voxelHandler;
    private final AdditionalPropModifier modifier;

    public M203(ResourceLocation id, float weight, IVoxelHandler voxelHandler, AdditionalPropModifier modifier) {
        super(id, true, weight, Direction.NONE);
        this.voxelHandler = voxelHandler;
        this.modifier = modifier;
    }

    @Override
    public IVoxelHandler getHandler() {
        return voxelHandler;
    }

    @Override
    public int getPriority(boolean rightArm) {
        return rightArm ? IArmHandlerModular.NONE_PRIORITY : IArmHandlerModular.SUB_WEAPON_PRIORITY;
    }

    @Override
    public @Nullable AdditionalPropModifier getModifier() {
        return modifier;
    }

    @Override
    public void onInitStates(CompoundTag states, String nodeId, String moduleId) {
        CHAMBER_STATUS.init(states);
    }

    @Override
    public void onUpdate(StatesUpdateContext context) {

    }

    @Override
    public String getChamberStatus(CompoundTag states) {
        return CHAMBER_STATUS.get(states);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void onKeyPressed(int keyCode, int action, String thisNodeId, Unit unit, IGun gun, ItemStack itemStack) {
        super.onKeyPressed(keyCode, action, thisNodeId, unit, gun, itemStack);
        if (keyCode == KeyBinds.USE_GRENADE_LAUNCHER.getKey().getValue() && KeyBinds.USE_GRENADE_LAUNCHER.isDown()) {
            CompoundTag states = gun.getNodeStatesTag(itemStack, thisNodeId);
            String chamberStatus = getChamberStatus(states);
            if (action == 1) {
                if (!CHAMBER_LOADED.equals(chamberStatus)) {
                    //TODO: 重新装填

                } else {
                    //TODO: 进入准备射击模式
                }
            } else if (action == 0) {
                //TODO: 如果准备好射击，则开火，否则退出准备射击模式
            }
        } else if (keyCode == KeyBinds.CHECK_SUB_WEAPON.getKey().getValue() && KeyBinds.CHECK_SUB_WEAPON.isDown()) {
            //TODO: 发送弹膛检查动画
        }
    }

}
