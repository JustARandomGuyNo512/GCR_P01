package com.sheridan.gcr.modularSys.modules.impl;

import com.google.gson.JsonObject;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.WeaponStatus;
import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.IScope;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import com.sheridan.gcr.modularSys.modules.IVoxelHandlerModule;
import com.sheridan.gcr.modularSys.modules.StatesUpdateContext;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.c2s.SyncGunStatusPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public class Scope extends AttachmentModule implements IVoxelHandlerModule, IScope {
    private final IVoxelHandler voxelHandler;
    protected float adsSpeedModifier;
    protected float maxRate;
    protected float minRate;
    protected float zoomSensitivity;

    public Scope(ResourceLocation id, IVoxelHandler voxelHandler, float weight, float adsSpeedModifier, float minRate, float maxRate, float zoomSensitivity) {
        super(id, false, weight, Direction.UPPER);
        this.voxelHandler = voxelHandler;
        this.adsSpeedModifier = adsSpeedModifier;
        this.maxRate = maxRate;
        this.minRate = minRate;
        this.zoomSensitivity = zoomSensitivity;
    }

    @Override
    public void writeToJson(JsonObject jsonObject) {

    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {

    }

    @Override
    public IVoxelHandler getHandler() {
        return voxelHandler;
    }


    @Override
    public float getAdsSpeedModifier() {
        return adsSpeedModifier;
    }

    @Override
    public int defaultSightPriority(Unit unit) {
        return SCOPE;
    }

    @Override
    public void setZoomRatio(float ratio, CompoundTag states) {
        ZOOM_RATIO.set(ratio, states);
    }

    @Override
    public float getZoomSensitivity() {
        return zoomSensitivity;
    }

    @Override
    public void onUpdate(StatesUpdateContext context) {

    }

    @Override
    public float getMaxRate() {
        return maxRate;
    }

    @Override
    public float getMinRate() {
        return minRate;
    }

    @Override
    public float getRate(CompoundTag states) {
        return Mth.lerp(getRatio(states), minRate, maxRate);
    }

    @Override
    public float getRatio(CompoundTag states) {
        return Mth.clamp(ZOOM_RATIO.get(states), 0, 1);
    }

    protected static double totalDelta;
    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean onMouseScroll(double mx, double my, double deltaX, double deltaY, String thisNodeId, Unit unit, IGun gun, ItemStack itemStack) {
        WeaponStatus status = Client.WEAPON_STATUS;
        if (status.isAiming() && Minecraft.getInstance().screen == null) {
            CompoundTag states = gun.getNodeStatesTag(itemStack, thisNodeId);
            if (states != null) {
                deltaY = -deltaY * getZoomSensitivity();
                totalDelta += deltaY;
            }
            return true;
        } else {
            return false;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void onClientTick(String thisNodeId, Unit unit, IGun gun, ItemStack itemStack) {
        if (totalDelta == 0) {
            return;
        }
        CompoundTag states = gun.getNodeStatesTag(itemStack, thisNodeId);
        if (states == null) {
            return;
        }
        float ratio = getRatio(states);
        ratio = (float) Mth.clamp(ratio + totalDelta, 0, 1);
        setZoomRatio(ratio, states);
        SyncGunStatusPacket.toServer(gun, itemStack, thisNodeId, ZOOM_RATIO);
        totalDelta = 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        String replace = Component.translatable("tooltip.prop.magnification").getString()
                .replace("$min", String.format("%.2f", minRate))
                .replace("$max", String.format("%.2f", maxRate));
        tooltipComponents.add(Component.translatable(replace));
    }
}
