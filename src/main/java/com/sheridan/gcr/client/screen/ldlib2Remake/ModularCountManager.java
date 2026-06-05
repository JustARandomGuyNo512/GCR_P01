package com.sheridan.gcr.client.screen.ldlib2Remake;

import com.sheridan.gcr.items.ModuleItem;
import com.sheridan.gcr.modularSys.IModular;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ModularCountManager {
    private boolean isCreativeMode = false;
    private final Map<IModular, Integer> modulesInBackpack = new HashMap<>();
    private final Map<IModular, Integer> usedModulesCount = new HashMap<>();
    private final Map<IModular, Integer> modulesTakeoffFromGun = new HashMap<>();

    public int getModuleLeftCount(IModular modular) {
        if (isCreativeMode) {
            return Integer.MAX_VALUE;
        }
        Integer usedCount = usedModulesCount.getOrDefault(modular, 0);
        Integer backpackCount = modulesInBackpack.getOrDefault(modular, 0);
        Integer takeOffCount = modulesTakeoffFromGun.getOrDefault(modular, 0);
        return backpackCount + takeOffCount - usedCount;
    }

    public void onModuleUsed(IModular module) {
        if (isCreativeMode) {
            return;
        }
        Integer takeOffCount = modulesTakeoffFromGun.getOrDefault(module, 0);
        if (takeOffCount > 0) {
            takeOffCount -= 1;
            if (takeOffCount == 0) {
                modulesTakeoffFromGun.remove(module);
            } else {
                modulesTakeoffFromGun.put(module, takeOffCount);
            }
            return;
        }
        Integer backpackCount = modulesInBackpack.getOrDefault(module, 0);
        Integer usedCount = usedModulesCount.getOrDefault(module, 0);
        if (backpackCount >= usedCount + 1) {
            usedCount += 1;
            usedModulesCount.put(module, usedCount);
        }
    }

    public void onModuleTakeOffFromGun(IModular module) {
        if (isCreativeMode) {
            return;
        }
        int usedCount = usedModulesCount.getOrDefault(module, 0);
        if (usedCount > 0) {
            usedCount -= 1;
            if (usedCount == 0) {
                usedModulesCount.remove(module);
            } else {
                usedModulesCount.put(module, usedCount);
            }
            return;
        }
        int takeOffCount = modulesTakeoffFromGun.getOrDefault(module, 0);
        takeOffCount += 1;
        modulesTakeoffFromGun.put(module, takeOffCount);
    }

    public void onTick(Player player) {
        isCreativeMode = player.isCreative();
        if (isCreativeMode) {
            return;
        }
        modulesInBackpack.clear();
        Inventory inventory = player.getInventory();
        NonNullList<ItemStack> items = inventory.items;
        for (ItemStack itemStack : items) {
            if (itemStack.getItem() instanceof ModuleItem<?> moduleItem) {
                IModular module = moduleItem.getModule();
                if (modulesInBackpack.containsKey(module)) {
                    modulesInBackpack.put(module, modulesInBackpack.get(module) + 1);
                } else {
                    modulesInBackpack.put(module, 1);
                }
            }
        }
    }
}
