package com.sheridan.gcr.mixin;

import com.sheridan.gcr.mixinUtils.DualHandItemAccessor;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin  implements DualHandItemAccessor {
    private ItemStack mainHandItem;

    @Override
    public ItemStack getMainHandItem() {
        return mainHandItem;
    }

    @Override
    public void setMainHandItem(ItemStack stack) {
        this.mainHandItem = stack;
    }

}