package com.sheridan.gcr.modularSys.modules.impl;

import com.google.gson.JsonObject;
import com.sheridan.gcr.client.KeyBinds;
import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.builder.ShadowNode;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.IFlashLight;
import com.sheridan.gcr.modularSys.modules.IInteractiveModular;
import com.sheridan.gcr.modularSys.modules.StatesUpdateContext;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.c2s.SyncGunStatusPacket;
import com.sheridan.gcr.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.awt.*;
import java.util.List;

public class FlashLightModule extends AttachmentModule implements IInteractiveModular, IFlashLight {
    protected float luminance;
    protected float range;
    protected float angle;

    public FlashLightModule(ResourceLocation id, boolean fixedPosition, float weight, float luminance, float range, float angle, Direction direction) {
        super(id, fixedPosition, weight, direction);
        this.luminance = luminance;
        this.range = range;
        this.angle = angle;

    }

    @Override
    public void writeToJson(JsonObject jsonObject) {

    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {

    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void onKeyPressed(int keyCode, int action, String thisNodeId, Unit unit, IGun gun, ItemStack itemStack) {
        if (keyCode == KeyBinds.TURN_FLASHLIGHT.getKey().getValue() && KeyBinds.TURN_FLASHLIGHT.isDown()) {
            CompoundTag nodeStatesTag = gun.getNodeStatesTag(itemStack, thisNodeId);
            if (nodeStatesTag == null) {
                return;
            }
            boolean on = IS_ON.get(nodeStatesTag);
            setIsOn(!on, nodeStatesTag);
            SyncGunStatusPacket.toServer(gun, itemStack, thisNodeId, IS_ON);
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                ModSounds.sound(1, 1, player, ModSounds.BTN.get());
            }
        }
    }

    @Override
    public void setIsOn(boolean isOn, CompoundTag states) {
        IS_ON.set(isOn, states);
    }

    @Override
    public boolean isOn(CompoundTag states) {
        return IS_ON.get(states);
    }

    @Override
    public float getLuminance() {
        return luminance;
    }

    @Override
    public float getRange() {
        return range;
    }

    @Override
    public float getAngle() {
        return angle;
    }

    @Override
    public void onUpdate(StatesUpdateContext context) {
        boolean isOn = context.get(IS_ON);
        if (isOn) {
            return;
        }
        boolean turnOn = false;
        for (ShadowNode node : context.getAllNodesOfThisTree()) {
            IModular module = node.unit.getModule();
            if (module instanceof IFlashLight flashLight) {
                CompoundTag statesData = context.getStatesData(node);
                boolean on = flashLight.isOn(statesData);
                if (on) {
                    turnOn = true;
                    break;
                }
            }
        }
        if (turnOn) {
            context.set(IS_ON, true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        String string = Component.translatable("tooltip.util.flashlight").getString();
        String msg = string.replace("$key", KeyBinds.TURN_FLASHLIGHT.getTranslatedKeyMessage().getString());
        tooltipComponents.add(Component.literal(msg).setStyle(Style.EMPTY.withColor(Color.GRAY.getRGB())));
    }
}
