package com.sheridan.gcr.modularSys;

import com.sheridan.gcr.IJsonSync;
import com.sheridan.gcr.items.ModuleItem;
import com.sheridan.gcr.modularSys.builder.IAccessor;
import com.sheridan.gcr.modularSys.builder.IWriteableAccessor;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.builder.ValidateResult;
import com.sheridan.gcr.modularSys.modules.gunProperties.PropertiesAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Set;


public interface IModular extends IJsonSync {

    void modifyProperties(PropertiesAccessor accessor);

    String getID();

    String getSimpleID();

    Set<String> getTags();

    boolean hasTag(String tag);

    boolean fixedPosition();

    void validate(IAccessor accessor, Unit thisUnit, ValidateResult result);

    void onMutated(IWriteableAccessor accessor, Unit thisUnit);

    boolean bindItem(ModuleItem<?> item);

    ModuleItem<?> getBindItem();

    Direction getDirection();

    void finalizeInit();

    float getWeight();

    void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag);
}
