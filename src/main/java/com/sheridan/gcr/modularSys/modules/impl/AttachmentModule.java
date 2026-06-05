package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.modules.Module;
import com.sheridan.gcr.modularSys.modules.gunProperties.IProp;
import com.sheridan.gcr.modularSys.modules.gunProperties.IProperties;
import com.sheridan.gcr.modularSys.modules.gunProperties.PropertiesAccessor;
import com.sheridan.gcr.modularSys.modules.gunProperties.PropertiesDummies;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AttachmentModule extends Module {
    protected Map<Class<? extends IProperties>, List<PropModifier>> modifiers = new HashMap<>();

    public AttachmentModule(ResourceLocation id, boolean fixedPosition, float weight, Direction direction) {
        super(id, fixedPosition, weight, direction);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        String weight = Component.translatable("gcr:prop.base.weight").getString();
        weight = weight + ": " + String.format("%.2f", getWeight());
        tooltipComponents.add(Component.literal(weight));
        for (List<PropModifier> list : modifiers.values()) {
            for (PropModifier modifier : list) {
                tooltipComponents.add(Component.literal(modifier.getTooltipMsg()));
            }
        }
    }

    @Override
    public void modifyProperties(PropertiesAccessor accessor) {
        super.modifyProperties(accessor);
        for (Class<? extends IProperties> clazz : modifiers.keySet()) {
            for (PropModifier entry : modifiers.get(clazz)) {
                accessor.using(clazz, prop -> {
                    IProp apply = entry.getter.apply(prop);
                    if (entry.inc) {
                        prop.inc(apply, entry.value);
                    } else {
                        prop.dec(apply, entry.value);
                    }
                });
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends IProperties> void defPropInc(Class<T> clazz, Function<T, IProp> propGetter, float val) {
        modifiers
                .computeIfAbsent(clazz, k -> new ArrayList<>())
                .add(new PropModifier(
                        clazz,
                        (Function<IProperties, IProp>) propGetter,
                        val,
                        true
                ));
    }

    protected void clearPropModifiers() {
        modifiers.clear();
    }

    protected <T extends IProperties> void clearPropModifiers(Class<T> clazz) {
        modifiers.remove(clazz);
    }

    @SuppressWarnings("unchecked")
    protected <T extends IProperties> void defPropDec(Class<T> clazz, Function<T, IProp> propGetter, float val) {
        modifiers
                .computeIfAbsent(clazz, k -> new ArrayList<>())
                .add(new PropModifier(
                        clazz,
                        (Function<IProperties, IProp>) propGetter,
                        val,
                        false
                ));
    }

    protected static class PropModifier {
        final Class<?> type;
        final Function<IProperties, IProp> getter;
        final float value;
        final boolean inc;

        PropModifier(Class<?> type, Function<IProperties, IProp> getter, float value, boolean inc) {
            this.type = type;
            this.getter = getter;
            this.value = value;
            this.inc = inc;
        }

        public String getTooltipMsg() {
            String fullName = getFullName();
            float val = inc ? value : -value;
            val *= 100f;
            String format = String.format("%.2f", val);
            if (val >= 0) {
                format = "+" + format;
            }
            return Component.translatable("tooltip.prop.modifier").getString()
                    .replace("$name", fullName)
                    .replace("$amount", format);
        }

        public String getFullName() {
            IProperties dummy = PropertiesDummies.getDummy(type);
            if (dummy instanceof IProperties properties) {
                IProp apply = getter.apply(properties);
                return apply.getFullName();
            }
            return "UNKNOWN";
        }
    }
}
