package com.sheridan.gcr.items;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.modularSys.IModular;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class ModuleItem<T extends IModular> extends NoRepairNoEnchantmentItem {
    private final T module;

    public ModuleItem(T module) {
        super(new Properties().stacksTo(1));
        this.module = module;
        boolean bind = this.module.bindItem(this);
        if (!bind) {
            if (GCR.IS_DEVELOPMENT) {
                throw new RuntimeException("Module " + module.getID() + " failed to bind to item " + this.getDescription().getString());
            } else {
                GCR.LOGGER.warn("Module {} failed to bind to item {}", module.getID(), this.getDescription().getString());
            }
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        module.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    public T getModule() {
        return module;
    }

    @Override
    public @NotNull String getDescriptionId() {
        return module.getID();
    }
}
